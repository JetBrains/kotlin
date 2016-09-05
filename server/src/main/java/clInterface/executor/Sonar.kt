package clInterface.executor

import CodedOutputStream
import SonarRequest
import net.car.client.Client
import objects.Car
import objects.Environment
import java.net.ConnectException

class Sonar : CommandExecutor {

    private val SONAR_REGEX = Regex("sonar [0-9]{1,10}")

    override fun execute(command: String) {
        if (!SONAR_REGEX.matches(command)) {
            println("incorrect args of command sonar.")
            return
        }
        val id: Int
        try {
            id = command.split(" ")[1].toInt()
        } catch (e: NumberFormatException) {
            e.printStackTrace()
            println("error in converting id to int type")
            return
        }
        val car: Car? = synchronized(Environment, {
            Environment.map[id]
        })
        if (car == null) {
            println("car with id=$id not found")
            return
        }
        val requestMessage = getSonarRequest() ?: return
        val requestBytes = ByteArray(requestMessage.getSizeNoTag())
        requestMessage.writeTo(CodedOutputStream(requestBytes))
        try {
            car.carConnection.sendRequest(Client.Request.SONAR, requestBytes)
        } catch (e: ConnectException) {
            synchronized(Environment, {
                Environment.map.remove(id)
            })
        }
    }

    private fun getSonarRequest(): SonarRequest? {
        println("print angles, after printing all angles print done")
        val angles = arrayListOf<Int>()
        while (true) {
            val command = readLine()!!.toLowerCase()
            when (command) {
                "reset" -> return null
                "done" -> {
                    val sonarBuilder = SonarRequest.BuilderSonarRequest(angles.toIntArray(), IntArray(angles.size, { 5 }), 3, SonarRequest.Smoothing.MEDIAN)
                    return sonarBuilder.build()
                }
                else -> {
                    try {
                        val angle = command.toInt()
                        if (angle < 0 || angle > 180) {
                            println("incorrect angle $angle. angle must be in [0,180] and div on 4")
                        } else {
                            angles.add(angle)
                        }
                    } catch (e: NumberFormatException) {
                        println("error in converting angle to int. try again")
                    }
                }
            }
        }
    }
}