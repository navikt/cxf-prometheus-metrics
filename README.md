CXF Prometheus Metrics [![Maven Central](https://maven-badges.herokuapp.com/maven-central/no.nav.helse/cxf-prometheus-metrics/badge.svg)](https://maven-badges.herokuapp.com/maven-central/no.nav.helse/cxf-prometheus-metrics)
======================

En `WebServiceFeature` som tar tiden på utgående kall, og teller antall vellykkede og feilende kall. Det skilles mellom faults
som tjenesten svarer med (`error`) og andre feil som f.eks. connection timeout og den slags (`failure`). 
Alle kall blir tatt tiden på, ikke kun vellykkede kall.

Det opprettes totalt to metrikker:

```
webservice_calls_latency{service="", operation=""}
webservice_calls_total{service="", operation="", status="success|error|failure"}
```

`service` er navnet på tjenesten som kalles og `operation` er navnet på operasjonen, f.eks.:

```
service=Person_V3
operation=ping
```

`webservice_calls_latency` er et histogram, og Prometheus vil derfor opprette tre metrikker på denne:

```
webservice_calls_latency_bucket
webservice_calls_latency_count
webservice_calls_latency_sum
```

Se https://prometheus.io/docs/practices/histograms/ for mer informasjon.
Det er verd å merke seg at her brukes Prometheus direkte. Om man bruker Micrometer (“slf4j for metrikker”) må man passe på at _CollectorRegistry_ i Prometheus registreres inn i Micrometers _MeterRegistry_. For Spring Boot holder det å eksponere Prometheus sitt default registry som en _@Bean_ så sørger Boot for selve registreringen. For andre rammeverk må dette gjøres manuelt

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #område-helse.
