import roomScanner.CarController
import roomScanner.RoomScanner
import clInterface.DebugClInterface
import net.car.Dropper
import net.car.client.Client
import objects.Environment

fun main(args: Array<String>) {
    var roomScanner: RoomScanner? = null
    val carServer = net.car.server.Server.createCarServerThread()
    val webServer = net.web.server.Server.createWebServerThread()
    val carsDestroy = Dropper.createCarsDestroyThread()
    carServer.start()
    carsDestroy.start()
    webServer.start()

    if (args.contains("--scan")) {
        Environment.onCarConnect { car ->
            roomScanner = RoomScanner(CarController(car))
            roomScanner!!.start()
        }
    }

    DebugClInterface.run()

    carsDestroy.interrupt()
    carServer.interrupt()
    webServer.interrupt()
    Client.shutDownClient()

    roomScanner?.interrupt()
}
