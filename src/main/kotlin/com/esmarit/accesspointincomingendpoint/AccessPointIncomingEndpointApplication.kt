package com.esmarit.accesspointincomingendpoint

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AccessPointIncomingEndpointApplication

fun main(args: Array<String>) {
    println("hello test 2")
    runApplication<AccessPointIncomingEndpointApplication>(*args)
}
