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
Zipkin collectors receives and decodes span messages from a source. These
spans are later stored.

Collector | Description | Readme
--- | --- | ---
[Event Hub](./collector/eventhub) | An alternative to Kafka. | [eventhub-collector](/collector/eventhub/README.md)
[Application Insights](./collector/applicationinsights) | Integrates Application Insights data model with Zipkin concepts. | [applicationinsights-storage](/storage/applicationinsights/README.md)

## Server integration
In order to integrate with zipkin-server, you need to use properties
launcher to load your collector (or sender) alongside the zipkin-server
process.

To integrate a module with a Zipkin server, you need to:
* add an extracted module directory to the `loader.path`
* enable the profile associated with that module
* launch Zipkin with `PropertiesLauncher`

Each module will also have different minimum variables that need to be set.

Ex.
```
$ java -Dloader.path=eventhub -Dspring.profiles.active=eventhub -cp zipkin.jar org.springframework.boot.loader.PropertiesLauncher
```

TODO: Until distributions are published, we can't write instructions to
simply download modules. Until then, users will have to build locally.

## Building locally

Here's an example of building and integrating the Azure Event Hub Collector. For Windows users PowerShell is recommended.

### Step 1: Download zipkin-server jar
Download the [latest released server](https://search.maven.org/remote_content?g=io.zipkin.java&a=zipkin-server&v=LATEST&c=exec) as zipkin.jar:

```
cd /tmp
wget -O zipkin.jar 'https://search.maven.org/remote_content?g=io.zipkin.java&a=zipkin-server&v=LATEST&c=exec'
```

### Step 2: Build the Event Hub module
Until the first version is published, you need to build the collector locally.
``` bash
git clone https://github.com/openzipkin/zipkin-azure.git
(cd zipkin-azure && ./mvnw -DskipTests package)
```

This should result in a file like:
`zipkin-azure/autoconfigure/collector-eventhub/target/zipkin-autoconfigure-collector-eventhub-x.x.x-SNAPSHOT-module.jar`

### Step 3: Extract the Event Hub module into a subdirectory
The Event Hub module should be in a different directory than where you put zipkin.jar.
It is easiest to create a directory named "eventhub" relative to zipkin.jar

``` bash
cd /tmp
mkdir eventhub
(cd eventhub && jar -xf zipkin-collector-eventhub-autoconfig-x.x.x-SNAPSHOT-module.jar)
```

### Step 4: Run the server with the "eventhub" profile active
When you enable the "eventhub" profile, you can configure eventhub with
short environment variables similar to other [Zipkin integrations](https://github.com/openzipkin/zipkin/blob/master/zipkin-server/README.md#elasticsearch-storage).


``` bash
cd /tmp
EVENTHUB_CONNECTION_STRING=Endpoint=sb://< EventHub Address>;SharedAccessKeyName=<name>;SharedAccessKey=<key> \
EVENTHUB_STORAGE_CONNECTION_STRING=<connection string>;DefaultEndpointsProtocol=https;AccountName=<yourAccountName>;AccountKey=<yourAccountKey> \
java -Dloader.path=eventhub -Dspring.profiles.active=eventhub -cp zipkin.jar org.springframework.boot.loader.PropertiesLauncher
```
** NOTE: Make sure the parameters are defined in the same line or use environment variables **


Below command for PowerShell users:

``` PowerShell
cd /tmp
$env:EVENTHUB_CONNECTION_STRING='Endpoint=sb://< EventHub Address>;SharedAccessKeyName=<name>;SharedAccessKey=<key>'
$env:EVENTHUB_STORAGE_CONNECTION_STRING='<connection string>;DefaultEndpointsProtocol=https;AccountName=<yourAccountName>;AccountKey=<yourAccountKey>'
java '-Dloader.path=eventhub' '-Dspring.profiles.active=eventhub' -cp zipkin.jar org.springframework.boot.loader.PropertiesLauncher
```
