import RoomScanner.CarController
import RoomScanner.RoomScanner
import car.Dropper
import car.client.Client
import objects.Environment

val carServerPort: Int = 7925
val webServerPort: Int = 7926
val getLocationUrl = "/getLocation"
val setRouteUrl = "/route"
val setRouteMetricUrl = "/routeMetric"
val connectUrl = "/connect"
val debugMemoryUrl = "/debug/memory"
val sonarUrl = "/sonar"

fun main(args: Array<String>) {
    var roomScanner: RoomScanner? = null
    val carServer = car.server.Server.getCarServerThread(carServerPort)
    val webServer = web.server.Server.getWebServerThread(webServerPort)
    val carsDestroy = Dropper.getCarsDestroyThread()
    carServer.start()
    carsDestroy.start()
    webServer.start()

    if (args.contains("--scan")) {
        Environment.onCarConnect { car ->
            roomScanner = RoomScanner(CarController(car))
            roomScanner!!.start()
        }
    }

    //CL user interface
    DebugClInterface.run()

    carsDestroy.interrupt()
    carServer.interrupt()
    webServer.interrupt()
    Client.shutDownClient()

    roomScanner?.interrupt()
}
