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

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin.autoconfigure.collector.eventhub.EventHubCollectorAutoConfiguration;
import zipkin.autoconfigure.collector.eventhub.EventHubCollectorProperties;
import zipkin.collector.CollectorMetrics;
import zipkin.collector.CollectorSampler;
import zipkin.storage.InMemoryStorage;
import zipkin.storage.StorageComponent;

import static org.junit.Assert.assertEquals;
import static org.springframework.boot.test.EnvironmentTestUtils.addEnvironment;

public class EventHubCollectorAutoConfigurationTest {

  AnnotationConfigApplicationContext context;

  static final String dummyEventHubConnectionString =
      "endpoint=sb://someurl.net;SharedAccessKeyName=dumbo;SharedAccessKey=uius7y8ewychsih";
  static final String dummyStorageConnectionString = "UseDevelopmentStorage=true";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @After
  public void close() {
    if (context != null) context.close();
  }

  @Test
  public void doesntProvideCollectorComponent_whenEventHubConnectionStringUnset() {
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
  public void providesCollectorComponent_whenEventHubConnectionStringIsSet() {
    context = new AnnotationConfigApplicationContext();
    addEnvironment(context,
        "zipkin.collector.eventhub.eventHubConnectionString:" + dummyEventHubConnectionString);
    addEnvironment(context,
        "zipkin.collector.eventhub.storageConnectionString:" + dummyStorageConnectionString);
    context.register(PropertyPlaceholderAutoConfiguration.class,
        EventHubCollectorAutoConfiguration.class,
        InMemoryConfiguration.class);
    context.refresh();

    EventHubCollectorProperties props = context.getBean(EventHubCollectorProperties.class);
    assertEquals(dummyEventHubConnectionString, props.getEventHubConnectionString());
    assertEquals(dummyStorageConnectionString, props.getStorageConnectionString());
  }

  @Test
  public void provideCollectorComponent_canSetConsumerGroupName() {

    String consumerGroupName = "pashmak";

    context = new AnnotationConfigApplicationContext();
    addEnvironment(context,
        "zipkin.collector.eventhub.eventHubConnectionString:" + dummyEventHubConnectionString);
    addEnvironment(context,
        "zipkin.collector.eventhub.storageConnectionString:" + dummyStorageConnectionString);
    addEnvironment(context, "zipkin.collector.eventhub.consumerGroupName:" + consumerGroupName);
    context.register(PropertyPlaceholderAutoConfiguration.class,
        EventHubCollectorAutoConfiguration.class,
        InMemoryConfiguration.class);
    context.refresh();

    EventHubCollectorProperties props = context.getBean(EventHubCollectorProperties.class);
    assertEquals(consumerGroupName, props.getConsumerGroupName());
  }

  @Test
  public void provideCollectorComponent_canSetEventHubName() {

    String eventHubName = "pashmak";

    context = new AnnotationConfigApplicationContext();
    addEnvironment(context,
        "zipkin.collector.eventhub.eventHubConnectionString:" + dummyEventHubConnectionString);
    addEnvironment(context,
        "zipkin.collector.eventhub.storageConnectionString:" + dummyStorageConnectionString);
    addEnvironment(context, "zipkin.collector.eventhub.eventHubName:" + eventHubName);
    context.register(PropertyPlaceholderAutoConfiguration.class,
        EventHubCollectorAutoConfiguration.class,
        InMemoryConfiguration.class);
    context.refresh();

    EventHubCollectorProperties props = context.getBean(EventHubCollectorProperties.class);
    assertEquals(eventHubName, props.getEventHubName());
  }

  @Test
  public void provideCollectorComponent_canSetProcessorHostName() {

    String processorHostName = "pashmak";

    context = new AnnotationConfigApplicationContext();
    addEnvironment(context,
        "zipkin.collector.eventhub.eventHubConnectionString:" + dummyEventHubConnectionString);
    addEnvironment(context,
        "zipkin.collector.eventhub.storageConnectionString:" + dummyStorageConnectionString);
    addEnvironment(context, "zipkin.collector.eventhub.processorHostName:" + processorHostName);
    context.register(PropertyPlaceholderAutoConfiguration.class,
        EventHubCollectorAutoConfiguration.class,
        InMemoryConfiguration.class);
    context.refresh();

    EventHubCollectorProperties props = context.getBean(EventHubCollectorProperties.class);
    assertEquals(processorHostName, props.getProcessorHostName());
  }

  @Test
  public void provideCollectorComponent_canSetStorageBlobPrefix() {

    String storageBlobPrefix = "pashmak";

    context = new AnnotationConfigApplicationContext();
    addEnvironment(context,
        "zipkin.collector.eventhub.eventHubConnectionString:" + dummyEventHubConnectionString);
    addEnvironment(context,
        "zipkin.collector.eventhub.storageConnectionString:" + dummyStorageConnectionString);
    addEnvironment(context, "zipkin.collector.eventhub.storageBlobPrefix:" + storageBlobPrefix);
    context.register(PropertyPlaceholderAutoConfiguration.class,
        EventHubCollectorAutoConfiguration.class,
        InMemoryConfiguration.class);
    context.refresh();

    EventHubCollectorProperties props = context.getBean(EventHubCollectorProperties.class);
    assertEquals(storageBlobPrefix, props.getStorageBlobPrefix());
  }

  @Test
  public void provideCollectorComponent_canSetStorageContainerName() {

    String storageContainerName = "pashmak";

    context = new AnnotationConfigApplicationContext();
    addEnvironment(context,
        "zipkin.collector.eventhub.eventHubConnectionString:" + dummyEventHubConnectionString);
    addEnvironment(context,
        "zipkin.collector.eventhub.storageConnectionString:" + dummyStorageConnectionString);
    addEnvironment(context,
        "zipkin.collector.eventhub.storageContainerName:" + storageContainerName);
    context.register(PropertyPlaceholderAutoConfiguration.class,
        EventHubCollectorAutoConfiguration.class,
        InMemoryConfiguration.class);
    context.refresh();

    EventHubCollectorProperties props = context.getBean(EventHubCollectorProperties.class);
    assertEquals(storageContainerName, props.getStorageContainerName());
  }

  @Test
  public void provideCollectorComponent_canSetCheckpointBatchSize() {

    int checkpointBatchSize = 1000;

    context = new AnnotationConfigApplicationContext();
    addEnvironment(context,
        "zipkin.collector.eventhub.eventHubConnectionString:" + dummyEventHubConnectionString);
    addEnvironment(context,
        "zipkin.collector.eventhub.storageConnectionString:" + dummyStorageConnectionString);
    addEnvironment(context,
        "zipkin.collector.eventhub.checkpoint-batch-size:" + checkpointBatchSize);
    context.register(PropertyPlaceholderAutoConfiguration.class,
        EventHubCollectorAutoConfiguration.class,
        InMemoryConfiguration.class);
    context.refresh();

    EventHubCollectorProperties props = context.getBean(EventHubCollectorProperties.class);
    assertEquals(checkpointBatchSize, props.getCheckpointBatchSize());
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
