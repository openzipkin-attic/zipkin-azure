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

import com.google.gson.Gson;
import java.util.*;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import javax.json.JsonObject;
import javax.json.JsonArray;
import javax.json.JsonReader;
import javax.json.Json;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import zipkin.internal.CorrectForClockSkew;
import zipkin.internal.MergeById;
import zipkin.storage.QueryRequest;
import zipkin.Span;

public class ApplicationInsightsClient {

  private final String AI_RESTAPI_QUERYURLSTUB =
      "https://api.applicationinsights.io/beta/apps/%s/query?query=%s";
  private final String AI_APPLICATIONID;
  private final String AI_APIKEY;
  private boolean strictTraceId = true;
  private int waitTimeInSeconds;
  private String namespace;
  private static Map<String, Boolean> namespaceWaitStatus = new HashMap<String, Boolean>();
  private static Gson gson = new Gson();

  private final String AI_QUERYTOGETUNIQUESERVICENAMES = "let namespace = %s;"
      + "let spansData = traces "
      + "| where isnull(namespace) or isempty(tostring(namespace)) or customDimensions.['namespace'] == tostring(namespace)"
      + "| project span = parsejson(message).Span  ; "
      + "let spans = materialize(spansData);"
      + "spans"
      + "| extend annotations = parsejson(span).annotations "
      + "| mvexpand annotations  "
      + "| where isnotnull(annotations.endpoint.serviceName) and isnotempty(tostring(annotations.endpoint.serviceName))"
      + "| summarize  by uniqueServiceNames = tostring(annotations.endpoint.serviceName)   "
      + "| union"
      + "( "
      + "spans"
      + "| extend binAnnotations = parsejson(span).binaryAnnotations "
      + "| mvexpand binAnnotations  "
      + "| where isnotnull(binAnnotations.endpoint.serviceName) and  isnotempty(tostring(binAnnotations.endpoint.serviceName))"
      + "| summarize by  uniqueServiceNames = tostring(binAnnotations.endpoint.serviceName)"
      + ") | summarize by uniqueServiceNames;";

  private final String AI_QUERYTOGETUNIQUESPANNAMES = "let namespace = %s;"
      + "let service = %s;"
      + ""
      + "let spansData = traces"
      + "| where isnull(namespace) or isempty(tostring(namespace)) or customDimensions.['namespace'] == tostring(namespace)"
      + "| project span = parsejson(message).Span; "
      + ""
      + "let spans = materialize(spansData);"
      + ""
      + "let spanNameSet = spans"
      + "| extend anns = span.annotations"
      + "| mvexpand anns"
      + "| where (tolower(service) == 'all services' or service == 'null' or tostring(anns.endpoint.serviceName) == service)"
      + "| summarize by spanName = tostring(span.name)"
      + "| union"
      + "("
      + "spans"
      + "| extend binAnns = span.binaryAnnotations"
      + "| mvexpand binAnns"
      + "| where (tolower(service) == 'all services' or service == 'null' or tostring(binAnns.endpoint.serviceName) == service)"
      + "| summarize by spanName = tostring(span.name)"
      + ")"
      + "| summarize by spanNames = tostring(spanName)"
      + "| sort by spanNames asc;spanNameSet";

