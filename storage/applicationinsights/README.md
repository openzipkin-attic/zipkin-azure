# storage-applicationinsights

## Application Insights Storage
[Application Insights](https://docs.microsoft.com/azure/application-insights/) is a APM vendor providing similar data
model for distributed transactions monitoring. This module allows to store Zipkin traces in Application Insights
natively so Application Insights monitoring features like analytics query or alerting could be used for these traces.
It also allows to view data collected by Application Insights natively in Zipkin UI.

## Server Configuration
The most common configuration is integrating with the Zipkin server. You need to provide three additional configuration
parameters. One to collect the data and two to query the data. Event Hub Collector is enabled when:
* the applicationinsights module is in the server's loader.path
* the `applicationinsights` profile is activated
* `AI_INSTRUMENTATION_KEY` is set to collect data
* `AI_APPLICATION_ID` is set alongside with
* `AI_API_KEY` to collect the data

Ex. Assuming the applicationinsights module artifact is extracted into a directory called applicationinsights..

```bash
AI_INSTRUMENTATION_KEY=c953a825-e184-4f31-b275-4f0199c36586\
AI_APPLICATION_ID=037f09c1-a96b-475f-b35e-eec772214619 \
AI_API_KEY=tp63dp4le4dgrmcb0cc2ecuqj5he1mvrgahfyeyn\
java -Dloader.path=applicationinsights.jar -Dspring.profiles.active=applicationinsights -cp zipkin.jar org.springframework.boot.loader.PropertiesLauncher
```

** NOTE: Make sure the parameters are defined in the same line or use environment variables **

TODO: put a maven central download link once published
TODO: put a link to a docker image that has the module installed and activated.
TODO: add descriptions to Event.Builder setters!
TODO: each description should note impact, like why you'd set this

## Alternate Configuration
`ApplicationInsightsStorage` can also be used as a library, where attributes are
set via `ApplicationInsightsStorage.Builder`.

You can also use this component directly in Spring Boot applications, by
depending on [io.zipkin.azure:zipkin-autoconfigure-storage-applicationinsights](../../autoconfigure/zipkin-autoconfigure-storage-applicationinsights)
In this case, properties prefixed with `zipkin.storage.applicationinsights` configure the integration.
