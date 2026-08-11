package ru.quickresto.consumer

import ru.quickresto.kkm.KkmContractFactory

/**
 * Android library entry that references the published KMP API.
 * Used to surface KTIJ-39840: dependency listed but JVM roots missing from index.
 */
object AndroidLibApi {
    fun describeDevice(deviceId: String = "android-lib"): String {
        val contract = KkmContractFactory.create(deviceId)
        return "android-lib -> ${contract.ping()}"
    }
}
