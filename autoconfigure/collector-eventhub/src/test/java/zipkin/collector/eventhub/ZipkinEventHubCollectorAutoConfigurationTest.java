/**
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
package zipkin.collector.eventhub;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin.autoconfigure.collector.eventhub.ZipkinEventHubCollectorAutoConfiguration;
import zipkin.autoconfigure.collector.eventhub.ZipkinEventHubCollectorProperties;
import zipkin.collector.CollectorMetrics;
import zipkin.collector.CollectorSampler;
import zipkin.storage.InMemoryStorage;
import zipkin.storage.StorageComponent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.util.EnvironmentTestUtils.addEnvironment;

public class ZipkinEventHubCollectorAutoConfigurationTest {
  static final String CONNECTION_STRING =
      "endpoint=sb://someurl.net;SharedAccessKeyName=dumbo;SharedAccessKey=uius7y8ewychsih";
  static final String STORAGE_CONNECTION_STRING = "UseDevelopmentStorage=true";

  AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @After
  public void close() {
    if (context != null) context.close();
  }

  @Test
  public void doesntProvideCollectorComponent_whenConnectionStringsUnset() {
    context.register(
        PropertyPlaceholderAutoConfiguration.class,
        ZipkinEventHubCollectorAutoConfiguration.class,
        TestConfiguration.class
    );
    context.refresh();

    thrown.expect(NoSuchBeanDefinitionException.class);
    context.getBean(EventHubCollector.class);
  }

  @Test
  public void providesCollectorComponent_whenConnectionStringsSet() {
    addEnvironment(context,
        "zipkin.collector.eventhub.connection-string:" + CONNECTION_STRING);
    addEnvironment(context,
        "zipkin.collector.eventhub.storage.connection-string:"
            + STORAGE_CONNECTION_STRING);
    context.register(
        PropertyPlaceholderAutoConfiguration.class,
        ZipkinEventHubCollectorAutoConfiguration.class,
        TestConfiguration.class
    );
    context.refresh();

    ZipkinEventHubCollectorProperties props = context.getBean(ZipkinEventHubCollectorProperties.class);
    assertThat(props.getConnectionString())
        .isEqualTo(CONNECTION_STRING);
    assertThat(props.getStorage().getConnectionString())
        .isEqualTo(STORAGE_CONNECTION_STRING);
  }

  @Configuration static class TestConfiguration {
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