  private final String AI_QUERYTOGETTRACES = "let namespace = %s;"
      + "let service = %s;"
      + "let spanName = %s;"
      + "let starttime = %s;"
      + "let endtime = %s;"
      + "let minDuration = %s;"
      + "let maxDuration = %s;"
      + "let rowLimit = %s;"
      + "let annotationList = dynamic(%s);"
      + "let binAnnotationKeyValuePairs =  dynamic(%s);"
      + "let strictTraceId = %s;"
      + "let spansData = traces"
      + "| where isnull(namespace) or isempty(tostring(namespace)) or customDimensions.['namespace'] == tostring(namespace)"
      + "| extend span = parsejson(message).Span"
      + "| where ((minDuration == 'null' or tolong(span.duration) >= tolong(minDuration)) and (maxDuration == 'null' or tolong(span.duration) <= tolong(maxDuration))) and (tolower(spanName) == 'all'  or spanName == 'null' or (isnotnull(span.name)  and tostring(span.name) == spanName)) and tolong(span.timestamp) <= tolong(endtime) and tolong(span.timestamp) >= tolong(starttime)"
      + "| project span; "
      + ""
      + "let spans = materialize(spansData);"
      + ""
      + "let traceIds = spans"
      + "| extend anns = parsejson(span).annotations"
      + "| mvexpand anns"
      + "| where (tolower(service) == 'all services' or service == 'null' or tostring(anns.endpoint.serviceName) == service) and (annotationList == 'null' or isempty(tostring(annotationList)) or anns.value in (annotationList))"
      + "| summarize ann_set = makeset(tostring(anns.value)), spanList = makeset(span)  by tostring(span.traceIdHigh), tostring(span.traceId), tostring(span.id)"
      + "| union (    spans"
      + "| extend binAnnotations = iff(arraylength(parsejson(span.binaryAnnotations))==0, dynamic(''), (span.binaryAnnotations))"
      + "| mvexpand binAnnotations"
      + "| where ((binAnnotationKeyValuePairs == 'null' or isempty(tostring(binAnnotationKeyValuePairs)) or strcat(binAnnotations.key, binAnnotations.value) in (binAnnotationKeyValuePairs)) or (annotationList == 'null' or isempty(tostring(annotationList)) or binAnnotations.key in (annotationList))) and (tolower(service) == 'all services' or service == 'null' or tostring(binAnnotations.endpoint.serviceName) == service)"
      + "| summarize ann_set = makeset(iff(isnotnull(binAnnotations.key) and binAnnotations.key in (annotationList),tostring(binAnnotations.key),\" \")) "
      + ", binAnn_KeyValue = makeset(iff(isnotnull(binAnnotations.key) and isnotnull(binAnnotations.value) and strcat(binAnnotations.key, binAnnotations.value) in (binAnnotationKeyValuePairs),strcat(binAnnotations.key, binAnnotations.value), \" \")) , spanList = makeset(span) "
      + " by tostring(span.traceIdHigh), tostring(span.traceId), tostring(span.id))"
      + "| summarize annsSet= makeset(ann_set), binAnnkV = makeset(binAnn_KeyValue), makeset(spanList) by tostring(span_traceIdHigh), tostring(span_traceId), tostring(span_id)"
      + "| where  (annotationList == 'null' or isempty(tostring(annotationList)) or iff(tostring(annsSet) contains \" \", arraylength(annsSet)-1, arraylength(annsSet)) ==  arraylength(annotationList)) and (binAnnotationKeyValuePairs == 'null' or isempty(tostring(binAnnotationKeyValuePairs)) or  iff(tostring(binAnnkV) contains \" \", arraylength(binAnnkV)-1, arraylength(binAnnkV)) == arraylength(binAnnotationKeyValuePairs))"
      + "| distinct strcat(tostring(parsejson(set_spanList)[0].traceIdHigh),tostring(parsejson(set_spanList)[0].traceId));"
      + ""
      + "let resultTraces = traces"
      + "| where (isnull(namespace) or isempty(tostring(namespace)) or customDimensions.['namespace'] == tostring(namespace)) and (strcat(customDimensions.['traceIdHigh'],customDimensions.['traceId']) in (traceIds))"
      + "| project span = parsejson(message).Span"
      + "| summarize spanList = makeset(span), tsOfLastSpan = min(tolong(span.timestamp)) by iff(strictTraceId == false,'' , tostring(span.traceIdHigh)), tostring(span.traceId)"
      + "| sort by tsOfLastSpan desc"
      + "| take rowLimit;"
      + "resultTraces"
      + "| project spanList;";

  private final String AI_QUERYTOGETTRACE = "let namespace = %s;"
      + "let traceId = %s;"
      + "let traceIdHigh = %s;"
      + "let strictTraceId = %s;"
      + "traces"
      + "| where ( isnull(namespace) or isempty(tostring(namespace)) or customDimensions.['namespace'] == tostring(namespace)) "
      + "and (customDimensions.['traceId'] == traceId)"
      + "and (strictTraceId == false or isempty(tostring(traceIdHigh)) or isnull(customDimensions.['traceIdHigh']) or customDimensions.['traceIdHigh'] == '0L' or customDimensions.['traceIdHigh'] == traceIdHigh)"
      + "| project span = parsejson(message).Span;";

  public ApplicationInsightsClient(String appId, String apiKey) {
    this.AI_APPLICATIONID = appId;
    this.AI_APIKEY = apiKey;
  }

  public static void setNamespaceWaitStatus(String namespace, Boolean value) {
    namespaceWaitStatus.put(namespace, value);
  }

  public void setStrictTraceId(boolean isStrict) {
    this.strictTraceId = isStrict;
  }

  public void setNamespace(String name) {
    if (!namespaceWaitStatus.containsKey(name)) {
      namespaceWaitStatus.put(name, false);
    }
    this.namespace = name;
  }

  public void setWaitTimeInSeconds(int timeInSeconds) {
    this.waitTimeInSeconds = timeInSeconds;
  }

