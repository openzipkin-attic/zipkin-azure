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
package zipkin.storage.applicationinsights;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import zipkin.autoconfigure.storage.applicationinsights.ZipkinApplicationInsightsStorageAutoConfiguration;
import zipkin.autoconfigure.storage.applicationinsights.ZipkinApplicationInsightsStorageProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.util.EnvironmentTestUtils.addEnvironment;

public class ZipkinApplicationInsightsStorageAutoConfigurationTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  AnnotationConfigApplicationContext context;

  @After
  public void close() {
    if (context != null) {
      context.close();
    }
  }

  @Test
  public void doesntProvidesStorageComponent_whenStorageTypeNotApplicationInsights() {
    context = new AnnotationConfigApplicationContext();
    addEnvironment(context, "zipkin.storage.type:elasticsearch");
    context.register(PropertyPlaceholderAutoConfiguration.class,
        ZipkinApplicationInsightsStorageAutoConfiguration.class);
    context.refresh();

    thrown.expect(NoSuchBeanDefinitionException.class);
    context.getBean(ApplicationInsightsStorage.class);
  }

  @Test
  public void providesStorageComponent_whenStorageTypeApplicationInsights() {
    context = new AnnotationConfigApplicationContext();
    addEnvironment(context, "zipkin.storage.type:applicationinsights");
    context.register(PropertyPlaceholderAutoConfiguration.class,
        ZipkinApplicationInsightsStorageAutoConfiguration.class);
    context.refresh();

    assertThat(context.getBean(ApplicationInsightsStorage.class)).isNotNull();
  }

  @Test
  public void canOverridesProperty_InstrumentationKey() {
    context = new AnnotationConfigApplicationContext();
    addEnvironment(context,
        "zipkin.storage.type:applicationinsights",
        "zipkin.storage.applicationinsights.instrumentationKey:xyz",
        "zipkin.storage.applicationinsights.applicationId:abc",
        "zipkin.storage.applicationinsights.apiKey:mnm"
    );
    context.register(PropertyPlaceholderAutoConfiguration.class,
        ZipkinApplicationInsightsStorageAutoConfiguration.class);
    context.refresh();

    assertThat(
        context.getBean(ZipkinApplicationInsightsStorageProperties.class).getInstrumentationKey())
        .isEqualTo("xyz");
  }
}