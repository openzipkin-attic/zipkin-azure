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
package zipkin.autoconfigure.storage.applicationinsights;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import zipkin.storage.StorageComponent;
import zipkin.storage.applicationinsights.ApplicationInsightsStorage;
import java.util.concurrent.Executor;

/**
 * This storage accepts ApplicationInsights logs in a specified category. Each log entry is expected
 * to contain a single span, which is TBinaryProtocol big-endian, then base64 encoded. Decoded spans
 * are stored asynchronously.
 */
@Configuration
@EnableConfigurationProperties(ZipkinApplicationInsightsStorageProperties.class)
@ConditionalOnProperty(name = "zipkin.storage.type", havingValue = "applicationinsights")
@ConditionalOnMissingBean(StorageComponent.class)
public class ZipkinApplicationInsightsStorageAutoConfiguration {

  @Bean @ConditionalOnMissingBean(Executor.class) Executor executor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix("ZipkinApplicationInsightsStorage-");
    executor.initialize();
    return executor;
  }

  @Bean StorageComponent storage(Executor executor,
      ZipkinApplicationInsightsStorageProperties properties,
      @Value("${zipkin.storage.strict-trace-id:true}") boolean strictTraceId) {
    return ApplicationInsightsStorage.builder()
        .strictTraceId(strictTraceId)
        .instrumentationKey(properties.getInstrumentationKey())
        .applicationId(properties.getApplicationId())
        .apikey(properties.getApiKey())
        .executor(executor)
        .build();
  }
}
