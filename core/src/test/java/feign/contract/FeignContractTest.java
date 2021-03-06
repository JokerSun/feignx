/*
 * Copyright 2019 OpenFeign Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package feign.contract;

import static org.assertj.core.api.Assertions.assertThat;

import feign.Contract;
import feign.RequestOptions;
import feign.Response;
import feign.TargetMethodDefinition;
import feign.http.HttpMethod;
import feign.impl.UriTarget;
import feign.template.SimpleTemplateParameter;
import feign.template.expander.ListExpander;
import feign.template.expander.MapExpander;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FeignContractTest {

  @Test
  void can_parseSimpleInterface() {
    Contract contract = new FeignContract();
    Collection<TargetMethodDefinition> methodDefinitions = contract.apply(
        new UriTarget<>(SimpleInterface.class, "https://example.com"));

    assertThat(methodDefinitions).isNotEmpty();

    /* verify the defaults */
    assertThat(methodDefinitions).allMatch(
        targetMethodDefinition -> targetMethodDefinition.getUri().startsWith("/resources"));
    assertThat(methodDefinitions).allMatch(
        targetMethodDefinition -> targetMethodDefinition.getHeaders().stream().anyMatch(
            header -> header.name().equalsIgnoreCase("Accept")));

    /* verify each method is registered */
    assertThat(methodDefinitions).anyMatch(
        targetMethodDefinition -> targetMethodDefinition.getName().equalsIgnoreCase("get")
            && targetMethodDefinition.getReturnType().getType() == String.class
            && targetMethodDefinition.getMethod() == HttpMethod.GET
            && targetMethodDefinition.getTemplateParameters().isEmpty()
            && targetMethodDefinition.getBody() == -1
            && targetMethodDefinition.getConnectTimeout() == RequestOptions.DEFAULT_CONNECT_TIMEOUT
            && targetMethodDefinition.getReadTimeout() == RequestOptions.DEFAULT_READ_TIMEOUT
            && targetMethodDefinition.isFollowRedirects());

    /* implicit body parameter */
    assertThat(methodDefinitions).anyMatch(
        targetMethodDefinition -> targetMethodDefinition.getName().equalsIgnoreCase("post")
            && targetMethodDefinition.getReturnType().getType() == String.class
            && targetMethodDefinition.getMethod() == HttpMethod.POST
            && targetMethodDefinition.getTemplateParameters().contains(
            new SimpleTemplateParameter("parameter"))
            && targetMethodDefinition.getBody() == 1);

    /* explicit body parameter */
    assertThat(methodDefinitions).anyMatch(
        targetMethodDefinition -> targetMethodDefinition.getName().equalsIgnoreCase("put")
            && targetMethodDefinition.getReturnType().getType() == String.class
            && targetMethodDefinition.getMethod() == HttpMethod.PUT
            && targetMethodDefinition.getTemplateParameters()
            .contains(new SimpleTemplateParameter("parameter"))
            && targetMethodDefinition.getBody() == 1);

    /* void return type */
    assertThat(methodDefinitions).anyMatch(
        targetMethodDefinition -> targetMethodDefinition.getName().equalsIgnoreCase("delete")
            && targetMethodDefinition.getReturnType().getType() == void.class
            && targetMethodDefinition.getMethod() == HttpMethod.DELETE
            && targetMethodDefinition.getTemplateParameters()
            .contains(new SimpleTemplateParameter("parameter"))
            && targetMethodDefinition.getBody() == -1);

    /* request options and generic return type */
    assertThat(methodDefinitions).anyMatch(
        targetMethodDefinition -> targetMethodDefinition.getName().equalsIgnoreCase("search")
            && targetMethodDefinition.getReturnType().getType() == List.class
            && targetMethodDefinition.getMethod() == HttpMethod.GET
            && targetMethodDefinition.getTemplateParameters().isEmpty()
            && targetMethodDefinition.getBody() == -1
            && targetMethodDefinition.getConnectTimeout() == 1000
            && targetMethodDefinition.getReadTimeout() == 2000
            && !targetMethodDefinition.isFollowRedirects());

    /* map parameter type */
    assertThat(methodDefinitions).anySatisfy(targetMethodDefinition -> {
      boolean properties = targetMethodDefinition.getName().equalsIgnoreCase("map")
          && targetMethodDefinition.getReturnType().getType() == List.class
          && targetMethodDefinition.getMethod() == HttpMethod.GET
          && targetMethodDefinition.getBody() == -1;
      assertThat(properties).isTrue();

      targetMethodDefinition.getTemplateParameter(0)
          .ifPresent(
              parameter -> assertThat(parameter.expander()).isInstanceOf(MapExpander.class));
    });

    /* list parameter type */
    assertThat(methodDefinitions).anySatisfy(
        targetMethodDefinition -> {
          boolean properties = targetMethodDefinition.getName().equalsIgnoreCase("list")
              && targetMethodDefinition.getReturnType().getType() == List.class
              && targetMethodDefinition.getMethod() == HttpMethod.GET
              && targetMethodDefinition.getBody() == -1;
          assertThat(properties).isTrue();

          targetMethodDefinition.getTemplateParameter(0)
              .ifPresent(
                  parameter -> assertThat(parameter.expander()).isInstanceOf(ListExpander.class));
        });

    /* response return type */
    assertThat(methodDefinitions).anyMatch(
        targetMethodDefinition -> targetMethodDefinition.getName().equalsIgnoreCase("response")
            && targetMethodDefinition.getReturnType().getType() == Response.class
            && targetMethodDefinition.getMethod() == HttpMethod.GET
            && targetMethodDefinition.getTemplateParameters()
            .contains(new SimpleTemplateParameter("parameters"))
            && targetMethodDefinition.getBody() == -1);
  }

  @Test
  void replaceUri_whenDefinedAsAbsolute() {
    Contract contract = new FeignContract();
    Collection<TargetMethodDefinition> methodDefinitions =
        contract.apply(new UriTarget<>(AbsoluteRequests.class, "https://www.example.com"));

    /* the search method should not have the uri root */
    //noinspection OptionalGetWithoutIsPresent
    TargetMethodDefinition methodDefinition = methodDefinitions.stream()
        .filter(
            targetMethodDefinition -> targetMethodDefinition.getName().equalsIgnoreCase("search"))
        .findAny()
        .get();
    assertThat(methodDefinition.getUri()).isEqualTo("https://www.google.com?q={query}");

    //noinspection OptionalGetWithoutIsPresent
    TargetMethodDefinition getDefinition = methodDefinitions.stream()
        .filter(
            targetMethodDefinition -> targetMethodDefinition.getName().equalsIgnoreCase("get"))
        .findAny()
        .get();
    assertThat(getDefinition.getUri()).isEqualTo("/resources/");
  }

  @SuppressWarnings("unused")
  @Request("/resources")
  @Headers(value = @Header(name = "Accept", value = "application/json"))
  interface SimpleInterface {

    @Request(value = "/")
    String get();

    @Request(value = "/create", method = HttpMethod.POST)
    String post(@Param("parameter") String value, String content);

    @Request(value = "/replace", method = HttpMethod.PUT)
    String put(@Param("parameter") String value, @Body String content);

    @Request(value = "/remove", method = HttpMethod.DELETE)
    void delete(@Param("parameter") String value);

    @Request(value = "/search", method = HttpMethod.GET, followRedirects = false,
        readTimeout = 2000, connectTimeout = 1000)
    List<String> search();

    @Request("/map")
    List<String> map(@Param("parameters") Map<String, String> parameters);

    @Request("/list")
    List<String> list(@Param("parameters") List<String> parameters);

    @Request(value = "/response")
    Response response(@Param("parameters") String parameters);
  }

  @SuppressWarnings("unused")
  @Request("/resources")
  interface AbsoluteRequests {

    @Request("/")
    String get();

    @Request("https://www.google.com?q={query}")
    String search(@Param("query") String query);

  }

}