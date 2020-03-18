package com.esmarit.accesspointincomingendpoint.http.endpoint

import com.esmarit.accesspointincomingendpoint.http.request.Data
import com.esmarit.accesspointincomingendpoint.http.request.Location
import com.esmarit.accesspointincomingendpoint.http.request.MerakiPayload
import com.esmarit.accesspointincomingendpoint.http.request.Observation
import com.esmarit.accesspointincomingendpoint.producer.DeviceLocation
import com.esmarit.accesspointincomingendpoint.producer.DeviceSeen
import com.esmarit.accesspointincomingendpoint.producer.DeviceSeenEvent
import com.esmarit.accesspointincomingendpoint.producer.EventProducer
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

    @Value("\${app.meraki.secret}")
    private lateinit var merakiSecretCheck: String

    @GetMapping("/devices")
    fun merakiSecretCheck():ResponseEntity<String>{
        return ResponseEntity.ok().body(merakiSecretCheck)
    }

    @PostMapping(value = ["/devices"])
    fun sendMessageToKafkaTopic(@RequestBody payload: Mono<MerakiPayload>): Mono<ResponseEntity<String>> {

        return payload
            .map { it.data }
            .flatMapIterable { mapToDeviceSeenEvents(it) }
            .parallel()
            .runOn(Schedulers.parallel())
            .doOnNext { producer.process(it) }
            .sequential()
            .reduce(ResponseEntity.accepted().build<String>(), { t, k -> t })
    }

    private fun mapToDeviceSeenEvents(incomingData: Data): List<DeviceSeenEvent> {

        val tags = incomingData.apTags.filter { it.isNotBlank() }
            .map { it.split(":") }
            .map { it[0] to it[1] }
            .toMap()
        val groupName = tags["groupname"] as String
        val hotSpot = tags["hotspot"] as String
        val sensorName = tags["sensorname"] as String
        val spotId = tags["spot_id"] as String
        val apMac = incomingData.apMac
        val apFloors = incomingData.apFloors

        return incomingData.observations.map { it.toDeviceSeen() }
            .map {
                DeviceSeenEvent(
                    apMac,
                    groupName,
                    hotSpot,
                    sensorName,
                    spotId,
                    it,
                    apFloors
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
        rssi,
        seenEpoch,
        seenTime,
        ssid
    )

fun Location.toDeviceLocation() =
    DeviceLocation(lat, lng, unc, x, y)