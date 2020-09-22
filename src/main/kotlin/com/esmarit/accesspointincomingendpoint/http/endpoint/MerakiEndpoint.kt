package com.esmarit.accesspointincomingendpoint.http.endpoint

import com.esmarit.accesspointincomingendpoint.http.request.Data
import com.esmarit.accesspointincomingendpoint.http.request.Location
import com.esmarit.accesspointincomingendpoint.http.request.MerakiPayload
import com.esmarit.accesspointincomingendpoint.http.request.Observation
import com.esmarit.accesspointincomingendpoint.producer.DeviceLocation
import com.esmarit.accesspointincomingendpoint.producer.DeviceSeen
import com.esmarit.accesspointincomingendpoint.producer.DeviceSeenEvent
import com.esmarit.accesspointincomingendpoint.producer.EventProducer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets


@RestController
@RequestMapping(value = ["/meraki"])
class MerakiEndpoint(
    private val objectMapper: ObjectMapper,
    private val producer: EventProducer
) {

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
            .map { it.data }
            .flatMapIterable { mapToDeviceSeenEvents(it) }
            .doOnNext { producer.process(it) }
            .reduce(ResponseEntity.accepted().build(), { t, _ -> t })
    }

    @PostMapping(
        value = ["/upload-devices"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    @ResponseStatus(value = HttpStatus.OK)
    fun upload(@RequestPart("files") filePartFlux: Flux<FilePart>): Mono<ResponseEntity<String>> {
        return filePartFlux
            .flatMap { readLines(it) }
            .map { objectMapper.readValue<MerakiPayload>(it.toString()) }
            .map { it.data }
            .flatMapIterable { mapToDeviceSeenEvents(it) }
            .doOnNext { producer.process(it) }
            .reduce(ResponseEntity.accepted().build(), { t, _ -> t })
    }

    private fun readLines(filePart: FilePart): Mono<StringBuilder> {
        return filePart.content().map {
            val bytes = ByteArray(it.readableByteCount())
            it.read(bytes)
            DataBufferUtils.release(it)
            String(bytes, StandardCharsets.UTF_8)
        }.reduce(StringBuilder()) { acc, curr -> acc.append(curr) }
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
