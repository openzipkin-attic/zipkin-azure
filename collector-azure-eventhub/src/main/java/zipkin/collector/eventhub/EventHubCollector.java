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
/**
 * Created by aliostad on 14/01/2017.
 */
package zipkin.collector.eventhub;


import com.microsoft.azure.eventprocessorhost.EventProcessorHost;
import zipkin.collector.Collector;
import zipkin.collector.CollectorComponent;
import zipkin.collector.CollectorMetrics;
import zipkin.collector.CollectorSampler;
import zipkin.storage.StorageComponent;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


public class EventHubCollector implements CollectorComponent {

  private Builder builder;
  private EventProcessorHost host;
  private Boolean started = false;
  private Exception lastException;

  private static final Logger logger = Logger.getLogger(EventHubCollector.class.getName());

  public EventHubCollector(Builder builder) {
    this.builder = builder;
    logger.log(Level.INFO,"_____EventHubCollector_ctor_____");
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder implements CollectorComponent.Builder {
    Collector.Builder delegate = Collector.builder(EventHubCollector.class);
    CollectorMetrics metrics = CollectorMetrics.NOOP_METRICS;
    String eventHubName = "zipkin";
    String consumerGroupName = "$Default";
    String eventHubConnectionString;
    String storageConnectionString;
    String storageContainerName = "zipkin";
    int checkpointBatchSize = 10;

    String processorHostName = UUID.randomUUID().toString();
    String storageBlobPrefix = processorHostName;


    public Builder() {
      System.out.println("_____builder______");
    }

    public EventHubCollector.Builder eventHubName(String name) {
      eventHubName = name;
      return this;
    }

    public EventHubCollector.Builder consumerGroupName(String name) {
      consumerGroupName = name;
      return this;
    }

    public EventHubCollector.Builder checkpointBatchSize(int size) {
      checkpointBatchSize = size;
      return this;
    }

    public EventHubCollector.Builder eventHubConnectionString(String connectionString) {
      eventHubConnectionString = connectionString;
      return this;
    }

    public EventHubCollector.Builder storageConnectionString(String connectionString) {
      storageConnectionString = connectionString;
      return this;
    }

    public EventHubCollector.Builder storageContainerName(String containerName) {
      storageContainerName = containerName;
      return this;
    }

    public EventHubCollector.Builder storageBlobPrefix(String blobPrefix) {
      storageBlobPrefix = blobPrefix;
      return this;
    }

    public EventHubCollector.Builder processorHostName(String nameForThisProcessorHost) {
      processorHostName = nameForThisProcessorHost;
      return this;
    }

    public EventHubCollector.Builder storage(StorageComponent storage) {
      delegate.storage(storage);
      return this;
    }

    public EventHubCollector.Builder metrics(CollectorMetrics metrics) {
      delegate.metrics(metrics);
      return this;
    }

    public EventHubCollector.Builder sampler(CollectorSampler sampler) {
      delegate.sampler(sampler);
      return this;
    }

    public EventHubCollector build() {
      return new EventHubCollector(this);
    }
  }

  public String getEventHubName() {
    return builder.eventHubName;
  }

  public String getConsumerGroupName() {
    return builder.consumerGroupName;
  }

  public String getEventHubConnectionString() {
    return builder.eventHubConnectionString;
  }

  public String getStorageConnectionString() {
    return builder.storageConnectionString;
  }

  public String getStorageContainerName() {
    return builder.storageContainerName;
  }

  public int getCheckpointBatchSize() {
    return builder.checkpointBatchSize;
  }

  public String getProcessorHostName() {
    return builder.processorHostName;
  }

  public String getStorageBlobPrefix() {
    return builder.storageBlobPrefix;
  }

  public EventHubCollector start() {

    logger.log(Level.INFO,"_____EventHubCollector_start______");
    try {

      logger.log(Level.INFO, "processorHostName: " + builder.processorHostName);
      logger.log(Level.INFO, "consumerGroupName: " + builder.consumerGroupName);
      logger.log(Level.INFO, "eventHubName: " + builder.eventHubName);
      logger.log(Level.INFO, "eventHubConnectionString: " + builder.eventHubConnectionString);
      logger.log(Level.INFO, "storageConnectionString: " + builder.storageConnectionString);
      logger.log(Level.INFO, "storageContainerName: " + builder.storageContainerName);
      logger.log(Level.INFO, "storageBlobPrefix: " + builder.storageBlobPrefix);

      host = new EventProcessorHost(builder.processorHostName,
          builder.eventHubName, builder.consumerGroupName,
          builder.eventHubConnectionString, builder.storageConnectionString,
          builder.storageContainerName, builder.storageBlobPrefix);

      host.registerEventProcessorFactory(
          context -> new ZipkinEventProcessor(builder)
      );
      started = true;
    } catch (Exception e) {
      e.printStackTrace();
      lastException = e;

      logger.log(Level.WARNING,"_____EventHubCollector_exception____");
      System.out.println(e);

    }
    return this;
  }

  public CheckResult check() {
    logger.log(Level.INFO,"_____EventHubCollector_check____");
    return started || lastException == null ? CheckResult.OK : CheckResult.failed(lastException);
  }

  public void close() throws IOException {
    try {
      logger.log(Level.INFO,"_____EventHubCollector_close_____");
      if (host != null) {
        host.unregisterEventProcessor();
      }
    } catch (Exception e) {
      e.printStackTrace();
      lastException = e;
    }

    started = false;
  }
}
