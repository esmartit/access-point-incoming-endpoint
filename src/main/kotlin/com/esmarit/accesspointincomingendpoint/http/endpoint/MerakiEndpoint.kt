package com.esmarit.accesspointincomingendpoint.http.endpoint

import com.esmarit.accesspointincomingendpoint.http.request.Data
import com.esmarit.accesspointincomingendpoint.http.request.Location
import com.esmarit.accesspointincomingendpoint.http.request.MerakiPayload
import com.esmarit.accesspointincomingendpoint.http.request.Observation
import com.esmarit.accesspointincomingendpoint.producer.DeviceLocation
import com.esmarit.accesspointincomingendpoint.producer.DeviceSeen
import com.esmarit.accesspointincomingendpoint.producer.DeviceSeenEvent
import com.esmarit.accesspointincomingendpoint.producer.EventProducer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@RestController
@RequestMapping(value = ["/meraki"])
class MerakiEndpoint(private val producer: EventProducer) {

    private val logger = LoggerFactory.getLogger(MerakiEndpoint::class.java)

    @Value("\${app.meraki.secret}")
    private lateinit var merakiSecretCheck: String

    @GetMapping("/devices")
    fun merakiSecretCheck(): ResponseEntity<String> {
        return ResponseEntity.ok().body(merakiSecretCheck)
    }

    @PostMapping(value = ["/devices"])
    fun sendMessageToKafkaTopic(@RequestBody payload: Mono<MerakiPayload>): Mono<ResponseEntity<String>> {
        return payload
            .doOnNext {
                logger.info("received payload: $it")
            }
            .map { it.data }
            .flatMapIterable { mapToDeviceSeenEvents(it) }
            .parallel()
            .runOn(Schedulers.parallel())
            .doOnNext { producer.process(it) }
            .sequential()
            .reduce(ResponseEntity.accepted().build(), { t, _ -> t })
    }

    private fun mapToDeviceSeenEvents(incomingData: Data): List<DeviceSeenEvent> {

        val apMac = incomingData.apMac
        val apFloors = incomingData.apFloors

        return incomingData.observations.map { it.toDeviceSeen() }
            .map {
                DeviceSeenEvent(
                    apMac = apMac,
                    device = it,
                    apFloors = apFloors
                )
            }
    }
}

fun Observation.toDeviceSeen() =
    DeviceSeen(
        clientMac,
        ipv4,
        ipv6,
        location.toDeviceLocation(),
        manufacturer,
        os,
        rssi - 95,
        seenEpoch,
        seenTime,
        ssid
    )

fun Location.toDeviceLocation() =
    DeviceLocation(lat, lng, unc, x, y)
