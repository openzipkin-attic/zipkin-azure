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

import com.microsoft.azure.eventprocessorhost.IEventProcessorFactory;
import com.microsoft.azure.eventprocessorhost.PartitionContext;
import org.junit.Test;
import zipkin.storage.InMemoryStorage;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;


public class ZipkinEventProcessorTest {

  static final String dummyEventHubConnectionString = "endpoint=sb://someurl.net;SharedAccessKeyName=dumbo;SharedAccessKey=uius7y8ewychsih";

  @Test
  public void canCreateZipkinEventProcessor() {
    ZipkinEventProcessor zipkinEventProcessor = new ZipkinEventProcessor(getBuilder());
  }

  private EventHubCollector.Builder getBuilder() {
    return EventHubCollector.builder()
        .storage(new InMemoryStorage());
  }

  @Test
  public void canCreateCollecot() {
    EventHubCollector collector = new EventHubCollector(getBuilder(), new IEventProcessorHostFactory() {
      @Override
      public IEventProcessorHost createNew(String hostName, String eventHubPath, String consumerGroupName, String eventHubConnectionString, String storageConnectionString, String storageContainerName, String storageBlobPrefix) {
        return new IEventProcessorHost() {
          @Override
          public Future<?> registerEventProcessorFactory(IEventProcessorFactory<?> factory) throws Exception {
            return null;
          }

          @Override
          public void unregisterEventProcessor() throws InterruptedException, ExecutionException {
          }
        };
      }
    });
  }

  @Test
  public void unregisterGetsCalled_ifStopIsCalledAfterStart() throws IOException {

    DummyEventProcessorHost dummy = new DummyEventProcessorHost();
    EventHubCollector collector = new EventHubCollector(getBuilder(),
        (hostName, eventHubPath, consumerGroupName, eventHubConnectionString, storageConnectionString, storageContainerName, storageBlobPrefix) -> dummy
    );

    collector.start();
    collector.close();

    assertEquals(true, dummy.unrergisterWasCalled);

  }

  @Test
  public void registerGetsCalled_AfterStart() throws IOException {

    DummyEventProcessorHost dummy = new DummyEventProcessorHost();
    EventHubCollector collector = new EventHubCollector(getBuilder(),
      (hostName, eventHubPath, consumerGroupName, eventHubConnectionString, storageConnectionString, storageContainerName, storageBlobPrefix) -> dummy
    );

    collector.start();

    assertEquals(true, dummy.rergisterWasCalled);
  }


}
