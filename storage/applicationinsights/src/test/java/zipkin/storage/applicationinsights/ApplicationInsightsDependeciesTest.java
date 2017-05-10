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

import zipkin.storage.DependenciesTest;
import java.util.UUID;

public class ApplicationInsightsDependeciesTest extends DependenciesTest {

  private UUID namespace;
  private String instrumentationKey = ""; //set test instrumentation key
  private String applicationId = ""; //set test application id
  private String apiKey = ""; //set test api key
  private int waitTimeInSeconds;

  @Override protected ApplicationInsightsStorage storage() {

    ApplicationInsightsStorage aiStorage = ApplicationInsightsStorage.builder()
        .instrumentationKey(this.instrumentationKey)
        .applicationId(this.applicationId)
        .apikey(this.apiKey)
        .namespace(this.namespace.toString())
        .readWaitTimeInSeconds(this.waitTimeInSeconds)
        .executor(Runnable::run)
        .build();

    return aiStorage;
  }

  @Override public void clear() {
    namespace = UUID.randomUUID();
    this.waitTimeInSeconds = 300;
    System.out.println("namespace: " + namespace);
  }
}
