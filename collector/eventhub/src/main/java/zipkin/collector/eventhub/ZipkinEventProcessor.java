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
package zipkin.collector.eventhub;

import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventprocessorhost.CloseReason;
import com.microsoft.azure.eventprocessorhost.IEventProcessor;
import com.microsoft.azure.eventprocessorhost.PartitionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import zipkin.Codec;
import zipkin.Span;
import zipkin.collector.Collector;

import static zipkin.storage.Callback.NOOP;

final class ZipkinEventProcessor implements IEventProcessor {
  final Logger logger;
  final Collector collector;
  final int checkpointBatchSize;

  // We assume callbacks can come from different threads, so access to this variable are guarded.
  // Undo concurrency and related code if https://github.com/Azure/azure-event-hubs-java/issues/52
  // deems it unnecessary
  int countSinceCheckpoint = 0; // guarded by this

  ZipkinEventProcessor(Collector collector, int checkpointBatchSize) {
    this(Logger.getLogger(ZipkinEventProcessor.class.getName()), collector, checkpointBatchSize);
  }

  ZipkinEventProcessor(Logger logger, Collector collector, int checkpointBatchSize) {
    this.logger = logger;
    this.collector = collector;
    this.checkpointBatchSize = checkpointBatchSize;
  }

  @Override
  public void onOpen(PartitionContext context) {
    logger.log(Level.FINE, "Opened " + context.getConsumerGroupName());
  }

  @Override
  public void onClose(PartitionContext context, CloseReason reason) {
    logger.log(Level.FINE, "Closed due to " + reason);
  }

  @Override
  public void onEvents(PartitionContext context, Iterable<EventData> messages)
      throws ExecutionException, InterruptedException {
    // don't issue a write until we checkpoint or exit this callback
    List<Span> buffer = new ArrayList<>();

    for (EventData data : messages) {
      byte[] bytes = data.getBody();
      List<Span> nextSpans = bytes[0] == '['
          ? Codec.JSON.readSpans(bytes)
          : Codec.THRIFT.readSpans(bytes);
      buffer.addAll(nextSpans);

      if (maybeCheckpoint(context, data, nextSpans.size())) {
        collector.accept(buffer, NOOP);
        buffer.clear();
      }
    }

    if (!buffer.isEmpty()) {
      collector.accept(buffer, NOOP);
    }
  }

  /**
   * If what we've read puts us at or over our count since last checkpoint, checkpoint and return
   * true.
   */
  boolean maybeCheckpoint(PartitionContext context, EventData data, int spansRead)
      throws InterruptedException, ExecutionException {
    if (!shouldCheckPoint(spansRead)) return false;

    if (logger.isLoggable(Level.FINE)) {
      logger.log(Level.FINE, "Partition " + context.getPartitionId() + " checkpointing at " +
          data.getSystemProperties().getOffset() + "," + data.getSystemProperties()
          .getSequenceNumber());
    }
    context.checkpoint(data);
    return true;
  }

  boolean shouldCheckPoint(int spansRead) {
    synchronized (this) {
      countSinceCheckpoint += spansRead;
      if (countSinceCheckpoint >= checkpointBatchSize) {
        countSinceCheckpoint = 0;
        return true;
      }
    }
    return false;
  }

  @Override
  public void onError(PartitionContext context, Throwable error) {
    logger.log(Level.WARNING, "Error in " + context.getConsumerGroupName(), error);
  }
}
