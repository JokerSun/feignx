package feign.http;

import feign.Request;
import feign.RequestOptions;
import java.net.URI;
import java.util.Arrays;

/**
 * Http Request Model.
 */
public final class HttpRequest implements Request {

  private URI uri;
  private HttpMethod method;
  private HttpHeader[] headers;
  private RequestOptions options;
  private byte[] content;

  /**
   * Creates a new empty HttpRequest.
   */
  HttpRequest() {
    super();
  }

  /**
   * Creates a new HttpRequest.
   *
   * @param uri for the request.
   * @param method for the request.
   * @param headers to include on the request.
   * @param options for the request.
   * @param content to include in the request.
   */
  public HttpRequest(URI uri, HttpMethod method, HttpHeader[] headers, RequestOptions options,
      byte[] content) {
    this.uri = uri;
    this.method = method;
    this.headers = headers;
    this.options = (options == null) ? RequestOptions.builder().build() : options;
    this.content = content;
  }

  /**
   * URI of the Request.
   *
   * @return request uri.
   */
  public URI uri() {
    return this.uri;
  }

  /**
   * Request Content.  Can be {@literal null}.
   *
   * @return the request content, if any.
   */
  public byte[] content() {
    return this.content;
  }

  /**
   * Http Method for the Request.
   *
   * @return the http method.
   */
  public HttpMethod method() {
    return this.method;
  }

  /**
   * Headers for the Request.
   *
   * @return an array of Request Headers.
   */
  public HttpHeader[] headers() {
    return this.headers;
  }

  /**
   * Options for the Request.
   *
   * @return request options.
   */
  public RequestOptions options() {
    return this.options;
  }

  @Override
  public String toString() {
    return "HttpRequest [" + "uri=" + uri
        + ", method=" + method
        + ", headers=" + Arrays.toString(headers)
        + ", options=" + options
        + "]";
  }
}
