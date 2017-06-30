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

import java.util.concurrent.Executor;
import zipkin.internal.Nullable;

import java.util.logging.Logger;
import zipkin.storage.AsyncSpanConsumer;
import zipkin.storage.AsyncSpanStore;
import zipkin.storage.SpanStore;
import zipkin.storage.StorageComponent;

import static zipkin.internal.Util.checkNotNull;
import static zipkin.storage.StorageAdapters.blockingToAsync;

public final class ApplicationInsightsStorage implements StorageComponent {

  static final Logger LOG = Logger.getLogger(ApplicationInsightsStorage.class.getName());

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder implements StorageComponent.Builder {
    boolean strictTraceId = true;
    private String instrumentationKey;
    private String applicationId;
    private String apiKey;
    private String namespace;
    private Executor executor;
    private int readWaitTimeInSeconds;

    public Builder executor(Executor executor) {
      this.executor = checkNotNull(executor, "executor");
      return this;
    }

    @Override public Builder strictTraceId(boolean strictTraceId) {
      this.strictTraceId = strictTraceId;
      return this;
    }

    public Builder instrumentationKey(@Nullable String instrumentationKey) {
      this.instrumentationKey = checkNotNull(instrumentationKey, "instrumentationKey");
      return this;
    }

    public Builder applicationId(@Nullable String applicationId) {
      this.applicationId = checkNotNull(applicationId, "applicationId");
      return this;
    }

    public Builder apikey(@Nullable String apiKey) {
      this.apiKey = checkNotNull(apiKey, "apiKey");
      return this;
    }

    public Builder namespace(@Nullable String namespace) {
      this.namespace = namespace;
      return this;
    }

    public Builder readWaitTimeInSeconds(@Nullable int readWaitTimeInSeconds) {
      this.readWaitTimeInSeconds = readWaitTimeInSeconds;
      return this;
    }

    @Override public ApplicationInsightsStorage build() {
      return new ApplicationInsightsStorage(this);
    }

    Builder() {
    }
  }

  final boolean strictTraceId;
  private final String namespace;
  private final String instrumentationKey;
  private final String applicationId;
  private final String apiKey;
  private final Executor executor;
  private int readWaitTimeInSeconds;

  ApplicationInsightsStorage(Builder builder) {
    this.strictTraceId = builder.strictTraceId;
    this.instrumentationKey = builder.instrumentationKey;
    this.applicationId = builder.applicationId;
    this.apiKey = builder.apiKey;
    this.namespace = builder.namespace;
    this.readWaitTimeInSeconds = builder.readWaitTimeInSeconds;
    this.executor = checkNotNull(builder.executor, "executor");
    LOG.info("ApplicationInsightsStorage initialized: " + instrumentationKey + " " + applicationId + " " + apiKey);
  }

  @Override
  public SpanStore spanStore() {
    ApplicationInsightsSpanStore aiSpanStore =
        new ApplicationInsightsSpanStore(this.applicationId, this.apiKey);
    aiSpanStore.setStrictTraceId(this.strictTraceId);
    aiSpanStore.setNamespace(this.namespace);
    aiSpanStore.setWaitTimeInSeconds(this.readWaitTimeInSeconds);

    return aiSpanStore;
  }

  @Override
  public AsyncSpanStore asyncSpanStore() {
    return blockingToAsync(spanStore(), executor);
  }

  @Override
  public AsyncSpanConsumer asyncSpanConsumer() {
    ApplicationInsightsSpanConsumer aiSpanConsumer =
        new ApplicationInsightsSpanConsumer(this.instrumentationKey);
    aiSpanConsumer.setNamespace(this.namespace);

    return blockingToAsync(aiSpanConsumer, executor);
  }

  @Override
  public void close() {
    // didn't open the DataSource or executor
  }

  @Override
  public CheckResult check() {
    return null;
  }
}
