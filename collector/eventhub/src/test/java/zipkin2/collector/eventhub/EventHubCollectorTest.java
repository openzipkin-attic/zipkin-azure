/*
 * Copyright 2017-2018 The OpenZipkin Authors
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
package zipkin2.collector.eventhub;

import com.microsoft.azure.eventprocessorhost.EventProcessorHost;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import zipkin2.CheckResult;

import static org.assertj.core.api.Assertions.assertThat;

public class EventHubCollectorTest {
  CompletableFuture<Object> registration = new CompletableFuture<>();

  @Test
  public void start_invokesRegistration() {
    EventHubCollector collector = new EventHubCollector(new LazyFuture());

    collector.start();

    assertThat(registration).isCompleted();
  }

  @Test
  public void start_registersOnlyOnce() {
    AtomicInteger registrations = new AtomicInteger();
    EventHubCollector collector =
        new EventHubCollector(
            new LazyFuture() {
              @Override
              protected Future<?> compute() {
                registrations.incrementAndGet();
                return super.compute();
              }
            });

    collector.start();
    collector.start();

    assertThat(registrations.get()).isEqualTo(1);
  }

  @Test
  public void check_invokesRegistration() {
    EventHubCollector collector = new EventHubCollector(new LazyFuture());

    assertThat(collector.check().ok()).isTrue();
    assertThat(registration).isCompleted();
  }

  @Test
  public void check_failsOnRuntimeException_registering() {
    RuntimeException exception = new RuntimeException();
    EventHubCollector collector =
        new EventHubCollector(
            new LazyFuture() {
              @Override
              protected Future<?> compute() {
                throw exception;
              }
            });

    CheckResult result = collector.check();
    assertThat(result.error()).isEqualTo(exception);
  }

  @Test
  public void check_failsOnRuntimeException_registration() {
    RuntimeException exception = new RuntimeException();
    EventHubCollector collector =
        new EventHubCollector(
            new LazyFuture() {
              @Override
              protected Future<?> compute() {
                registration.completeExceptionally(exception);
                return registration;
              }
            });

    CheckResult result = collector.check();
    assertThat(result.error()).isEqualTo(exception);
  }

  @Test
  public void close_onlyOnce() throws IOException {
    AtomicInteger closes = new AtomicInteger();
    EventHubCollector collector =
        new EventHubCollector(
            new LazyFuture() {
              @Override
              public void close() {
                closes.incrementAndGet();
              }
            });

    collector.start();
    collector.close();
    collector.close();

    assertThat(closes.get()).isEqualTo(1);
  }

  @Test
  public void blobpathprefix_set_toaconstant_atstartup() {
    EventHubCollector.Builder builder1 = EventHubCollector.newBuilder();
    EventHubCollector.Builder builder2 = EventHubCollector.newBuilder();

    // make sure it is not changing every time like before
    assertThat(builder2.storageBlobPrefix).isEqualTo(builder1.storageBlobPrefix);
  }

  class LazyFuture extends LazyRegisterEventProcessorFactoryWithHost {
    LazyFuture() {
      super(new EventHubCollector.Builder());
    }

    @Override
    EventProcessorHost newEventProcessorHost(EventHubCollector.Builder builder) {
      return null; // Tests don't use this, and skipping avoids initializing EventProcessorHost
    }

    @Override
    Future<?> compute() {
      registration.complete("foo");
      return registration;
    }
  }
}
