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

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin.collector.CollectorMetrics;
import zipkin.collector.CollectorSampler;
import zipkin.collector.eventhub.EventHubCollector;
import zipkin.storage.InMemoryStorage;
import zipkin.storage.StorageComponent;

import static org.junit.Assert.*;
import static org.springframework.boot.test.EnvironmentTestUtils.addEnvironment;

// this comment has no meaning

public class EventHubCollectorAutoConfigurationTest {

  AnnotationConfigApplicationContext context;

  String dummyEventHubConnectionString = "endpoint=sb://someurl.net;SharedAccessKeyName=dumbo;SharedAccessKey=uius7y8ewychsih";
  String dummyStorageConnectionString = "a=b;c=d";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @After
  public void close() {
    if (context != null) context.close();
  }

  @Test
  public void doesntProvideCollectorComponent_whenSqsQueueUrlUnset() {
    context = new AnnotationConfigApplicationContext();
    context.register(PropertyPlaceholderAutoConfiguration.class,
        EventHubCollectorProperties.class,
        EventHubCollectorAutoConfiguration.class,
        InMemoryConfiguration.class);
    context.refresh();

    thrown.expect(NoSuchBeanDefinitionException.class);
    context.getBean(EventHubCollector.class);
  }

  @Test
  public void provideCollectorComponent_whenEventHubConnectionStringIsSet() {
    context = new AnnotationConfigApplicationContext();
    addEnvironment(context, "zipkin.collector.eventhub.storageConnectionString:" + dummyStorageConnectionString);
    addEnvironment(context, "zipkin.collector.eventhub.eventHubConnectionString:" + dummyEventHubConnectionString);
    context.register(PropertyPlaceholderAutoConfiguration.class,
        EventHubCollectorAutoConfiguration.class,
        InMemoryConfiguration.class);
    context.refresh();

    EventHubCollector collector = context.getBean(EventHubCollector.class);
    assertNotNull(collector);
    assertEquals(dummyEventHubConnectionString, collector.getEventHubConnectionString());
    assertEquals(dummyStorageConnectionString, collector.getStorageConnectionString());
  }

  @Test
  public void provideCollectorComponent_canSetConsumerGroupName() {

    String consumerGroupName = "pashmak";

    context = new AnnotationConfigApplicationContext();
    addEnvironment(context, "zipkin.collector.eventhub.storageConnectionString:" + dummyEventHubConnectionString);
    addEnvironment(context, "zipkin.collector.eventhub.eventHubConnectionString:" + dummyStorageConnectionString);
    addEnvironment(context, "zipkin.collector.eventhub.consumerGroupName:" + consumerGroupName);
    context.register(PropertyPlaceholderAutoConfiguration.class,
        EventHubCollectorAutoConfiguration.class,
        InMemoryConfiguration.class);
    context.refresh();

    EventHubCollector collector = context.getBean(EventHubCollector.class);
    assertNotNull(collector);
    assertEquals(consumerGroupName, collector.getConsumerGroupName());
  }

  @Test
  public void provideCollectorComponent_canSetEventHubName() {

    String eventHubName = "pashmak";

    context = new AnnotationConfigApplicationContext();
    addEnvironment(context, "zipkin.collector.eventhub.storageConnectionString:" + dummyEventHubConnectionString);
    addEnvironment(context, "zipkin.collector.eventhub.eventHubConnectionString:" + dummyStorageConnectionString);
    addEnvironment(context, "zipkin.collector.eventhub.eventHubName:" + eventHubName);
    context.register(PropertyPlaceholderAutoConfiguration.class,
        EventHubCollectorAutoConfiguration.class,
        InMemoryConfiguration.class);
    context.refresh();

    EventHubCollector collector = context.getBean(EventHubCollector.class);
    assertNotNull(collector);
    assertEquals(eventHubName, collector.getEventHubName());
  }

