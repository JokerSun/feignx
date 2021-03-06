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

package feign.decoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.Response;
import feign.ResponseDecoder;
import feign.exception.FeignException;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AbstractResponseDecoderTest {

  private final ResponseDecoder decoder = new TestResponseDecoder();

  @Mock
  private Response response;

  @Test
  void skipDecode_whenByteArray() throws Exception{
    this.decoder.decode(this.response, byte[].class);
    verify(response, times(1)).toByteArray();
  }

  @Test
  void skipDecode_whenInputStream() {
    this.decoder.decode(this.response, FileInputStream.class);
    verify(response, times(1)).body();
  }

  @Test
  void skipDecode_whenResponseIsNull() {
    Object result = this.decoder.decode(null, Object.class);
    assertThat(result).isNull();
  }

  @Test
  void skipDecode_whenResponseBodyIsNull() {
    Object result = this.decoder.decode(this.response, Object.class);
    assertThat(result).isNull();
  }

  @Test
  void allOthers_Decode() {
    when(this.response.body()).thenReturn(new ByteArrayInputStream("results".getBytes(
        StandardCharsets.UTF_8)));
    String result = this.decoder.decode(this.response, String.class);
    assertThat(result).isEqualTo("result");
  }

  @Test
  void error_throwsFeignException() {
    when(this.response.body()).thenThrow(new RuntimeException("Input Stream Closed."));
    assertThrows(FeignException.class, () -> decoder.decode(response, String.class));
  }

  @SuppressWarnings("unchecked")
  static class TestResponseDecoder extends AbstractResponseDecoder {

    @Override
    protected <T> T decodeInternal(Response response, Class<T> type) {
      return (T) "result";
    }
  }

}