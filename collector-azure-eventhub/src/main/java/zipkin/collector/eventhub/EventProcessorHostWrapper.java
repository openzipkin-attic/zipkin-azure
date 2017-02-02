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

import com.microsoft.azure.eventprocessorhost.EventProcessorHost;
import com.microsoft.azure.eventprocessorhost.IEventProcessorFactory;

import java.util.concurrent.Future;

public class EventProcessorHostWrapper implements IEventProcessorHost {
  private EventProcessorHost host;

  @Override
  public Future<?> registerEventProcessorFactory(IEventProcessorFactory<?> factory) throws Exception {
    if(host==null)
      throw new Exception("Please call the setup first!");
    return host.registerEventProcessorFactory(factory);
  }

  @Override
  public void setup(String hostName,
                    String eventHubPath,
                    String consumerGroupName,
                    String eventHubConnectionString,
                    String storageConnectionString,
                    String storageContainerName,
                    String storageBlobPrefix) {

    host = new EventProcessorHost(hostName,
        eventHubPath,
        consumerGroupName,
        eventHubConnectionString,
        storageConnectionString,
        storageContainerName,
        storageBlobPrefix);
  }
}
