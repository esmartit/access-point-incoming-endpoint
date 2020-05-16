package com.esmarit.accesspointincomingendpoint.http.request

data class MerakiPayload(
    val data: Data,
    val secret: String,
    val type: String,
    val version: String
)

data class Data(
    val apFloors: List<String?>,
    val apMac: String,
    val apTags: List<String>,
    val observations: List<Observation>
)

data class Observation(
    val clientMac: String,
    val ipv4: String?,
    val ipv6: String?,
    val location: Location,
    val manufacturer: String?,
    val os: String?,
    val rssi: Int,
    val seenEpoch: Int,
    val seenTime: String,
    val ssid: String?
)

data class Location(
    val lat: Double?,
    val lng: Double?,
    val unc: Double?,
    val x: List<String?>,
    val y: List<String?>
)