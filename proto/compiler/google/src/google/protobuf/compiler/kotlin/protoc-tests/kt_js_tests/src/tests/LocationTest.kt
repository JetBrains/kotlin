package tests

import CodedInputStream
import LocationResponse

object LocationTest {
    fun generateKtLocationResponse(): LocationResponse {
        fun generateKtLocationData(): LocationResponse.LocationData {
            return LocationResponse.LocationData.BuilderLocationData(
                    Util.nextInt(),
                    Util.nextInt(),
                    Util.nextInt()
            ).build()
        }

        return LocationResponse.BuilderLocationResponse(
                generateKtLocationData(),
                Util.nextInt(),
                Util.nextInt()
        ).build()
    }

    fun compareLocationResponses(kt1: LocationResponse, kt2: LocationResponse): Boolean {
        fun compareLocationDatas(kt1: LocationResponse.LocationData, kt2: LocationResponse.LocationData): Boolean {
            return kt1.x == kt2.x && kt1.y == kt2.y && kt1.angle == kt2.angle
        }

        return kt1.code == kt2.code && compareLocationDatas(kt1.locationResponseData, kt2.locationResponseData)
    }

    fun ktToKtOnce() {
        val msg = generateKtLocationResponse()
        val outs = Util.getKtOutputStream(msg.getSizeNoTag())
        msg.writeTo(outs)

        val ins = CodedInputStream(outs.buffer)
        val readMsg = LocationResponse.BuilderLocationResponse(LocationResponse.LocationData.BuilderLocationData(0, 0, 0).build(), 0, 0).build()
        readMsg.mergeFrom(ins)

        Util.assert(readMsg.errorCode == 0)
        Util.assert(compareLocationResponses(msg, readMsg))
    }

    val testRuns = 1000

    fun runTests() {
        for (i in 0..testRuns) {
            ktToKtOnce()
        }
    }
}
