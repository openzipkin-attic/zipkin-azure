[![Gitter chat](http://img.shields.io/badge/gitter-join%20chat%20%E2%86%92-brightgreen.svg)](https://gitter.im/openzipkin/zipkin)
[![Build Status](https://circleci.com/gh/openzipkin/zipkin-azure.svg?style=svg)](https://circleci.com/gh/openzipkin/zipkin-azure)
[![Download](https://api.bintray.com/packages/openzipkin/maven/zipkin-azure/images/download.svg)](https://bintray.com/openzipkin/maven/zipkin-azure/_latestVersion)

# zipkin-azure
Shared libraries that provide Zipkin integration with Azure services. Requires JRE 6 or later.

# Usage
These components provide Zipkin Senders and Collectors which build off interfaces provided by
the [zipkin-reporter-java](https://github.com/openzipkin/zipkin-reporter-java) and
[zipkin](https://github.com/openzipkin/zipkin) projects.

## Senders

## Collectors
Zipkin collectors receives and decodes span messages from a source. These
spans are later stored.

Collector | Description
--- | --- | ---
[Event Hub](./collector/eventhub) | An alternative to Kafka.

## Server integration
In order to integrate with zipkin-server, you need to use properties
launcher to load your collector (or sender) alongside the zipkin-server
process.

To integrate a module with a Zipkin server, you need to:
* add a module jar to the `loader.path`
* enable the profile associated with that module
* launch Zipkin with `PropertiesLauncher`

Each module will also have different minimum variables that need to be set.

Ex.
```
$ java -Dloader.path=eventhub.jar -Dspring.profiles.active=eventhub -cp zipkin.jar org.springframework.boot.loader.PropertiesLauncher
```

## Example integrating the Azure Event Hub Collector

If you cannot use our [Docker image](https://github.com/openzipkin/docker-zipkin-azure), you can still integrate
yourself by downloading a couple jars. Here's an example of integrating the Azure Event Hub Collector.

For Windows users Powershell is recommended.

### Step 1: Download zipkin-server jar
Download the [latest released server](https://search.maven.org/remote_content?g=io.zipkin.java&a=zipkin-server&v=LATEST&c=exec) as zipkin.jar:

```
cd /tmp
wget -O zipkin.jar 'https://search.maven.org/remote_content?g=io.zipkin.java&a=zipkin-server&v=LATEST&c=exec'
```

### Step 2: Download the latest eventhub-module jar
Download the [latest released Event Hub module](https://search.maven.org/remote_content?g=io.zipkin.azure&a=zipkin-autoconfigure-collector-eventhub&v=LATEST&c=module) as zipkin.jar:

```
cd /tmp
wget -O eventhub.jar 'https://search.maven.org/remote_content?g=io.zipkin.azure&a=zipkin-autoconfigure-collector-eventhub&v=LATEST&c=module'
```

### Step 3: Run the server with the "eventhub" profile active
When you enable the "eventhub" profile, you can configure eventhub with
short environment variables similar to other [Zipkin integrations](https://github.com/openzipkin/zipkin/blob/master/zipkin-server/README.md#elasticsearch-storage).


``` bash
cd /tmp
EVENTHUB_CONNECTION_STRING=Endpoint=sb://< EventHub Address>;SharedAccessKeyName=<name>;SharedAccessKey=<key>
EVENTHUB_STORAGE_CONNECTION_STRING=<connection string>;DefaultEndpointsProtocol=https;AccountName=<yourAccountName>;AccountKey=<yourAccountKey>
java -Dloader.path=eventhub.jar -Dspring.profiles.active=eventhub -cp zipkin.jar org.springframework.boot.loader.PropertiesLauncher
```

Below command for powershell users:

``` bash
cd /tmp
EVENTHUB_CONNECTION_STRING=Endpoint=sb://< EventHub Address>;SharedAccessKeyName=<name>;SharedAccessKey=<key>
EVENTHUB_STORAGE_CONNECTION_STRING=<connection string>;DefaultEndpointsProtocol=https;AccountName=<yourAccountName>;AccountKey=<yourAccountKey>
java '-Dloader.path=eventhub.jar' '-Dspring.profiles.active=eventhub' -cp zipkin.jar org.springframework.boot.loader.PropertiesLauncher
```
