package com.example.serverapp.core

import java.net.Inet4Address
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Finds the device's own LAN IPv4 address, so the UI can tell the user what to
 * type into the client on another device.
 *
 * Reads it from the network interfaces directly. The older WifiManager route
 * returns a deprecated int-encoded address and needs location permission on
 * recent Android versions.
 */
@Singleton
class NetworkAddressProvider @Inject constructor() {

    /** Returns e.g. "192.168.1.30", or null when not on a LAN. */
    fun localIpAddress(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces()
            .asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress }
            ?.hostAddress
    }.getOrNull()
}
