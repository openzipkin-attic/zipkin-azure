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

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import zipkin.collector.Collector;
import zipkin.collector.CollectorComponent;
import zipkin.collector.CollectorMetrics;
import zipkin.collector.CollectorSampler;
import zipkin.internal.LazyCloseable;
import zipkin.storage.StorageComponent;

public final class EventHubCollector implements CollectorComponent {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder implements CollectorComponent.Builder {
    Collector.Builder delegate = Collector.builder(EventHubCollector.class);
    String eventHubName = "zipkin";
    String consumerGroupName = "$Default";
    String eventHubConnectionString;
    String storageConnectionString;
    String storageContainerName = "zipkin";
    int checkpointBatchSize = 10;

    String processorHostName = UUID.randomUUID().toString();
    String storageBlobPrefix = processorHostName;

    Builder() {
    }

    public Builder eventHubName(String name) {
      eventHubName = name;
      return this;
    }

    public Builder consumerGroupName(String name) {
      consumerGroupName = name;
      return this;
    }

    public Builder checkpointBatchSize(int size) {
      checkpointBatchSize = size;
      return this;
    }

    public Builder eventHubConnectionString(String connectionString) {
      eventHubConnectionString = connectionString;
      return this;
    }

    public Builder storageConnectionString(String connectionString) {
      storageConnectionString = connectionString;
      return this;
    }

    public Builder storageContainerName(String containerName) {
      storageContainerName = containerName;
      return this;
    }

    public Builder storageBlobPrefix(String blobPrefix) {
      storageBlobPrefix = blobPrefix;
      return this;
    }

    public Builder processorHostName(String nameForThisProcessorHost) {
      processorHostName = nameForThisProcessorHost;
      return this;
    }

    @Override public Builder storage(StorageComponent storage) {
      delegate.storage(storage);
      return this;
    }

    @Override public Builder metrics(CollectorMetrics metrics) {
      delegate.metrics(metrics);
      return this;
    }

    @Override public Builder sampler(CollectorSampler sampler) {
      delegate.sampler(sampler);
      return this;
    }

    @Override public EventHubCollector build() {
      return new EventHubCollector(this);
    }
  }

  final AtomicBoolean closed = new AtomicBoolean(false);
  final LazyCloseable<Future<?>> lazyRegisterEventProcessor;

  EventHubCollector(Builder builder) {
    this(new LazyRegisterEventProcessor(builder));
  }

  EventHubCollector(LazyCloseable<Future<?>> lazyRegisterEventProcessor) {
    this.lazyRegisterEventProcessor = lazyRegisterEventProcessor;
  }

  @Override public EventHubCollector start() {
    if (!closed.get()) lazyRegisterEventProcessor.get();
    return this;
  }

  @Override public CheckResult check() {
    try {
      // make sure compute doesn't throw an exception
      Future<?> registrationFuture = lazyRegisterEventProcessor.get();
      registrationFuture.get(); // make sure registration succeeded
      return CheckResult.OK;
    } catch (RuntimeException e) {
      return CheckResult.failed(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return CheckResult.failed(e);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof Error) throw (Error) cause;
      return CheckResult.failed((Exception) cause);
    }
  }

  @Override public void close() throws IOException {
    if (!closed.compareAndSet(false, true)) return;
    lazyRegisterEventProcessor.close();
  }
}
