package no.nav.cxf.metrics

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Fault
import io.prometheus.client.CollectorRegistry
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import javax.xml.namespace.QName
import javax.xml.ws.soap.SOAPFaultException

class WebserviceTest {

    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())

        @BeforeAll
        @JvmStatic
        fun start() {
            server.start()
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            server.stop()
        }
    }

    @BeforeEach
    fun configure() {
        WireMock.configureFor(server.port())
    }

    private fun createPort(): PersonV3 {
        val factory = JaxWsProxyFactoryBean().apply {
            address = server.baseUrl()
            wsdlURL = "wsdl/no/nav/tjeneste/virksomhet/person/v3/Binding.wsdl"
            serviceClass = PersonV3::class.java
            serviceName = QName("http://nav.no/tjeneste/virksomhet/person/v3/Binding", "Person_v3")
            endpointName = QName("http://nav.no/tjeneste/virksomhet/person/v3/Binding", "Person_v3Port")
            features = listOf(WSAddressingFeature(), MetricFeature())
        }

        return factory.create(PersonV3::class.java)
    }

    @Test
    fun `successful request should increase success counter`() {
        val latencyCountBefore = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_latency_count",
                arrayOf("service", "operation"), arrayOf("Person_v3", "ping")) ?: 0.0
        val totalSuccessCountBefore = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_total",
                arrayOf("service", "operation", "status"), arrayOf("Person_v3", "ping", "success")) ?: 0.0
        val totalErrorCountBefore = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_total",
                arrayOf("service", "operation", "status"), arrayOf("Person_v3", "ping", "error")) ?: 0.0
        val totalFailureCountBefore = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_total",
                arrayOf("service", "operation", "status"), arrayOf("Person_v3", "ping", "failure")) ?: 0.0

        val port = createPort()

        WireMock.stubFor(WireMock.post("/")
                .willReturn(WireMock.okTextXml(successful_response)))

        port.ping()

        val latencyCountAfter = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_latency_count",
                arrayOf("service", "operation"), arrayOf("Person_v3", "ping")) ?: 0.0
        val latencySumAfter = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_latency_sum",
                arrayOf("service", "operation"), arrayOf("Person_v3", "ping")) ?: 0.0
        val totalSuccessCountAfter = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_total",
                arrayOf("service", "operation", "status"), arrayOf("Person_v3", "ping", "success")) ?: 0.0
        val totalErrorCountAfter = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_total",
                arrayOf("service", "operation", "status"), arrayOf("Person_v3", "ping", "error")) ?: 0.0
        val totalFailureCountAfter = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_total",
                arrayOf("service", "operation", "status"), arrayOf("Person_v3", "ping", "failure")) ?: 0.0

        Assertions.assertEquals(1.0, latencyCountAfter - latencyCountBefore)
        Assertions.assertNotNull(latencySumAfter)
        Assertions.assertEquals(1.0, totalSuccessCountAfter - totalSuccessCountBefore)
        Assertions.assertEquals(totalErrorCountBefore, totalErrorCountAfter)
        Assertions.assertEquals(totalFailureCountBefore, totalFailureCountAfter)
    }

    @Test
    fun `soap faults should increase error counter`() {
        val latencyCountBefore = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_latency_count",
                arrayOf("service", "operation"), arrayOf("Person_v3", "ping")) ?: 0.0
        val totalSuccessCountBefore = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_total",
                arrayOf("service", "operation", "status"), arrayOf("Person_v3", "ping", "success")) ?: 0.0
        val totalErrorCountBefore = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_total",
                arrayOf("service", "operation", "status"), arrayOf("Person_v3", "ping", "error")) ?: 0.0
        val totalFailureCountBefore = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_total",
                arrayOf("service", "operation", "status"), arrayOf("Person_v3", "ping", "failure")) ?: 0.0

        val port = createPort()

        WireMock.stubFor(WireMock.post("/")
                .willReturn(WireMock.okTextXml(soap_fault_response)))

        try {
            port.ping()
            fail("Expected SOAPFaultException to be thrown")
        } catch (err: SOAPFaultException) {
            // ok
            err.printStackTrace()
        }

        val latencyCountAfter = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_latency_count",
                arrayOf("service", "operation"), arrayOf("Person_v3", "ping")) ?: 0.0
        val latencySumAfter = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_latency_sum",
                arrayOf("service", "operation"), arrayOf("Person_v3", "ping")) ?: 0.0
        val totalSuccessCountAfter = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_total",
                arrayOf("service", "operation", "status"), arrayOf("Person_v3", "ping", "success")) ?: 0.0
        val totalErrorCountAfter = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_total",
                arrayOf("service", "operation", "status"), arrayOf("Person_v3", "ping", "error")) ?: 0.0
        val totalFailureCountAfter = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_total",
                arrayOf("service", "operation", "status"), arrayOf("Person_v3", "ping", "failure")) ?: 0.0

        Assertions.assertEquals(1.0, latencyCountAfter - latencyCountBefore)
        Assertions.assertNotNull(latencySumAfter)
        Assertions.assertEquals(1.0, totalErrorCountAfter - totalErrorCountBefore)
        Assertions.assertEquals(totalSuccessCountBefore, totalSuccessCountAfter)
        Assertions.assertEquals(totalFailureCountBefore, totalFailureCountAfter)
    }

    @Test
    fun `network errors increase failure counter`() {
        val latencyCountBefore = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_latency_count",
                arrayOf("service", "operation"), arrayOf("Person_v3", "ping")) ?: 0.0
        val totalSuccessCountBefore = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_total",
                arrayOf("service", "operation", "status"), arrayOf("Person_v3", "ping", "success")) ?: 0.0
        val totalErrorCountBefore = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_total",
                arrayOf("service", "operation", "status"), arrayOf("Person_v3", "ping", "error")) ?: 0.0
        val totalFailureCountBefore = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_total",
                arrayOf("service", "operation", "status"), arrayOf("Person_v3", "ping", "failure")) ?: 0.0

        val port = createPort()

        WireMock.stubFor(WireMock.post("/")
                .willReturn(WireMock.aResponse()
                        .withStatus(500)
                        .withFault(Fault.EMPTY_RESPONSE)))

        try {
            port.ping()
            fail("Expected an exception to be thrown")
        } catch (err: Exception) {
            // ok
            err.printStackTrace()
        }

        val latencyCountAfter = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_latency_count",
                arrayOf("service", "operation"), arrayOf("Person_v3", "ping")) ?: 0.0
        val latencySumAfter = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_latency_sum",
                arrayOf("service", "operation"), arrayOf("Person_v3", "ping")) ?: 0.0
        val totalSuccessCountAfter = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_total",
                arrayOf("service", "operation", "status"), arrayOf("Person_v3", "ping", "success")) ?: 0.0
        val totalErrorCountAfter = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_total",
                arrayOf("service", "operation", "status"), arrayOf("Person_v3", "ping", "error")) ?: 0.0
        val totalFailureCountAfter = CollectorRegistry.defaultRegistry.getSampleValue("webservice_calls_total",
                arrayOf("service", "operation", "status"), arrayOf("Person_v3", "ping", "failure")) ?: 0.0

        Assertions.assertEquals(1.0, latencyCountAfter - latencyCountBefore)
        Assertions.assertNotNull(latencySumAfter)
        Assertions.assertEquals(1.0, totalFailureCountAfter - totalFailureCountBefore)
        Assertions.assertEquals(totalSuccessCountBefore, totalSuccessCountAfter)
        Assertions.assertEquals(totalErrorCountBefore, totalErrorCountAfter)
    }
}

