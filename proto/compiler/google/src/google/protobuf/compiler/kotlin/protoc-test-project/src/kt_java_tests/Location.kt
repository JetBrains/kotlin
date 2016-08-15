package tests

import java_msg.Location
import main.kotlin.CodedInputStream
import main.kotlin.LocationResponse
import java.io.ByteArrayOutputStream

/**
 * Created by user on 8/11/16.
 */

object Location {
    fun generateKtLocationResponse(): LocationResponse {
        fun generateKtLocationData(): LocationResponse.LocationData {
            return LocationResponse.LocationData.BuilderLocationData(
                    RandomGen.rnd.nextInt(),
                    RandomGen.rnd.nextInt(),
                    RandomGen.rnd.nextInt()
                ).build()
        }

        return LocationResponse.BuilderLocationResponse(
                generateKtLocationData(),
                RandomGen.rnd.nextInt(),
                RandomGen.rnd.nextInt()
            ).build()
    }

    fun generateJvLocationResponse(): Location.LocationResponse {
        fun generateJvLocationData(): Location.LocationResponse.LocationData {
            return java_msg.Location.LocationResponse.LocationData.newBuilder()
                        .setX(RandomGen.rnd.nextInt())
                        .setY(RandomGen.rnd.nextInt())
                        .setAngle(RandomGen.rnd.nextInt())
                        .build()
        }

        return java_msg.Location.LocationResponse.newBuilder()
                .setLocationResponseData(generateJvLocationData())
                .setCode(RandomGen.rnd.nextInt())
                .build()
    }

    fun compareLocationResponses(kt: LocationResponse, jv: Location.LocationResponse): Boolean {
        fun compareLocationDatas(kt: LocationResponse.LocationData, jv: Location.LocationResponse.LocationData): Boolean {
            return kt.x == jv.x && kt.y == jv.y && kt.angle == jv.angle
        }

        return kt.code == jv.code && compareLocationDatas(kt.locationResponseData, jv.locationResponseData)
    }

    fun ktToJavaOnce() {
        val kt = generateKtLocationResponse()
        val outs = Util.getKtOutputStream(kt.getSizeNoTag())
        kt.writeTo(outs)

        val ins = Util.KtOutputStreamToInputStream(outs)
        val jv = java_msg.Location.LocationResponse.parseFrom(ins)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareLocationResponses(kt, jv))
    }

    fun JavaToKtOnce() {
        val outs = ByteArrayOutputStream(100000)

        val jv = generateJvLocationResponse()
        jv.writeTo(outs)
        val ins = CodedInputStream(outs.toByteArray())
        val kt = LocationResponse.BuilderLocationResponse(LocationResponse.LocationData.BuilderLocationData(0, 0, 0).build(), 0, 0).build()
        kt.mergeFrom(ins)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareLocationResponses(kt, jv))
    }

    val testRuns = 1000

    fun runTests() {
        for (i in 0..testRuns) {
            ktToJavaOnce()
            JavaToKtOnce()
        }
    }
}
