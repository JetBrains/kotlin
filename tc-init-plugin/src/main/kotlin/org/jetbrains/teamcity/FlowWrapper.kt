package org.jetbrains.teamcity

import java.util.concurrent.ConcurrentHashMap

class FlowWrapper(
    val id: String
) {
    private val started = ConcurrentHashMap.newKeySet<String>()

    fun start(flowId: String, action: (ServiceMessage) -> Unit) {
        if (flowId != id && started.add(flowId)) {
            val message = ServiceMessage(
                name = "flowStarted",
                attributes = mutableMapOf(
                    "flowId" to flowId,
                    "parent" to id
                )
            )
            action(message)
        }
    }

    fun finish(flowId: String, action: (ServiceMessage) -> Unit) {
        if (flowId != id && started.remove(flowId)) {
            val message = ServiceMessage(
                name = "flowFinished",
                attributes = mutableMapOf(
                    "flowId" to flowId
                )
            )
            action(message)
        }
    }
}
