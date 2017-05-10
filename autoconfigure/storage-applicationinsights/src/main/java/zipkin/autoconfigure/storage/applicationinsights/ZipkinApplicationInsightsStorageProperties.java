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

import java.util.concurrent.TimeUnit;
import org.springframework.boot.context.properties.ConfigurationProperties;
import zipkin.storage.applicationinsights.ApplicationInsightsStorage;

@ConfigurationProperties("zipkin.storage.applicationinsights")
public class ZipkinApplicationInsightsStorageProperties {

  private String instrumentationKey;
  private String applicationId;
  private String apiKey;

  public String getInstrumentationKey() {
    return instrumentationKey;
  }

  public void setInstrumentationKey(String key) {
    this.instrumentationKey = key;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(String key) {
    this.applicationId = key;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String key) {
    this.apiKey = key;
  }
}