private val successful_response = """
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
   <soapenv:Header xmlns:wsa="http://www.w3.org/2005/08/addressing">
      <wsa:Action>http://nav.no/tjeneste/virksomhet/person/v3/Person_v3/pingResponse</wsa:Action>
   </soapenv:Header>
   <soapenv:Body>
      <ns2:pingResponse xmlns:ns2="http://nav.no/tjeneste/virksomhet/person/v3"/>
   </soapenv:Body>
</soapenv:Envelope>
""".trimIndent()

private val soap_fault_response = """
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
    <soapenv:Header/>
    <soapenv:Body>
        <soapenv:Fault>
            <faultcode>soapenv:Server</faultcode>
            <faultstring>Ingen forekomster funnet</faultstring>
            <detail>
                <ns2:hentPersonpersonIkkeFunnet xmlns:ns2="http://nav.no/tjeneste/virksomhet/person/v3" xmlns:ns3="http://nav.no/tjeneste/virksomhet/person/v3/informasjon">
                    <feilkilde>TPSWS</feilkilde>
                    <feilaarsak>Person med id 12345678901 ikke funnet.</feilaarsak>
                    <feilmelding>Person ikke funnet</feilmelding>
                    <tidspunkt>2019-01-01T00:00:00.000+01:00</tidspunkt>
                </ns2:hentPersonpersonIkkeFunnet>
            </detail>
        </soapenv:Fault>
    </soapenv:Body>
</soapenv:Envelope>
""".trimIndent()
