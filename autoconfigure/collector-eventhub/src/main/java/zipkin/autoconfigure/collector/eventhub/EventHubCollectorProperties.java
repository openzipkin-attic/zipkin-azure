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


/**
 * Created by aliostad on 16/01/2017.
 */
@ConfigurationProperties("zipkin.collector.eventhub")
public class EventHubCollectorProperties {

  private String eventHubName = "zipkin";
  private String consumerGroupName = "$Default";
  private String eventHubConnectionString;
  private String storageConnectionString;
  private String storageContainerName;
  private int checkpointBatchSize = 10;

  private String processorHostName = "";
  private String storageBlobPrefix = getProcessorHostName();

  public String getEventHubName() {
    return eventHubName;
  }

  public void setEventHubName(String eventHubName) {
    this.eventHubName = eventHubName;
  }

  public String getConsumerGroupName() {
    return consumerGroupName;
  }

  public void setConsumerGroupName(String consumerGroupName) {
    this.consumerGroupName = consumerGroupName;
  }

  public String getEventHubConnectionString() {
    return eventHubConnectionString;
  }

  public void setEventHubConnectionString(String eventHubConnectionString) {
    this.eventHubConnectionString = eventHubConnectionString;
  }

  public String getStorageConnectionString() {
    return storageConnectionString;
  }

  public void setStorageConnectionString(String storageConnectionString) {
    this.storageConnectionString = storageConnectionString;
  }

  public String getStorageContainerName() {
    return storageContainerName;
  }

  public void setStorageContainerName(String storageContainerName) {
    this.storageContainerName = storageContainerName;
  }

  public int getCheckpointBatchSize() {
    return checkpointBatchSize;
  }

  public void setCheckpointBatchSize(int checkpointBatchSize) {
    this.checkpointBatchSize = checkpointBatchSize;
  }

  public String getProcessorHostName() {
    return processorHostName;
  }

  public void setProcessorHostName(String processorHostName) {
    this.processorHostName = processorHostName;
  }

  public String getStorageBlobPrefix() {
    return storageBlobPrefix;
  }

  public void setStorageBlobPrefix(String storageBlobPrefix) {
    this.storageBlobPrefix = storageBlobPrefix;
  }

  public EventHubCollector.Builder toBuilder() {
    EventHubCollector.Builder builder = EventHubCollector.builder()
        .eventHubConnectionString(eventHubConnectionString)
        .storageConnectionString(storageConnectionString);

    if (notEmpty(storageBlobPrefix)) {
      builder = builder.storageBlobPrefix(storageBlobPrefix);
    }

    if (notEmpty(processorHostName)) {
      builder = builder.processorHostName(processorHostName);
    }

    if (checkpointBatchSize > 0) {
      builder = builder.checkpointBatchSize(checkpointBatchSize);
    }

    if (notEmpty(consumerGroupName)) {
      builder = builder.consumerGroupName(consumerGroupName);
    }

    if (notEmpty(eventHubName)) {
      builder = builder.eventHubName(eventHubName);
    }

    if (notEmpty(storageContainerName)) {
      builder = builder.storageContainerName(storageContainerName);
    }

    return builder;
  }

  private static boolean notEmpty(String s) {
    return !(s == null || s.isEmpty());
  }
}
