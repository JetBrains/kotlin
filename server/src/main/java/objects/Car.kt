package objects

import SonarRequest
import org.asynchttpclient.ListenableFuture
import org.asynchttpclient.Response
import roomScanner.CarController

abstract class Car(val uid: Int) {

    var x = 0.0
    var y = 0.0
    var angle = 0.0

    abstract fun moveCar(distance: Int, direction: CarController.Direction): ListenableFuture<Response>

    abstract fun scan(angles: IntArray, attempts: Int, windowSize: Int, smoothing: SonarRequest.Smoothing): ListenableFuture<Response>
}