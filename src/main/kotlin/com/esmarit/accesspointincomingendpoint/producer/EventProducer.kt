package com.esmarit.accesspointincomingendpoint.producer

import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.messaging.Source
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.support.MessageBuilder

@EnableBinding(Source::class)
class EventProducer(private val source: Source) {
    fun process(event: DeviceSeenEvent) {
        event.run {
            source.output().send(
                MessageBuilder
                    .withPayload(this)
                    .setHeader(KafkaHeaders.MESSAGE_KEY, this.device.clientMac.toByteArray())
                    .build()
            )
        }
    }
}