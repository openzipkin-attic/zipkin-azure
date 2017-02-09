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

  static final String dummyConnectionString =
      "endpoint=sb://someurl.net;SharedAccessKeyName=dumbo;SharedAccessKey=uius7y8ewychsih";
  static final String dummyStorageConnectionString = "UseDevelopmentStorage=true";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @After
  public void close() {
    if (context != null) context.close();
  }

  @Test
  public void doesntProvideCollectorComponent_whenConnectionStringUnset() {
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
  public void providesCollectorComponent_whenConnectionStringIsSet() {
    context = new AnnotationConfigApplicationContext();
    addEnvironment(context,
        "zipkin.collector.eventhub.connection-string:" + dummyConnectionString);
    addEnvironment(context,
        "zipkin.collector.eventhub.storage.connection-string:" + dummyStorageConnectionString);
    context.register(PropertyPlaceholderAutoConfiguration.class,
        EventHubCollectorAutoConfiguration.class,
        InMemoryConfiguration.class);
    context.refresh();

    EventHubCollectorProperties props = context.getBean(EventHubCollectorProperties.class);
    assertEquals(dummyConnectionString, props.getConnectionString());
    assertEquals(dummyStorageConnectionString, props.getStorage().getConnectionString());
  }

  @Test
  public void provideCollectorComponent_canSetConsumerGroup() {

    String consumerGroup = "pashmak";

    context = new AnnotationConfigApplicationContext();
    addEnvironment(context,
        "zipkin.collector.eventhub.connection-string:" + dummyConnectionString);
    addEnvironment(context,
        "zipkin.collector.eventhub.storage.connection-string:" + dummyStorageConnectionString);
    addEnvironment(context, "zipkin.collector.eventhub.consumer-group:" + consumerGroup);
    context.register(PropertyPlaceholderAutoConfiguration.class,
        EventHubCollectorAutoConfiguration.class,
        InMemoryConfiguration.class);
    context.refresh();

    EventHubCollectorProperties props = context.getBean(EventHubCollectorProperties.class);
    assertEquals(consumerGroup, props.getConsumerGroup());
  }

  @Test
  public void provideCollectorComponent_canSetName() {

    String name = "pashmak";

    context = new AnnotationConfigApplicationContext();
    addEnvironment(context,
        "zipkin.collector.eventhub.connection-string:" + dummyConnectionString);
    addEnvironment(context,
        "zipkin.collector.eventhub.storage.connection-string:" + dummyStorageConnectionString);
    addEnvironment(context, "zipkin.collector.eventhub.name:" + name);
    context.register(PropertyPlaceholderAutoConfiguration.class,
        EventHubCollectorAutoConfiguration.class,
        InMemoryConfiguration.class);
    context.refresh();

    EventHubCollectorProperties props = context.getBean(EventHubCollectorProperties.class);
    assertEquals(name, props.getName());
  }

  @Test
  public void provideCollectorComponent_canSetProcessorHost() {

    String processorHost = "pashmak";

    context = new AnnotationConfigApplicationContext();
    addEnvironment(context,
        "zipkin.collector.eventhub.connection-string:" + dummyConnectionString);
    addEnvironment(context,
        "zipkin.collector.eventhub.storage.connection-string:" + dummyStorageConnectionString);
    addEnvironment(context, "zipkin.collector.eventhub.processor-host:" + processorHost);
    context.register(PropertyPlaceholderAutoConfiguration.class,
        EventHubCollectorAutoConfiguration.class,
        InMemoryConfiguration.class);
    context.refresh();

    EventHubCollectorProperties props = context.getBean(EventHubCollectorProperties.class);
    assertEquals(processorHost, props.getProcessorHost());
  }

  @Test
  public void provideCollectorComponent_canSetStorageBlobPrefix() {

    String storageBlobPrefix = "pashmak";

    context = new AnnotationConfigApplicationContext();
    addEnvironment(context,
        "zipkin.collector.eventhub.connection-string:" + dummyConnectionString);
    addEnvironment(context,
        "zipkin.collector.eventhub.storage.connection-string:" + dummyStorageConnectionString);
    addEnvironment(context, "zipkin.collector.eventhub.storage.blob-prefix:" + storageBlobPrefix);
    context.register(PropertyPlaceholderAutoConfiguration.class,
        EventHubCollectorAutoConfiguration.class,
        InMemoryConfiguration.class);
    context.refresh();

    EventHubCollectorProperties props = context.getBean(EventHubCollectorProperties.class);
    assertEquals(storageBlobPrefix, props.getStorage().getBlobPrefix());
  }

  @Test
  public void provideCollectorComponent_canSetStorageContainer() {

    String storageContainer = "pashmak";

    context = new AnnotationConfigApplicationContext();
    addEnvironment(context,
        "zipkin.collector.eventhub.connection-string:" + dummyConnectionString);
    addEnvironment(context,
        "zipkin.collector.eventhub.storage.connection-string:" + dummyStorageConnectionString);
    addEnvironment(context,
        "zipkin.collector.eventhub.storage.container:" + storageContainer);
    context.register(PropertyPlaceholderAutoConfiguration.class,
        EventHubCollectorAutoConfiguration.class,
        InMemoryConfiguration.class);
    context.refresh();

    EventHubCollectorProperties props = context.getBean(EventHubCollectorProperties.class);
    assertEquals(storageContainer, props.getStorage().getContainer());
  }

  @Test
  public void provideCollectorComponent_canSetCheckpointBatchSize() {

    int checkpointBatchSize = 1000;

    context = new AnnotationConfigApplicationContext();
    addEnvironment(context,
        "zipkin.collector.eventhub.connection-string:" + dummyConnectionString);
    addEnvironment(context,
        "zipkin.collector.eventhub.storage.connection-string:" + dummyStorageConnectionString);
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
