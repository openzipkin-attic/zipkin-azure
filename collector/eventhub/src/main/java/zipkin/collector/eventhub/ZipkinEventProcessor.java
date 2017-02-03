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
import zipkin.Codec;
import zipkin.collector.Collector;

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import static zipkin.storage.Callback.NOOP;

public class ZipkinEventProcessor implements IEventProcessor {

  private static final Logger logger = Logger.getLogger(ZipkinEventProcessor.class.getName());

  int checkpointBatchingCount = 0;
  EventHubCollector.Builder config;
  Collector collector;

  public ZipkinEventProcessor(EventHubCollector.Builder builder) {
    config = builder;
    collector = config.delegate.build();
  }

  @Override
  public void onOpen(PartitionContext context) throws Exception {
    logger.log(Level.INFO,"Opened " + context.getConsumerGroupName());
  }

  @Override
  public void onClose(PartitionContext context, CloseReason reason) throws Exception {
    logger.log(Level.INFO,"Closed due to " + reason);
  }

  @Override
  public void onEvents(PartitionContext context, Iterable<EventData> messages) throws Exception {
    for (EventData data : messages) {
      byte[] bytes = data.getBody();
      if (bytes[0] == '[') {
        collector.acceptSpans(bytes, Codec.JSON, NOOP);
      } else {
        if (bytes[0] == 12 /* TType.STRUCT */) {
          collector.acceptSpans(bytes, Codec.THRIFT, NOOP);
        } else {
          collector.acceptSpans(Collections.singletonList(bytes), Codec.THRIFT, NOOP);
        }
      }

      this.checkpointBatchingCount++;
      if ((checkpointBatchingCount % config.checkpointBatchSize) == 0) {
        logger.log(Level.INFO,"Partition " + context.getPartitionId() + " checkpointing at " +
            data.getSystemProperties().getOffset() + "," + data.getSystemProperties().getSequenceNumber());
        context.checkpoint(data);
      }
    }
  }

  @Override
  public void onError(PartitionContext context, Throwable error) {
    error.printStackTrace();
  }
}
