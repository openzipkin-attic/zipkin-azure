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
import com.microsoft.azure.eventprocessorhost.IEventProcessor;
import com.microsoft.azure.eventprocessorhost.IEventProcessorFactory;
import com.microsoft.azure.eventprocessorhost.PartitionContext;
import java.io.InterruptedIOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * This registers an event processor factory on the first call to get {@link #get()}. If an event
 * processor factory was registered, it is deregistered on {@link #close()}.
 */
// not final for testing
class LazyRegisterEventProcessorFactoryWithHost {
  final EventProcessorHost host;
  final IEventProcessorFactory<?> factory;
  final ConcurrentMap<String, IEventProcessor> hosts = new ConcurrentHashMap<>();
  volatile Future<?> future;

  LazyRegisterEventProcessorFactoryWithHost(EventHubCollector.Builder builder) {
    host = newEventProcessorHost(builder);

    // NOTE: for some reason using lambdas occasionally giving java.lang.NoSuchMethodError
    // exceptions
    factory =
        new IEventProcessorFactory<IEventProcessor>() {
          @Override
          public IEventProcessor createEventProcessor(PartitionContext context) throws Exception {
            hosts.putIfAbsent(
                context.getPartitionId(),
                new ZipkinEventProcessor(builder.delegate.build(), builder.checkpointBatchSize));
            return hosts.get(context.getPartitionId());
          }
        };
  }

  EventProcessorHost newEventProcessorHost(EventHubCollector.Builder builder) {
    return new EventProcessorHost(
        builder.processorHost,
        builder.name,
        builder.consumerGroup,
        builder.connectionString,
        builder.storageConnectionString,
        builder.storageContainer,
        builder.storageBlobPrefix);
  }

  Future<?> get() {
    if (future == null) {
      synchronized (this) {
        if (future == null) {
          future = compute();
        }
      }
    }
    return future;
  }

  Future<?> compute() {
    try {
      return registerEventProcessorFactoryWithHost();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  void close() throws InterruptedIOException {
    Future<?> maybeNull = future;
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
