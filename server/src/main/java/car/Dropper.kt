package car

import Exceptions.InactiveCarException
import car.client.Client
import io.netty.handler.codec.http.DefaultHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpVersion
import objects.Environment
import kotlin.concurrent.thread

object Dropper {
    private val timeDeltaToDrop = 600000//time in ms.
    // if car is inactive more than this time, this thread drop session with car

    fun getCarsDestroyThread(): Thread {
        return thread(false, false, null, "dropCar", -1, getThreadCode())
    }

    private fun getThreadCode(): () -> Unit {
        return {
            var stopped = false
            while (!stopped) {
                val environment = Environment.instance
                synchronized(environment, {
                    val currentTime = System.currentTimeMillis()
                    val keysToRemove = mutableListOf<Int>()
                    for ((key, value) in environment.map) {
                        if ((value.lastAction + timeDeltaToDrop) < currentTime) {
                            keysToRemove.add(key)
                        }
                    }
                    dropInactiveCar(keysToRemove, environment)
                })
                try {
                    Thread.sleep(60000)
                } catch (e: InterruptedException) {
                    println("thread for destroy cars stopped")
                    stopped = true
                }
            }
        }
    }

    private fun dropInactiveCar(keysToRemove: List<Int>, environment: Environment) {
        for (key in keysToRemove) {
            try {
                val carValue = environment.map[key] ?: continue
                val request = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/ping")
                Client.sendRequest(request, carValue.host, carValue.port, mapOf(Pair("uid", key)))
            } catch (e: InactiveCarException) {
                environment.map.remove(key)
            }
        }
    }
}