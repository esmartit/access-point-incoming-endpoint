package com.esmarit.accesspointincomingendpoint.producer

import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.messaging.Source
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.support.MessageBuilder

@EnableBinding(Source::class)
class EventProducer(private val source: Source) {

    private val logger = LoggerFactory.getLogger(EventProducer::class.java)

    fun process(event: DeviceSeenEvent) {

        logger.info("sending event: $event")

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