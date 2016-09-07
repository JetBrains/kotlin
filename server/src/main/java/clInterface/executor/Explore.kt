package clInterface.executor

import CodedInputStream
import roomScanner.serialize
import SonarExploreAngleRequest
import SonarExploreAngleResponse
import net.car.client.Client
import objects.CarReal
import objects.Environment

class Explore : CommandExecutor {

    override fun execute(command: String) {
        val params = command.split(" ")
        val car = Environment.map[params[1].toInt()]!!
        if (!(car is CarReal)) {
            return
        }
        val angle = params[2].toInt()
        val window = params[3].toInt()

        val request = SonarExploreAngleRequest.BuilderSonarExploreAngleRequest(angle, window).build()
        val responseData = car.carConnection.sendRequest(
                Client.Request.EXPLORE_ANGLE,
                serialize(request.getSizeNoTag(), { request.writeTo(it) })
        ).get().responseBodyAsBytes

        val distances = SonarExploreAngleResponse.BuilderSonarExploreAngleResponse(IntArray(0)).parseFrom(CodedInputStream(responseData)).build().distances

        println("Received distances: [${distances.joinToString()}]")
    }
}