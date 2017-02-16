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
import com.microsoft.azure.eventprocessorhost.EventProcessorHost;
import com.microsoft.azure.eventprocessorhost.PartitionContext;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;

// Azure classes are in a signed jar, so we hack a class and put it in the same protection domain.
//
// See http://stackoverflow.com/questions/42184038/strategy-for-intercepting-signed-classes
public enum FakePartitionContext {
  INSTANCE;
  final ConcurrentLinkedQueue<EventData> checkpointEvents;
  private final PartitionContext hacked;

  FakePartitionContext() {
    try {
      String partitionId = "1";
      checkpointEvents = new ConcurrentLinkedQueue<>();
      Consumer<EventData> addEvent = checkpointEvents::add;
      Class<?> hackedSubclass = new ByteBuddy()
          .subclass(PartitionContext.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
          .name("com.microsoft.azure.eventprocessorhost.HackedPartitionContext")
          .defineConstructor(Visibility.PUBLIC)
          .withParameters(new Type[0])
          // TODO: make parameter(0) of type String and pass it to super as parameter(1)
          .intercept(MethodCall.invoke(PartitionContext.class.getDeclaredConstructor(
              EventProcessorHost.class, String.class, String.class, String.class))
              .with(new Object[] {null, partitionId, null, null}))
          .defineMethod("checkpoint", void.class)
          .withParameter(EventData.class)
          // TODO: add a ctor parameter of Consumer<EventData>, and save as a field. delegate the
          // method call here to that field::accept
          .intercept(MethodDelegation.to(addEvent, Consumer.class))
          .make()
          .load(PartitionContext.class.getClassLoader(),
              ClassLoadingStrategy.Default.INJECTION.with(
                  PartitionContext.class.getProtectionDomain()))
          .getLoaded();
      hacked = ((PartitionContext) hackedSubclass.getDeclaredConstructor().newInstance());
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  PartitionContext get() {
    return hacked;
  }
}