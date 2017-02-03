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

### Azure Event Hub Collector
The Zipkin Azure Event Hub Collector is an alternative to Kafka. The [Azure Event Hub](https://azure.microsoft.com/en-us/services/event-hubs/) service is similar to Apache Kafka where data is be pushed to a sink. On the other hand, single consumer reads from a partition have ordering guarantees. These partitions are check pointed on regular intervals.

See **Server Integration** for more info how to run it.

## Server integration
In order to integrate with zipkin-server, you need to use properties launcher to load your collector (or sender) alongside the zipkin-server process.

Here's an example of integrating the Azure Event Hub Collector:

### 1- Clone the source and build
Until the first version is published, you need to build the collector locally.
``` bash
git clone https://github.com/openzipkin/zipkin-azure.git
cd zipkin-azure
./mvnw package
```

### 2- Unpackage MODULE jar into an empty folder
copy zipkin-collector-eventhub-autoconfig-x.x.x-SNAPSHOT-module.jar (that has been package in the `target` folder) into an empty folder and unpackage
``` bash
jar xf zipkin-collector-eventhub-autoconfig-0.1.0-SNAPSHOT-module.jar
```
You may then delete the jar itself.

### 3- Download zipkin-server jar
Download the latest zipkin-server jar (which is named zipkin.jar) from [here](https://search.maven.org/remote_content?g=io.zipkin.java&a=zipkin-server&v=LATEST&c=exec). For more information visit [zipkin-server homepage](https://github.com/openzipkin/zipkin/tree/master/zipkin-server).  

### 4- create an `application.properties` file for configuration next to the zipkin.jar file
Populate the configuration - make sure the resources (Azure Storage, Event Hub, etc) exist. **Only storageConnectionString is mandatory** the rest are optional and must be used only to override the defaults:
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
