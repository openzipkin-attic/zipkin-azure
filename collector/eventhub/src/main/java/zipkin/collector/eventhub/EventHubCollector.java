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
    String name = "zipkin";
    String consumerGroup = "$Default";
    String connectionString;
    String processorHost = UUID.randomUUID().toString();
    int checkpointBatchSize = 10;
    String storageConnectionString;
    String storageContainer = "zipkin";
    String storageBlobPrefix = processorHost;

    Builder() {
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder consumerGroup(String name) {
      this.consumerGroup = name;
      return this;
    }

    public Builder checkpointBatchSize(int size) {
      this.checkpointBatchSize = size;
      return this;
    }

    public Builder connectionString(String connectionString) {
      this.connectionString = connectionString;
      return this;
    }

    public Builder storageConnectionString(String storageConnectionString) {
      this.storageConnectionString = storageConnectionString;
      return this;
    }

    public Builder storageContainer(String storageContainer) {
      this.storageContainer = storageContainer;
      return this;
    }

    public Builder storageBlobPrefix(String storageBlobPrefix) {
      this.storageBlobPrefix = storageBlobPrefix;
      return this;
    }

    public Builder processorHost(String processorHost) {
      this.processorHost = processorHost;
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
