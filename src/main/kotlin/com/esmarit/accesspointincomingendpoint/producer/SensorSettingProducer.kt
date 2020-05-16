package com.esmarit.accesspointincomingendpoint.producer

import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Output
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.MessageBuilder

@EnableBinding(SensorSettingSource::class)
class SensorSettingProducer(private val source: SensorSettingSource) {
    fun process(event: SensorSetting) {
        event.run {
            source.output().send(
                MessageBuilder
                    .withPayload(this)
                    .setHeader(KafkaHeaders.MESSAGE_KEY, this.sensorId.toByteArray())
                    .build()
            )
        }
    }
}

data class SensorSetting(
    val id: String,
    val inEdge: Int,
    val limitEdge: Int,
    val location: String,
    val outEdge: Int,
    val sensorId: String,
    val spot: String
)

interface SensorSettingSource {

    @Output(SENSOR_SETTINGS_OUTPUT)
    fun output(): MessageChannel

    companion object {
        const val SENSOR_SETTINGS_OUTPUT = "sensor-settings-output"
    }
}