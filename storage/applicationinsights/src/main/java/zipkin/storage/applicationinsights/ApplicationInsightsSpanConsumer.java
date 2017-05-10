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

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import zipkin.Span;
import zipkin.storage.StorageAdapters;

import static zipkin.internal.ApplyTimestampAndDuration.guessTimestamp;

final class ApplicationInsightsSpanConsumer implements StorageAdapters.SpanConsumer {

  private static TelemetryClient telemetry = new TelemetryClient();
  private static Gson gson = new Gson();
  private String namespace;

  public ApplicationInsightsSpanConsumer(String instrumentationKey) {
    TelemetryConfiguration.getActive().setInstrumentationKey(instrumentationKey);
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  @Override
  public void accept(List<Span> spans) {
    if (spans.size() > 0) {

      for (int i = 0; i < spans.size(); i++) {
        Span span = spans.get(i);
        Map<String, String> spanProps = new HashMap<String, String>();
        //set indexing properties, avoid null values for props
        spanProps.put("spanid", Long.toString(span.id));
        spanProps.put("traceId", Long.toString(span.traceId));
        spanProps.put("traceIdHigh", Long.toString(span.traceIdHigh));
        //namespace to support duplicate data
        spanProps.put("namespace", (this.namespace == null)?"":this.namespace);
        if (span.annotations != null
            && span.annotations.size() > 0
            && span.annotations.get(0).endpoint != null
            && span.annotations.get(0).endpoint.serviceName != null) {
          spanProps.put("Application", span.annotations.get(0).endpoint.serviceName);
        }
        spanProps.put("ActivityId", Long.toString(span.traceId));

        Long timestamp = guessTimestamp(span);
        JsonElement jsonElement = gson.toJsonTree(span, Span.class);
        if (timestamp != null) {
          jsonElement.getAsJsonObject().addProperty("timestamp", Long.toString(timestamp));
        }
        String res = gson.toJson(jsonElement);
        String msg = "{ \"Span\":" + res + "}";
        telemetry.trackTrace(msg, SeverityLevel.Critical, spanProps);
      }

      telemetry.flush();
      ApplicationInsightsClient.setNamespaceWaitStatus(this.namespace, false);
    }
  }
}