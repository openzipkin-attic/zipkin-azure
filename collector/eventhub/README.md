# collector-eventhub

## EventHubCollector
The Zipkin Azure Event Hub Collector is an alternative to Kafka.
The [Azure Event Hub](https://azure.microsoft.com/en-us/services/event-hubs/)
service is similar to Apache Kafka where data is be pushed to a sink. On
the other hand, single consumer reads from a partition have ordering guarantees.
These partitions are check pointed on regular intervals.

## Server Configuration
The most common configuration is integrating with the Zipkin server.
Event Hub Collector is enabled when..
* the eventhub module is in the server's loader.path
* the "eventhub" profile is activated
* EVENTHUB_CONNECTION_STRING is set

Ex. Assuming the eventhub module artifact is extracted into a directory called eventhub..
TODO: realistic connection strings!
```bash
EVENTHUB_CONNECTION_STRING=foo
EVENTHUB_STORAGE_CONNECTION_STRING=bar
java -Dloader.path=eventhub -Dspring.profiles.active=eventhub -cp zipkin.jar org.springframework.boot.loader.PropertiesLauncher
```

TODO: put a maven central download link once published
TODO: put a link to a docker image that has the module installed and activated.
TODO: add descriptions to Event.Builder setters!
TODO: each description should note impact, like why you'd set this

The following variables configure the server:

Variable | Default | Description
--- | ---
`EVENTHUB_CONNECTION_STRING` | None | Azure EventHub ConnectionString
`EVENTHUB_STORAGE_CONNECTION_STRING` | None | Azure Storage ConnectionString
`EVENTHUB_NAME` | zipkin | TODO: link to azure docs
`EVENTHUB_CHECKPOINT_BATCH_SIZE` | 10 | The number of messages consumed from a partition after which checkpointing occurs.
`EVENTHUB_CONSUMER_GROUP` | "$Default" | Consumer Group for your EventHub
`EVENTHUB_PROCESSOR_HOST` | random GUID | Name of the processor host - for information purposes only.
`EVENTHUB_STORAGE_CONTAINER` | "zipkin" | Indicates the container in which partition offsets are stored and used for the partition lease.
`EVENTHUB_STORAGE_BLOB_PREFIX` | "zipkin_checkpoint_store" | The path within the storage container where the offsets get stored.

## Alternate Configuration
`EventHubCollector` can also be used as a library, where attributes are
set via `EventHubCollector.Builder`.

You can also use this component directly in Spring Boot applications, by
depending on [io.zipkin.azure:zipkin-autoconfigure-collector-eventhub](../../autoconfigure/zipkin-autoconfigure-collector-eventhub)
In this case, properties prefixed with "zipkin.collector.eventhub" configure the integration.
