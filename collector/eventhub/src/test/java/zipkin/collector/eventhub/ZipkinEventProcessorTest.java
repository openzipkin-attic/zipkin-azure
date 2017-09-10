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
import com.microsoft.azure.eventprocessorhost.PartitionContext;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Test;
import zipkin.Codec;
import zipkin.TestObjects;
import zipkin.collector.Collector;
import zipkin.internal.V2SpanConverter;
import zipkin.internal.v2.Span;
import zipkin.internal.v2.codec.SpanBytesEncoder;
import zipkin.storage.InMemoryStorage;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class ZipkinEventProcessorTest {

  InMemoryStorage storage = new InMemoryStorage();
  Collector collector = Collector.builder(EventHubCollector.class).storage(storage).build();
  ConcurrentLinkedQueue<EventData> checkpointEvents = new ConcurrentLinkedQueue<>();
  TestLogger logger = new TestLogger();

  // we are using mock only to create an instance. we can't mock methods as some of our tests
  // are multithreaded.
  PartitionContext context = mock(PartitionContext.class);

  ZipkinEventProcessor processor = new ZipkinEventProcessor(logger, collector, 10) {
    @Override String partitionId(PartitionContext context) {
      assertThat(context).isSameAs(ZipkinEventProcessorTest.this.context);
      return "1";
    }

    @Override void checkpoint(PartitionContext context, EventData data)
        throws ExecutionException, InterruptedException {
      assertThat(context).isSameAs(ZipkinEventProcessorTest.this.context);
      checkpointEvents.add(data);
    }
  };

  // mocked invocations aren't thread-safe. let's test we aren't using them
  @After public void verifyNoCallsToContext() {
    verifyZeroInteractions(context);
  }

  @Test
  public void onEvents_singleJsonEvent() throws Exception {
    onEvents_singleDatum(Codec.JSON);
  }

  @Test
  public void onEvents_singleThriftEvent() throws Exception {
    onEvents_singleDatum(Codec.THRIFT);
  }

  void onEvents_singleDatum(Codec codec) throws ExecutionException, InterruptedException {
    List<EventData> messages = asList(new EventData(codec.writeSpans(TestObjects.TRACE)));
    processor.onEvents(context, messages);

    assertThat(storage.spanStore().getRawTraces()).hasSize(1);
    assertThat(storage.spanStore().getRawTraces().get(0)).isEqualTo(TestObjects.TRACE);
  }

  @Test
  public void checkpointsOnBatchSize() throws Exception {
    EventData event1 = jsonMessageWithThreeSpans("a", 1);
    EventData event2 = json2MessageWithThreeSpans("b", 2);
    EventData event3 = thriftMessageWithThreeSpans("c", 3);
    EventData event4 = json2MessageWithThreeSpans("d", 4);

    // We don't expect a checkpoint, yet
    processor.onEvents(context, asList(event1, event2, event3));
    assertThat(checkpointEvents)
        .isEmpty();
    assertThat(logger.messages.poll())
        .isNull();

    // We expect a checkpoint as we completed a batch
    processor.onEvents(context, asList(event4));
    assertThat(checkpointEvents)
        .containsExactly(event4);
    assertThat(logger.messages.poll())
        .isEqualTo("FINE: Partition 1 checkpointing at d,4");
  }

  /** This shows that checkpointing is consistent when callbacks are on different threads. */
  @Test
  public void parallelCheckpoint() throws Exception {
    int spansPerEvent = 3;

    // We checkpoint at or over the checkpoint batch size. By default, our batch size is
    // 10, so if we have 3 spans per event, we checkpoint on the 3rd event (span count 12 not 10).
    int eventsPerCheckpoint = processor.checkpointBatchSize / spansPerEvent;
    if (processor.checkpointBatchSize % spansPerEvent > 0) eventsPerCheckpoint++;

    // make a lot of events to ensure concurrency works.
    int eventCount = 1000;
    final ConcurrentLinkedQueue<EventData> events = new ConcurrentLinkedQueue<>();
    for (int i = 0; i < eventCount; i++) {
      events.add(jsonMessageWithThreeSpans(Integer.toHexString(i + 1), 1 + i));
    }

    // We currently don't know if onEvents is always called from the same thread or not.
    //
    // To test logic is consistent, we fire up 10 threads who will pull events of the queue and
    // invoke onEvents with that event. This will happen concurrently and out-of-order.
    // If we don't end up with an exact number of checkpoints, we might have a concurrency bug.
    CountDownLatch latch = new CountDownLatch(events.size());
    int threadCount = 10;
    ExecutorService exec = Executors.newFixedThreadPool(threadCount);
    for (int i = 0; i < threadCount; i++) {
      exec.execute(() -> {
        EventData event;
        while ((event = events.poll()) != null) {
          try {
            processor.onEvents(context, asList(event));
          } catch (Exception e) {
            e.printStackTrace();
          }
          latch.countDown();
        }
      });
    }

    //latch.await();

    exec.shutdown();
    exec.awaitTermination(1, TimeUnit.SECONDS);

    assertThat(processor.countSinceCheckpoint)
        .isZero();
    assertThat(checkpointEvents)
        .hasSize(eventCount / eventsPerCheckpoint);
  }

  static EventData thriftMessageWithThreeSpans(String offset, long sequenceNumber) throws Exception {
    return message(offset, sequenceNumber, Codec.THRIFT.writeSpans(TestObjects.TRACE));
  }

  static EventData jsonMessageWithThreeSpans(String offset, long sequenceNumber) throws Exception {
    return message(offset, sequenceNumber, Codec.JSON.writeSpans(TestObjects.TRACE));
  }

  static EventData json2MessageWithThreeSpans(String offset, long sequenceNumber) throws Exception {
    List<Span> spans = IntStream.range(0, 3).mapToObj(i -> TestObjects.LOTS_OF_SPANS[i])
        .flatMap(s -> V2SpanConverter.fromSpan(s).stream())
        .collect(Collectors.toList());
    return message(offset, sequenceNumber, SpanBytesEncoder.JSON_V2.encodeList(spans));
  }

  static EventData message(String offset, long sequenceNumber, byte[] message)
      throws IllegalAccessException, NoSuchFieldException {
    EventData data = new EventData(message);
    LinkedHashMap<String, Object> sysProps = new LinkedHashMap<>();
    sysProps.put("x-opt-offset", offset);
    sysProps.put("x-opt-sequence-number", sequenceNumber);
    addSystemProperties(data, sysProps);
    return data;
  }

  static class TestLogger extends Logger {
    ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue<>();

    TestLogger() {
      super("", null);
      setLevel(Level.FINE);
    }

    @Override public void log(Level level, String msg) {
      messages.add(level + ": " + msg);
    }
  }

  static void addSystemProperties(EventData data, LinkedHashMap<String, Object> sysProps)
      throws IllegalAccessException, NoSuchFieldException {
    Field field = EventData.class.getDeclaredField("systemProperties");
    field.setAccessible(true);
    field.set(data, new EventData.SystemProperties(sysProps));
  }
}
