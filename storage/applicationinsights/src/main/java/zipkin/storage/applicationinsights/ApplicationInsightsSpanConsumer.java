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
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.telemetry.Duration;
<<<<<<< HEAD
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
=======
>>>>>>> 78a8ebd7abe80096951bdf798ed9965d52b42afa
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
<<<<<<< HEAD
import org.apache.http.HttpStatus;
=======
>>>>>>> 78a8ebd7abe80096951bdf798ed9965d52b42afa
import zipkin.Annotation;
import zipkin.Span;
import zipkin.storage.StorageAdapters;
import zipkin.Constants;

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

        String spanId = Long.toString(span.id);
        String parentSpanId = span.parentId != null? Long.toString(span.parentId): null;

        String traceId = Long.toString(span.traceId);
        String traceIdHigh = Long.toString(span.traceIdHigh);
        String namespace = (this.namespace == null) ? "" : this.namespace;

        spanProps.put("spanid", spanId);
        spanProps.put("traceId", traceId);
        spanProps.put("traceIdHigh", traceIdHigh);

        if(parentSpanId!= null)
          spanProps.put("OperationParentId", parentSpanId);

        //namespace to support duplicate data
        spanProps.put("namespace", namespace);
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
        //data model changes
        telemetry.getContext().getOperation().setId(traceId);
        telemetry.getContext().getOperation().setName(span.name);

        for (Annotation annotation : span.annotations) {

          if (annotation.value.equalsIgnoreCase(Constants.CLIENT_SEND)) {
            String spanName = span.name !=null && !span.name.isEmpty()?span.name:Constants.CLIENT_SEND;
            telemetry.trackDependency(spanName, "request", new Duration(span.duration==null?0L:span.duration),
                true);
          }
          else if(annotation.value.equalsIgnoreCase(Constants.SERVER_SEND)){
            String spanName = span.name !=null && !span.name.isEmpty()?span.name:Constants.SERVER_RECV;
            telemetry.trackRequest(new RequestTelemetry(spanName, new Date(timestamp), span.duration,
                "Ok",true));
          }
        }

        telemetry.trackTrace(msg, SeverityLevel.Critical, spanProps);
      }

      telemetry.flush();
      ApplicationInsightsClient.setNamespaceWaitStatus(this.namespace, false);
    }
  }
}