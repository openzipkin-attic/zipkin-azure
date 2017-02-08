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
package zipkin.autoconfigure.collector.eventhub;

import org.springframework.boot.context.properties.ConfigurationProperties;
import zipkin.collector.eventhub.EventHubCollector;

@ConfigurationProperties("zipkin.collector.eventhub")
public class EventHubCollectorProperties {
  private String name = "zipkin";
  private String consumerGroup = "$Default";
  private String connectionString;
  private int checkpointBatchSize = 10;
  private String processorHost;
  private Storage storage = new Storage();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getConsumerGroup() {
    return consumerGroup;
  }

  public void setConsumerGroup(String consumerGroup) {
    this.consumerGroup = consumerGroup;
  }

  public String getConnectionString() {
    return connectionString;
  }

  public void setConnectionString(String connectionString) {
    this.connectionString = connectionString;
  }

  public int getCheckpointBatchSize() {
    return checkpointBatchSize;
  }

  public void setCheckpointBatchSize(int checkpointBatchSize) {
    this.checkpointBatchSize = checkpointBatchSize;
  }

  public String getProcessorHost() {
    return processorHost;
  }

  public void setProcessorHost(String processorHost) {
    this.processorHost = processorHost;
  }

  public Storage getStorage() {
    return storage;
  }

  public void setStorage(
      Storage storage) {
    this.storage = storage;
  }

  public static class Storage {
    private String connectionString;
    private String container;
    private String blobPrefix;

    public String getConnectionString() {
      return connectionString;
    }

    public void setConnectionString(String connectionString) {
      this.connectionString = connectionString;
    }

    public String getContainer() {
      return container;
    }

    public void setContainer(String container) {
      this.container = container;
    }

    public String getBlobPrefix() {
      return blobPrefix;
    }

    public void setBlobPrefix(String blobPrefix) {
      this.blobPrefix = blobPrefix;
    }
  }

  public EventHubCollector.Builder toBuilder() {
    EventHubCollector.Builder builder = EventHubCollector.builder()
        .connectionString(connectionString)
        .storageConnectionString(storage.connectionString);

    if (notEmpty(getStorage().blobPrefix)) {
      builder = builder.storageBlobPrefix(storage.blobPrefix);
    }

    if (notEmpty(processorHost)) {
      builder = builder.processorHost(processorHost);
    }

    if (checkpointBatchSize > 0) {
      builder = builder.checkpointBatchSize(checkpointBatchSize);
    }

    if (notEmpty(consumerGroup)) {
      builder = builder.consumerGroup(consumerGroup);
    }

    if (notEmpty(name)) {
      builder = builder.name(name);
    }

    if (notEmpty(getStorage().container)) {
      builder = builder.storageContainer(storage.container);
    }

    return builder;
  }

  private static boolean notEmpty(String s) {
    return !(s == null || s.isEmpty());
  }
}
