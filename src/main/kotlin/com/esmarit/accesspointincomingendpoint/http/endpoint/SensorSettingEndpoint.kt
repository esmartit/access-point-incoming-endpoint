package com.esmarit.accesspointincomingendpoint.http.endpoint

import com.esmarit.accesspointincomingendpoint.producer.SensorSetting
import com.esmarit.accesspointincomingendpoint.producer.SensorSettingProducer
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping(value = ["/sensor-settings"])
class SensorSettingEndpoint(private val producer: SensorSettingProducer) {

    @PostMapping
    fun sendSensorSettingToKafka(@RequestBody payload: Mono<SensorSetting>): Mono<ResponseEntity<String>> {
        return payload
            .doOnNext { producer.process(it) }
            .map { ResponseEntity.accepted().body("") }
    }
}