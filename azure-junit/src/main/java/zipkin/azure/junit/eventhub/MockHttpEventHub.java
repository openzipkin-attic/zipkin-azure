/**
 * Copyright 2017 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package zipkin.azure.junit.eventhub;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.junit.rules.ExternalResource;
import zipkin.Codec;
import zipkin.Span;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import java.io.IOException;


public final class MockHttpEventHub extends ExternalResource {

  private MockWebServer server;

  public MockHttpEventHub(){
    server = new MockWebServer();
  }

  public void start(int portNumber, int numberOfSpans) throws Exception {
    server.start(portNumber);
    addSpans(numberOfSpans);
  }

  private void addSpans(int count) throws UnsupportedEncodingException {
    Random r = new Random();
    for (int i = 0; i < count; i++) {
      Span s = Span.builder()
          .id(Math.abs(r.nextLong()))
          .duration(Math.abs(r.nextLong()))
          .name("wah?")
          .parentId(Math.abs(r.nextLong()))
          .traceId(Math.abs(r.nextLong()))
          .build();

      ArrayList<Span> spans = new ArrayList<Span>();
      spans.add(s);
      byte[] buffer = Codec.JSON.writeSpans(spans);

      server.enqueue(new MockResponse()
          .setResponseCode(200)
          .addHeader("Content-Type", "application/json")
          .setBody(new String(buffer, "UTF-8")));
    }
  }

  @Override
  protected void before() throws Throwable {
    super.before();
  }

  @Override
  protected void after() {
    super.after();
    try {
      server.shutdown();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


}
