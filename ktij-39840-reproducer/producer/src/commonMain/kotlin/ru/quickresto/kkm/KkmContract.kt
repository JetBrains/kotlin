package ru.quickresto.kkm

/**
 * Minimal public API mirroring a published KMP library surface
 * (ticket-shaped coords: ru.quickresto:kkm-contract:3.0.0).
 */
interface KkmContract {
    val deviceId: String
    fun ping(): String
}

object KkmContractFactory {
    fun create(deviceId: String = "demo-device"): KkmContract = DefaultKkmContract(deviceId)
}

internal expect fun platformLabel(): String

private class DefaultKkmContract(
    override val deviceId: String,
) : KkmContract {
    override fun ping(): String = "pong from $deviceId on ${platformLabel()}"
}
