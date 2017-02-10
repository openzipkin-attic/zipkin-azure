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
public class ZipkinEventHubCollectorProperties {
  private String name;
  private String consumerGroup;
  private String connectionString;
  private Integer checkpointBatchSize;
  private String processorHost;
  private Storage storage = new Storage();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = emptyToNull(name);
  }

  public String getConsumerGroup() {
    return consumerGroup;
  }

  public void setConsumerGroup(String consumerGroup) {
    this.consumerGroup = emptyToNull(consumerGroup);
  }

  public String getConnectionString() {
    return connectionString;
  }

  public void setConnectionString(String connectionString) {
    this.connectionString = emptyToNull(connectionString);
  }

  public Integer getCheckpointBatchSize() {
    return checkpointBatchSize;
  }

  public void setCheckpointBatchSize(int checkpointBatchSize) {
    if (checkpointBatchSize > 0) this.checkpointBatchSize = checkpointBatchSize;
  }

  public String getProcessorHost() {
    return processorHost;
  }

  public void setProcessorHost(String processorHost) {
    this.processorHost = emptyToNull(processorHost);
  }

  public Storage getStorage() {
    return storage;
  }

  public void setStorage(Storage storage) {
    if (storage != null) this.storage = storage;
  }

  public static class Storage {
    private String connectionString;
    private String container;
    private String blobPrefix;

    public String getConnectionString() {
      return connectionString;
    }

    public void setConnectionString(String connectionString) {
      this.connectionString = emptyToNull(connectionString);
    }

    public String getContainer() {
      return container;
    }

    public void setContainer(String container) {
      this.container = emptyToNull(container);
    }

    public String getBlobPrefix() {
      return blobPrefix;
    }

    public void setBlobPrefix(String blobPrefix) {
      this.blobPrefix = emptyToNull(blobPrefix);
    }
  }

  EventHubCollector.Builder toBuilder() {
    EventHubCollector.Builder result = EventHubCollector.newBuilder();
    if (name != null) result.name(name);
    if (consumerGroup != null) result.consumerGroup(consumerGroup);
    if (connectionString != null) result.connectionString(connectionString);
    if (checkpointBatchSize != null) result.checkpointBatchSize(checkpointBatchSize);
    if (processorHost != null) result.processorHost(processorHost);
    if (storage.connectionString != null) result.storageConnectionString(storage.connectionString);
    if (storage.container != null) result.storageConnectionString(storage.container);
    if (storage.blobPrefix != null) result.storageConnectionString(storage.blobPrefix);
    return result;
  }

  private static String emptyToNull(String s) {
    return (s != null && !s.isEmpty()) ? s : null;
  }
}
