package feign.http.client;

import feign.Client;
import feign.http.HttpException;
import feign.http.HttpHeader;
import feign.http.Request;
import feign.http.RequestOptions;
import feign.http.Response;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Client implementation that uses a {@link java.net.URLConnection}.
 */
public class UrlConnectionClient implements Client {

  /**
   * Send the Request to the destination. and process the response.
   *
   * @param request to send.
   * @return Response received.
   * @throws HttpException if any error occurred during processing.
   */
  @Override
  public Response request(Request request) throws HttpException {
    HttpURLConnection connection = this.send(request);
    return this.receive(connection);
  }

  /**
   * Send the Request.
   *
   * @param request to send.
   * @return connection with the result of the request.
   */
  private HttpURLConnection send(Request request) {
    try {
      /* convert the uri to a url */
      URL url = request.uri().toURL();

      /* create a new connection */
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod(request.method().name());

      /* set the connection options */
      RequestOptions options = request.options();
      connection.setInstanceFollowRedirects(options.isFollowRedirects());
      connection.setConnectTimeout(options.getConnectTimeout());
      connection.setReadTimeout(options.getReadTimeout());
      connection.setAllowUserInteraction(false);

      /* apply the request headers */
      for (HttpHeader header : request.headers()) {
        header.values().forEach(value -> connection.addRequestProperty(header.name(), value));
      }

      /* write the request  */
      byte[] data = request.content();
      if (data != null) {
        try (OutputStream os = connection.getOutputStream()) {
          os.write(data);
        }
      }
      return connection;
    } catch (MalformedURLException mfe) {
      throw new IllegalArgumentException("Error occurred preparing the request.  The URL provided "
          + request.uri() + " is invalid", mfe);
    } catch (ProtocolException pe) {
      throw new IllegalArgumentException("Error occurred preparing the request.  The method "
          + "provided: " + request.method() + " is invalid", pe);
    } catch (IOException ioe) {
      throw new HttpException("Error occurred sending the request", ioe, request);
    }
  }

  /**
   * Receive the Response.
   *
   * @param connection with the Response.
   * @return the Response model.
   */
  private Response receive(HttpURLConnection connection) {
    try {
      /* execute the request */
      int statusCode = connection.getResponseCode();

      /* build up the common response objects */
      Response.Builder builder = Response.builder();
      builder.status(statusCode)
          .reason(connection.getResponseMessage())
          .contentLength(connection.getContentLength());

      /* copy the headers */
      connection.getHeaderFields().entrySet().stream()
          .filter(entry -> entry.getKey() != null)
          .forEach(entry -> builder.addHeader(new HttpHeader(entry.getKey(), entry.getValue())));

      if (statusCode > 399) {
        builder.body(connection.getErrorStream());
      } else {
        builder.body(connection.getInputStream());
      }
      return builder.build();
    } catch (IOException ioe) {
      throw new HttpException("Error occurred processing the response", ioe);
    }
  }
}
