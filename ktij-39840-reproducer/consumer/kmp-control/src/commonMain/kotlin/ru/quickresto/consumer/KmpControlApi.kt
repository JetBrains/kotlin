package ru.quickresto.consumer

import ru.quickresto.kkm.KkmContractFactory

/**
 * KMP control module (jvm target only, no Android).
 * Same mavenLocal dependency as android-lib; used as the healthy indexing baseline.
 */
object KmpControlApi {
    fun describeDevice(deviceId: String = "kmp-control"): String {
        val contract = KkmContractFactory.create(deviceId)
        return "kmp-control -> ${contract.ping()}"
    }
}
