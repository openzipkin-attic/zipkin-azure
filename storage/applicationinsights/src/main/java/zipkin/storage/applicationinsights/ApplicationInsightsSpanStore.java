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

import java.util.*;
import zipkin.DependencyLink;
import zipkin.internal.Nullable;
import zipkin.internal.MergeById;
import zipkin.internal.CorrectForClockSkew;
import zipkin.internal.DependencyLinker;
import zipkin.Span;
import zipkin.storage.QueryRequest;
import zipkin.storage.SpanStore;

final class ApplicationInsightsSpanStore implements SpanStore {

  private ApplicationInsightsClient appInsightsClient;
  private String applicationId;
  private String apiKey;
  private String namespace;
  private boolean strictTraceId = true;
  private int waitTimeInSeconds;

  public ApplicationInsightsSpanStore(String appId, String apiKey) {
    this.applicationId = appId;
    this.apiKey = apiKey;
    this.appInsightsClient = new ApplicationInsightsClient(appId, apiKey);
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
    this.appInsightsClient.setNamespace(namespace);
  }

  public void setStrictTraceId(boolean isStrict) {
    this.strictTraceId = isStrict;
    this.appInsightsClient.setStrictTraceId(isStrict);
  }

  public void setWaitTimeInSeconds(int timeInSeconds) {
    this.waitTimeInSeconds = timeInSeconds;
    this.appInsightsClient.setWaitTimeInSeconds(timeInSeconds);
  }

  @Override
  public List<String> getServiceNames() {
    return appInsightsClient.getServiceNames();
  }

  @Override
  public List<List<Span>> getTraces(QueryRequest request) {
    return appInsightsClient.getTraces(request);
  }

  /**
   * Get the available trace information from the storage system. Spans in trace are sorted by the
   * first annotation timestamp in that span. First event should be first in the spans list.
   *
   * @return a list of spans with the same {@link Span#traceId}, or null if not present.
   */
  //deprecated
  @Override
  public List<Span> getTrace(long traceId) {
    // return appInsightsClient.getTrace(String.valueOf(id));
    return getTrace(0L, traceId);
  }

  @Override
  public List<Span> getTrace(long traceIdHigh, long traceIdLow) {
    List<Span> result = getRawTrace(traceIdHigh, traceIdLow);
    if (result == null) {
      return null;
    } else {
      return CorrectForClockSkew.apply(MergeById.apply(result));
    }
  }

  /**
   * Retrieves spans that share a trace id, as returned from backend data store queries, with no
   * ordering expectation.
   *
   * <p>This is different, but related to {@link #getTrace}. {@link #getTrace} cleans data by
   * merging spans, adding timestamps and performing clock skew adjustment. This feature is for
   * debugging zipkin logic or zipkin instrumentation.
   *
   * @return a list of spans with the same {@link Span#traceId}, or null if not present.
   */
  //deprecated
  @Override
  public List<Span> getRawTrace(long traceId) {
    return getRawTrace(0L, traceId);
  }

  @Override
  public List<Span> getRawTrace(long traceIdHigh, long traceIdLow) {
    List<Span> spans =
        appInsightsClient.getTrace(String.valueOf(traceIdHigh), String.valueOf(traceIdLow));
    if (spans == null || spans.size() == 0) {
      return null;
    } else {
      return spans;
    }
  }

  /**
   * returns all unique span names currently in the system
   */
  @Override
  public List<String> getSpanNames(String serviceName) {
    return appInsightsClient.getSpanNames(serviceName);
  }

  @Override
  public List<DependencyLink> getDependencies(long endTs, @Nullable Long lookback) {
    QueryRequest request = QueryRequest.builder()
        .endTs(endTs)
        .lookback(lookback)
        .limit(Integer.MAX_VALUE).build();

    DependencyLinker linksBuilder = new DependencyLinker();
    for (Collection<Span> trace : getTraces(request)) {
      linksBuilder.putTrace(trace);
    }
    return linksBuilder.link();
  }
}