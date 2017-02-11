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

import com.microsoft.azure.eventprocessorhost.EventProcessorHost;
import com.microsoft.azure.eventprocessorhost.IEventProcessorFactory;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import zipkin.internal.LazyCloseable;

/**
 * This registers an event processor factory on the first call to get {@link #get()}. If an event
 * processor factory was registered, it is deregistered on {@link #close()}.
 */
// not final for testing
class LazyRegisterEventProcessorFactoryWithHost extends LazyCloseable<Future<?>> {
  final EventProcessorHost host;
  final IEventProcessorFactory<?> factory;

  LazyRegisterEventProcessorFactoryWithHost(EventHubCollector.Builder builder) {
    host = new EventProcessorHost(
        builder.processorHost,
        builder.name,
        builder.consumerGroup,
        builder.connectionString,
        builder.storageConnectionString,
        builder.storageContainer,
        builder.storageBlobPrefix
    );
    ZipkinEventProcessor processor =
        new ZipkinEventProcessor(builder.delegate.build(), builder.checkpointBatchSize);
    factory = context -> processor;
  }

  @Override protected final Future<?> compute() {
    try {
      return registerEventProcessorFactoryWithHost();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @Override public final void close() throws IOException {
    Future<?> maybeNull = this.maybeNull();
    if (maybeNull == null) return;
    try {
      maybeNull.cancel(true);
      unregisterEventProcessorFactoryFromHost();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      InterruptedIOException toThrow =
          new InterruptedIOException("interrupted unregistering the event processor from " + host);
      toThrow.initCause(e);
      throw toThrow;
    } catch (ExecutionException e) {
      throw new IllegalStateException(e.getCause());
    }
  }

  // Since EventProcessorHost is a final class, it cannot be mocked. Override for testing
  Future<?> registerEventProcessorFactoryWithHost() throws Exception {
    return host.registerEventProcessorFactory(factory);
  }

  void unregisterEventProcessorFactoryFromHost() throws InterruptedException, ExecutionException {
    host.unregisterEventProcessor();
  }
}
