[![Gitter chat](http://img.shields.io/badge/gitter-join%20chat%20%E2%86%92-brightgreen.svg)](https://gitter.im/openzipkin/zipkin)
[![Build Status](https://circleci.com/gh/openzipkin/zipkin-azure.svg?style=svg)](https://circleci.com/gh/openzipkin/zipkin-azure)
[![Download](https://api.bintray.com/packages/openzipkin/maven/zipkin-azure/images/download.svg)](https://bintray.com/openzipkin/maven/zipkin-azure/_latestVersion)

# zipkin-azure
Shared libraries that provide Zipkin integration with Azure services. Requires JRE 6 or later.

# Usage
These components provide Zipkin Senders and Collectors which build off interfaces provided by
the [zipkin-reporters-java](https://github.com/openzipkin/zipkin-reporter-java) and
[zipkin](https://github.com/openzipkin/zipkin) projects.

## Senders

## Collectors

### EventHub Collector
EventHub is a Zipkin collecor that can collect spans from Azure EventHub, instead of Kafka. [Azure EventHub](https://azure.microsoft.com/en-us/services/event-hubs/) is an Azure PaaS Service has a functionality (and design) very similar to Apache Kafka where on one hand, the data is be pushed to a sink. On the other hand, the data is read by the consumers from partitioned storage where only a single consumer reads from a partition guaranteeing ordering of the messages. On regular intervals, partition gets checkpointed.

See **Server Integration** for more info how to run it.

## Server integration
In order to integrate with zipkin-server, you need to use properties launcher to load your collector (or sender) alongside the zipkin-server process.

This stepwise guide, documents how to do this for EventHub Collector.

### 1- Clone the source and build
``` bash
mkdir zipkin-collector-eventhub
cd zipkin-collector-eventhub
git clone git@github.com:openzipkin/zipkin-azure.git
mvn package
```
If you do not have maven, get maven [here](http://maven.apache.org/install.html).

### 2- Unpackage MODULE jar into an empty folder
copy zipkin-collector-eventhub-autoconfig-x.x.x-SNAPSHOT-module.jar (that has been package in the `target` folder) into an empty folder and unpackage
``` bash
jar xf zipkin-collector-eventhub-autoconfig-0.1.0-SNAPSHOT-module.jar
```
You may then delete the jar itself.

### 3- Download zipkin-server jar
Download the latest zipkin-server jar (which is named zipkin.jar) from [here](https://search.maven.org/remote_content?g=io.zipkin.java&a=zipkin-server&v=LATEST&c=exec). For more information visit [zipkin-server homepage](https://github.com/openzipkin/zipkin/tree/master/zipkin-server).  

### 4- create an `application.properties` file for configuration next to the zipkin.jar file
Populate the configuration - make sure the resources (Azure Storage, EventHub, etc) exist. **Only storageConnectionString is mandatory** the rest are optional and must be used only to override the defaults:
```
zipkin.collector.eventhub.storageConnectionString=<azure storage connection string>
zipkin.collector.eventhub.eventHubName=<name of the eventhub, default is zipkin>
zipkin.collector.eventhub.consumerGroupName=<name of the consumer group, default is $Default>
zipkin.collector.eventhub.storageContainerName=<name of the storage container, default is zipkin>
zipkin.collector.eventhub.processorHostName=<name of the processor host, default is a randomly generated GUID>
zipkin.collector.eventhub.storageBlobPrefix=<the path within container where blobs are created for partition lease, processorHostName>
```

### 5- Run the server along with the collector
Assuming `zipkin.jar` and `application.properties` are in the current working directory. Note that the EventHub connection string gets passed as a command-line parameter, not from the `application.properties` file:
```
java -Dloader.path=/where/jar/was/unpackaged -cp zipkin.jar org.springframework.boot.loader.PropertiesLauncher --spring.config.location=application.properties --zipkin.collector.eventhub.eventHubConnectionString="<eventhub connection string, make sure quoted otherwise won't work>"
```
