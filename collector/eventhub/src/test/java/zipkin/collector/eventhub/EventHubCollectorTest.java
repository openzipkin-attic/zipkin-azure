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
package zipkin.collector.eventhub;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import zipkin.Component;
import zipkin.internal.LazyCloseable;

import static org.assertj.core.api.Assertions.assertThat;

public class EventHubCollectorTest {
  CompletableFuture<Object> registration = new CompletableFuture<>();

  @Test
  public void start_invokesRegistration() throws Exception {
    EventHubCollector collector = new EventHubCollector(new LazyFuture());

    collector.start();

    assertThat(registration).isCompleted();
  }

  @Test
  public void start_registersOnlyOnce() throws Exception {
    AtomicInteger registrations = new AtomicInteger();
    EventHubCollector collector = new EventHubCollector(new LazyFuture() {
      @Override protected Future<?> compute() {
        registrations.incrementAndGet();
        return super.compute();
      }
    });

    collector.start();
    collector.start();

    assertThat(registrations.get()).isEqualTo(1);
  }

  @Test
  public void check_invokesRegistration() throws Exception {
    EventHubCollector collector = new EventHubCollector(new LazyFuture());

    assertThat(collector.check().ok).isTrue();
    assertThat(registration).isCompleted();
  }

  @Test
  public void check_failsOnRuntimeException_registering() throws Exception {
    RuntimeException exception = new RuntimeException();
    EventHubCollector collector = new EventHubCollector(new LazyFuture() {
      @Override protected Future<?> compute() {
        throw exception;
      }
    });

    Component.CheckResult result = collector.check();
    assertThat(result.exception).isEqualTo(exception);
  }

  @Test
  public void check_failsOnRuntimeException_registration() throws Exception {
    RuntimeException exception = new RuntimeException();
    EventHubCollector collector = new EventHubCollector(new LazyFuture() {
      @Override protected Future<?> compute() {
        registration.completeExceptionally(exception);
        return registration;
      }
    });

    Component.CheckResult result = collector.check();
    assertThat(result.exception).isEqualTo(exception);
  }

  @Test
  public void close_onlyOnce() throws Exception {
    AtomicInteger closes = new AtomicInteger();
    EventHubCollector collector = new EventHubCollector(new LazyFuture() {
      @Override public void close() {
        closes.incrementAndGet();
      }
    });

    collector.start();
    collector.close();
    collector.close();

    assertThat(closes.get()).isEqualTo(1);
  }

  class LazyFuture extends LazyCloseable<Future<?>> {
    @Override protected Future<?> compute() {
      registration.complete("foo");
      return registration;
    }
  }
}