  public List<Span> getTrace(String traceIdHigh, String traceId) {
    String response = "";
    String currentNamespace = this.namespace == null ? "" : this.namespace;
    String getTraceQuery =
        String.format(AI_QUERYTOGETTRACE, addSingleQuote(currentNamespace), addSingleQuote(traceId),
            addSingleQuote(traceIdHigh), this.strictTraceId);
    String getTraceQueryUrl =
        String.format(AI_RESTAPI_QUERYURLSTUB, AI_APPLICATIONID, getTraceQuery);

    waitForRecentWrites();
    try {
      response = runAIQuery(getTraceQueryUrl);
    } catch (IOException e) {
      throw new RuntimeException("Error querying for trace traceIdHigh:"
          + traceIdHigh
          + "traceIdLow: "
          + traceId
          + "Error msg: "
          + e.getMessage());
    }

    JsonArray rows = getAIQueryResult(response);
    if (rows == null || rows.size() == 0) {
      return null;
    }
    List<Span> spans = readSpansFromJsonArrayOfRows(rows, 0);

    return spans;
  }

  public void waitForRecentWrites() {
    if (!isNullOrEmpty(this.namespace)) {
      if (!namespaceWaitStatus.get(this.namespace)) {
        try {
          Thread.sleep(this.waitTimeInSeconds * 1000);
          namespaceWaitStatus.put(this.namespace, true);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public List<List<Span>> getTraces(final QueryRequest request) {

    long endTimestamp = request.endTs * 1000;
    long startTimestamp = endTimestamp - request.lookback * 1000;

    String currentNamespace = this.namespace == null ? "" : this.namespace;
    waitForRecentWrites();

    String minDuration = request.minDuration == null ? "null" : Long.toString(request.minDuration);
    String maxDuration = request.maxDuration == null ? "null" : Long.toString(request.maxDuration);
    String annotations = toCommaSeperatedQuotedWords(request.annotations);
    String binaryAnnotations = toCommaSeperatedKeyValueWords(request.binaryAnnotations);
    String getTracesQuery = String.format(AI_QUERYTOGETTRACES, addSingleQuote(currentNamespace),
        addSingleQuote(request.serviceName), addSingleQuote(request.spanName),
        addSingleQuote(Long.toString(startTimestamp)), addSingleQuote(Long.toString(endTimestamp)),
        addSingleQuote(minDuration), addSingleQuote(maxDuration), request.limit, annotations,
        binaryAnnotations, this.strictTraceId);
    String getTracesQueryUrl =
        String.format(AI_RESTAPI_QUERYURLSTUB, AI_APPLICATIONID, getTracesQuery);
    String response = "";

    try {
      response = runAIQuery(getTracesQueryUrl);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (response == null || response.isEmpty()) {
      return null;
    }

    List<List<Span>> traces = new ArrayList<List<Span>>();

    JsonArray rows = getAIQueryResult(response);
    if (rows != null) {
      List<Span> spanList = new ArrayList<Span>();
      for (int j = 0; j < rows.size(); j++) {
        JsonArray currentRow = rows.getJsonArray(j);
        // read first column for spans in a trace
        if (!currentRow.isNull(0)) {
          JsonArray spanArr = getJsonArrayFromString(currentRow.getString(0));
          for (int i = 0; i < spanArr.size(); i++) {
            Span span = null;
            JsonObject spanObj = null;
            try {
              spanObj = spanArr.getJsonObject(i);
              span = gson.fromJson(spanObj.toString(), Span.class);
            } catch (Exception e) {
              System.out.println(
                  String.format("Error converting span: %s , from json to gson", spanObj));
            }
            if (span != null) {
              spanList.add(span);
            }
          }
        }
        try {
          traces.add(CorrectForClockSkew.apply(MergeById.apply(spanList)));
        } catch (Exception e) {
          System.out.println("Error reading trace" + spanList.get(0).traceId);
        }
        spanList.clear();
      }
    }

    return traces;
  }

  /*
   * Finds all unique service names till today, returns if there is existing value
   */
  public List<String> getServiceNames() {

    String currentNamespace = this.namespace == null ? "" : this.namespace;
    waitForRecentWrites();
    final String getServicesNamesQuery =
        String.format(AI_QUERYTOGETUNIQUESERVICENAMES, addSingleQuote(currentNamespace));

    String getServiceNamesQueryUrl = String.format(AI_RESTAPI_QUERYURLSTUB, AI_APPLICATIONID,
        getServicesNamesQuery);
    String response = "";
    try {
      response = runAIQuery(getServiceNamesQueryUrl);
    } catch (IOException e) {
      e.printStackTrace();
    }
    List<String> serviceNames = new ArrayList<String>();
    JsonArray rows = getAIQueryResult(response);

    if (rows != null) {
      for (int j = 0; j < rows.size(); j++) {
        JsonArray currentRow = rows.getJsonArray(j);
        // read first column for serviceName
        String serviceName = currentRow.getString(0);

        if (serviceName != null || !serviceName.isEmpty()) {
          serviceNames.add(serviceName);
        }
      }
    }
    Collections.sort(serviceNames);
    return serviceNames;
  }

  public List<String> getSpanNames(String serviceName) {

    String currentNamespace = this.namespace == null ? "" : this.namespace;
    final String getUniqueSpanNamesQuery =
        String.format(AI_QUERYTOGETUNIQUESPANNAMES, addSingleQuote(currentNamespace),
            addSingleQuote(serviceName));
    String getSpanNamesQueryUrl = String.format(AI_RESTAPI_QUERYURLSTUB, AI_APPLICATIONID,
        getUniqueSpanNamesQuery);
    String response = "";

    waitForRecentWrites();
    try {
      response = runAIQuery(getSpanNamesQueryUrl);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    List<String> spanNames = new ArrayList<String>();
    if (!isNullOrEmpty(response)) {
      JsonArray rows = getAIQueryResult(response);
      //read response for span names
      if (rows != null) {
        for (int j = 0; j < rows.size(); j++) {
          JsonArray currentRow = rows.getJsonArray(j);
          // read first column for spanName
          String spanName = currentRow.getString(0);
          if (spanName != null || !spanName.isEmpty()) {
            spanNames.add(spanName);
          }
        }
      }
    }
    return spanNames;
  }

  private List<Span> readSpansFromJsonArrayOfRows(JsonArray rows, int spanColumnIndex) {
    List<Span> spans = new ArrayList<Span>();
    if (rows != null) {
      for (int j = 0; j < rows.size(); j++) {
        JsonArray currentRow = rows.getJsonArray(j);
        // read specified column index for span
        String spanString = currentRow.getString(spanColumnIndex);

        if (spanString != null || !spanString.isEmpty()) {
          Span span = gson.fromJson(spanString, Span.class);

          if (span != null) {
            spans.add(span);
          }
        }
      }
    }
    return spans;
  }

  private JsonObject getJsonObjectFromString(String jsonStr) {
    Reader r = new StringReader(jsonStr);

    JsonReader reader = Json.createReader(r);
    JsonObject Jobject = reader.readObject();
    reader.close();
    return Jobject;
  }

  private JsonArray getJsonArrayFromString(String jsonStr) {
    Reader r = new StringReader(jsonStr);
    JsonReader reader = Json.createReader(r);
    JsonArray jArray = reader.readArray();
    reader.close();
    return jArray;
  }

  /**
   * AppInsights REST API query response (JSON format) has tables with first table having
   * result data in rows attribute (datatype: JsonArray).
   */
  private JsonArray getAIQueryResult(String response) {
    JsonObject queryResponseInJson = getJsonObjectFromString(response);
    if (queryResponseInJson.containsKey("error")) {
      return null;
    } else {
      JsonArray resultTables = queryResponseInJson.getJsonArray("Tables");
      if (!resultTables.isNull(0)) {
        // read first table for span data - rest of them are metadata
        JsonObject dataTableObj = resultTables.getJsonObject(0);
        return dataTableObj.getJsonArray("Rows");
      }
    }
    return null;
  }

  /**
   * Runs input AI Query URL (REST call) using HTTP Client
   */
  private String runAIQuery(String url) throws IOException {
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder().url(url)
        .addHeader("x-api-key", AI_APIKEY).build();
    try (Response response = client.newCall(request).execute()) {
      return response.body().string();
    }
  }

  private static String addSingleQuote(Object inputString) {
    return "'" + inputString + "'";
  }

  private static String toCommaSeperatedQuotedWords(List<String> words) {
    if (words == null || words.size() == 0) {
      return "''";
    }
    StringBuilder sb = new StringBuilder();
    for (String word : words) {
      sb.append(addSingleQuote(word) + ",");
    }
    sb.deleteCharAt(sb.length() - 1);

    return "[" + sb.toString() + "]";
  }

  private static String toCommaSeperatedKeyValueWords(Map<String, String> keyValuePairs) {
    if (keyValuePairs == null || keyValuePairs.size() == 0) {
      return "''";
    }
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> entry : keyValuePairs.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      // String valueInUTF = new String(value, Charset.forName("UTF-8"));
      ByteBuffer valueInUTF = Charset.forName("UTF-8").encode(value);
      byte[] data = new byte[valueInUTF.array().length];
      valueInUTF.get(data);

      String kv = addSingleQuote(key + Arrays.toString(data).replaceAll("\\s+", ""));
      sb.append(kv + ",");
    }
    sb.deleteCharAt(sb.length() - 1);
    return "[" + sb.toString() + "]";
  }

  private static boolean isNullOrEmpty(String str) {
    if (str == null || str.isEmpty()) {
      return true;
    } else {
      return false;
    }
  }
}
