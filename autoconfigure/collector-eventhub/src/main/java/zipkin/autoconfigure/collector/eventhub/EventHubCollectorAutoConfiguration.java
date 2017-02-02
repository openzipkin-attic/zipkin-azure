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

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import zipkin.collector.CollectorMetrics;
import zipkin.collector.CollectorSampler;
import zipkin.collector.eventhub.EventHubCollector;
import zipkin.storage.StorageComponent;

import java.util.logging.Level;
import java.util.logging.Logger;

@Configuration
@EnableConfigurationProperties(EventHubCollectorProperties.class)
@Conditional(EventHubSetCondition.class)
public class EventHubCollectorAutoConfiguration {

  private static final Logger logger = Logger.getLogger(EventHubCollectorAutoConfiguration.class.getName());


  @Bean
  EventHubCollector eventHubCollector(EventHubCollectorProperties properties,
                                      CollectorSampler sampler,
                                      CollectorMetrics metrics,
                                      StorageComponent storage) {


    logger.log(Level.INFO,"===========EventHubCollectorAutoConfiguration==============");

    return properties.toBuilder()
        .sampler(sampler)
        .storage(storage)
        .metrics(metrics)
        .build()
        .start();
  }
}