  @Test
  public void provideCollectorComponent_canSetProcessorHostName() {

    String processorHostName = "pashmak";

    context = new AnnotationConfigApplicationContext();
    addEnvironment(context, "zipkin.collector.eventhub.storageConnectionString:" + dummyEventHubConnectionString);
    addEnvironment(context, "zipkin.collector.eventhub.eventHubConnectionString:" + dummyStorageConnectionString);
    addEnvironment(context, "zipkin.collector.eventhub.processorHostName:" + processorHostName);
    context.register(PropertyPlaceholderAutoConfiguration.class,
        EventHubCollectorAutoConfiguration.class,
        InMemoryConfiguration.class);
    context.refresh();

    EventHubCollector collector = context.getBean(EventHubCollector.class);
    assertNotNull(collector);
    assertEquals(processorHostName, collector.getProcessorHostName());
  }

  @Test
  public void provideCollectorComponent_canSetStorageBlobPrefix() {

    String storageBlobPrefix = "pashmak";

    context = new AnnotationConfigApplicationContext();
    addEnvironment(context, "zipkin.collector.eventhub.storageConnectionString:" + dummyEventHubConnectionString);
    addEnvironment(context, "zipkin.collector.eventhub.eventHubConnectionString:" + dummyStorageConnectionString);
    addEnvironment(context, "zipkin.collector.eventhub.storageBlobPrefix:" + storageBlobPrefix);
    context.register(PropertyPlaceholderAutoConfiguration.class,
        EventHubCollectorAutoConfiguration.class,
        InMemoryConfiguration.class);
    context.refresh();

    EventHubCollector collector = context.getBean(EventHubCollector.class);
    assertNotNull(collector);
    assertEquals(storageBlobPrefix, collector.getStorageBlobPrefix());
  }

  @Test
  public void provideCollectorComponent_canSetStorageContainerName() {

    String storageContainerName = "pashmak";

    context = new AnnotationConfigApplicationContext();
    addEnvironment(context, "zipkin.collector.eventhub.storageConnectionString:" + dummyEventHubConnectionString);
    addEnvironment(context, "zipkin.collector.eventhub.eventHubConnectionString:" + dummyStorageConnectionString);
    addEnvironment(context, "zipkin.collector.eventhub.storageContainerName:" + storageContainerName);
    context.register(PropertyPlaceholderAutoConfiguration.class,
        EventHubCollectorAutoConfiguration.class,
        InMemoryConfiguration.class);
    context.refresh();

    EventHubCollector collector = context.getBean(EventHubCollector.class);
    assertNotNull(collector);
    assertEquals(storageContainerName, collector.getStorageContainerName());
  }

  @Test
  public void provideCollectorComponent_canSetCheckpointBatchSize() {

    int checkpointBatchSize = 1000;

    context = new AnnotationConfigApplicationContext();
    addEnvironment(context, "zipkin.collector.eventhub.storageConnectionString:" + dummyEventHubConnectionString);
    addEnvironment(context, "zipkin.collector.eventhub.eventHubConnectionString:" + dummyStorageConnectionString);
    addEnvironment(context, "zipkin.collector.eventhub.checkpoint-batch-size:" + checkpointBatchSize);
    context.register(PropertyPlaceholderAutoConfiguration.class,
        EventHubCollectorAutoConfiguration.class,
        InMemoryConfiguration.class);
    context.refresh();

    EventHubCollector collector = context.getBean(EventHubCollector.class);
    assertNotNull(collector);
    assertEquals(checkpointBatchSize, collector.getCheckpointBatchSize());
  }


  @Configuration
  static class InMemoryConfiguration {
    @Bean
    CollectorSampler sampler() {
      return CollectorSampler.ALWAYS_SAMPLE;
    }

    @Bean
    CollectorMetrics metrics() {
      return CollectorMetrics.NOOP_METRICS;
    }

    @Bean
    StorageComponent storage() {
      return new InMemoryStorage();
    }
  }

}
