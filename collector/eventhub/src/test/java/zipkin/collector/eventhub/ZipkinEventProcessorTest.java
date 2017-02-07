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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import zipkin.Codec;
import zipkin.TestObjects;
import zipkin.collector.Collector;
import zipkin.storage.InMemoryStorage;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ZipkinEventProcessorTest {

  @Rule
  public MockitoRule mocks = MockitoJUnit.rule();

  InMemoryStorage storage = new InMemoryStorage();
  Collector collector = Collector.builder(EventHubCollector.class).storage(storage).build();

  TestLogger logger = new TestLogger();
  ZipkinEventProcessor processor = new ZipkinEventProcessor(logger, collector, 10);

  @Mock PartitionContext context;

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
    processor = new ZipkinEventProcessor(logger, collector, TestObjects.TRACE.size() - 1);

    EventData data = new EventData(Codec.JSON.writeSpans(TestObjects.TRACE));
    LinkedHashMap<String, Object> sysProps = new LinkedHashMap<>();
    sysProps.put("x-opt-offset", "abcde");
    sysProps.put("x-opt-sequence-number", 1L);
    addSystemProperties(data, sysProps);
    when(context.getPartitionId()).thenReturn("0");

    processor.onEvents(context, asList(data));

    verify(context).checkpoint(data);
    assertThat(logger.messages)
        .containsExactly("FINE: Partition 0 checkpointing at abcde,1");
  }

  static class TestLogger extends Logger {
    List<String> messages = new ArrayList<>();

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
