import car.Dropper
import car.client.Client

val carServerPort: Int = 7925
val webServerPort: Int = 7926
val getLocationUrl = "/getLocation"
val setRouteUrl = "/route"
val setRouteMetricUrl = "/routeMetric"
val connectUrl = "/connect"
val debugMemoryUrl = "/debug/memory"
val sonarUrl = "/sonar"

fun main(args: Array<String>) {

    val carServer = car.server.Server.getCarServerThread(carServerPort)
    val webServer = web.server.Server.getWebServerThread(webServerPort)
    val carsDestroy = Dropper.getCarsDestroyThread()
    carServer.start()
    carsDestroy.start()
    webServer.start()

    //CL user interface
    DebugClInterface.run()

    carsDestroy.interrupt()
    carServer.interrupt()
    webServer.interrupt()
    Client.shutDownClient()
}
