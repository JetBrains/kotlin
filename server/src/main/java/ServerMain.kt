import clInterface.DebugClInterface
import com.martiansoftware.jsap.JSAP
import net.car.Dropper
import net.car.client.Client
import objects.CarReal
import objects.Environment
import objects.emulator.CarEmulator
import objects.emulator.EmulatedRoom
import objects.emulator.Rng
import roomScanner.CarController
import roomScanner.RoomScanner

fun main(args: Array<String>) {

    val clParser = JSAP()
    setClOptions(clParser)
    val argsConfig = clParser.parse(args)
    if (!argsConfig.success() || argsConfig.getBoolean("help")) {
        println(clParser.getHelp())
        return
    }
    if (argsConfig.getBoolean("emulator")) {
        val pathToRoomConfig = argsConfig.getString("test room")
        val randomSeed = argsConfig.getLong("seed")
        val useRandom = argsConfig.getBoolean("random")
        val carUid = 1
        val emulatedRoom = EmulatedRoom.EmulatedRoomFromFile(pathToRoomConfig)
        if (emulatedRoom == null) {
            println("error parsin room from file $pathToRoomConfig")
            return
        }
        Environment.map.put(carUid, CarEmulator(carUid, emulatedRoom, useRandom, Rng(randomSeed)))
    }
    var roomScanner: RoomScanner? = null
    val carServer = net.car.server.Server.createCarServerThread()
    val webServer = net.web.server.Server.createWebServerThread()
    val carsDestroy = Dropper.createCarsDestroyThread()
    carServer.start()
    carsDestroy.start()
    webServer.start()

    if (args.contains("--scan")) {
        Environment.onCarConnect { car ->
            if (car is CarReal) {
                roomScanner = RoomScanner(CarController(car))
                roomScanner!!.start()
            }
        }
    }

    DebugClInterface.run()

    carsDestroy.interrupt()
    carServer.interrupt()
    webServer.interrupt()
    Client.shutDownClient()

    roomScanner?.interrupt()

}
