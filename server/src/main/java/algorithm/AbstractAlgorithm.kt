package algorithm

import CodedInputStream
import CodedOutputStream
import Logger
import RouteMetricRequest
import SonarRequest
import SonarResponse
import algorithm.geometry.Angle
import algorithm.geometry.AngleData
import net.car.client.Client
import objects.Car
import java.net.ConnectException
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

abstract class AbstractAlgorithm(val thisCar: Car) {

    open val ATTEMPTS = 1
    open val SMOOTHING = SonarRequest.Smoothing.NONE
    open val WINDOW_SIZE = 0

    protected val FORWARD = 0
    protected val BACKWARD = 1
    protected val LEFT = 2
    protected val RIGHT = 3

    private val historySize = 10
    private val history = Stack<RouteMetricRequest>()

    private var prevState: CarState? = null
    private var prevSonarDistances = mapOf<Angle, AngleData>()
    private val defaultAngles = arrayOf(Angle(0), Angle(70), Angle(75), Angle(80), Angle(85), Angle(90), Angle(95), Angle(100), Angle(105), Angle(110), Angle(180))

    protected var requiredAngles = defaultAngles

    protected enum class CarState {
        WALL,
        INNER,
        OUTER

    }

    private var iterationCounter = 0

    protected fun getData(angles: Array<Angle>): IntArray {

        val anglesIntArray = (angles.map { it -> it.degs() }).toIntArray()
        val message = SonarRequest.BuilderSonarRequest(
                angles = anglesIntArray,
                attempts = IntArray(angles.size, { ATTEMPTS }),
                smoothing = SMOOTHING,
                windowSize = WINDOW_SIZE)
                .build()
        val requestBytes = ByteArray(message.getSizeNoTag())
        message.writeTo(CodedOutputStream(requestBytes))
        try {
            val futureListener = thisCar.carConnection.sendRequest(Client.Request.SONAR, requestBytes)
            val bytes = futureListener.get(300, TimeUnit.SECONDS).responseBodyAsBytes
            val responseMessage = SonarResponse.BuilderSonarResponse(IntArray(0)).build()
            responseMessage.mergeFrom(CodedInputStream(bytes))
            return responseMessage.distances
        } catch (e: ConnectException) {
            println("connection error!")
        } catch (e: TimeoutException) {
            println("don't have response from net.car. Timeout!")
        }
        return IntArray(0)
    }

    //todo методы управления машинкой должны быть в отдельном классе по аналогии с carController
    protected fun moveCar(message: RouteMetricRequest) {
        val requestBytes = ByteArray(message.getSizeNoTag())
        message.writeTo(CodedOutputStream(requestBytes))
        moveCar(requestBytes)
    }


    private fun moveCar(messageBytes: ByteArray) {
        try {
            thisCar.carConnection.sendRequest(Client.Request.ROUTE_METRIC, messageBytes).get(60, TimeUnit.SECONDS)
        } catch (e: ConnectException) {
            println("connection error!")
        } catch (e: TimeoutException) {
            println("don't have response from net.car. Timeout!")
        }
        return
    }

    fun iterate() {
        Logger.log("============= STARTING ITERATION $iterationCounter ============")
        Logger.indent()
        iterationCounter++
        if (RoomModel.finished) {
            return
        }
        val angles = getAngles()
        val distances = getData(angles)
        if (distances.size != angles.size) {
            throw RuntimeException("error! angles and distances have various sizes")
        }
        val anglesDistances = mutableMapOf<Angle, AngleData>()
        for (i in 0..angles.size - 1) {
            anglesDistances.put(angles[i], AngleData(angles[i], distances[i]))
        }

        this.requiredAngles = defaultAngles

        val state = getCarState(anglesDistances)

        if (state == null) {
            addCancelIterationToLog()
            return
        }
        val command = getCommand(anglesDistances, state)
        if (command == null) {
            addCancelIterationToLog()
            return
        }

        addToHistory(command)

        afterGetCommand(command)
        Logger.log("Sending command:")
        Logger.indent()
        Logger.log("Directions = ${Arrays.toString(command.directions)}")
        Logger.log("Distanced = ${Arrays.toString(command.distances)}")
        Logger.outdent()
        println(Arrays.toString(command.directions))
        println(Arrays.toString(command.distances))

        this.prevSonarDistances = anglesDistances
        this.prevState = state

        moveCar(command)
        Logger.outdent()
        Logger.log("============= FINISHING ITERATION $iterationCounter ============")
        Logger.log("")
    }

    private fun addCancelIterationToLog() {
        Logger.log("iteration cancelled. need more data from sonar")
        Logger.outdent()
        Logger.log("============= FINISHING ITERATION $iterationCounter ============")
        Logger.log("")
    }


    private fun getAngles(): Array<Angle> {
        return requiredAngles
    }

    private fun addToHistory(command: RouteMetricRequest) {
        history.push(command)
        while (history.size > historySize) {
            history.removeAt(0)
        }
    }

    private fun popFromHistory(): RouteMetricRequest {
        return history.pop()
    }

    private fun inverseCommand(command: RouteMetricRequest): RouteMetricRequest {
        val res = RouteMetricRequest.BuilderRouteMetricRequest(command.distances, command.directions)
        res.distances.reverse()
        res.directions.reverse()

        for ((index, dir) in res.directions.withIndex()) {
            res.directions[index] = when (dir) {
                FORWARD -> BACKWARD
                BACKWARD -> FORWARD
                LEFT -> RIGHT
                RIGHT -> LEFT
                else -> throw IllegalArgumentException("Unexpected direction = $dir found during command inversion")
            }
        }
        return res.build()
    }

    protected fun rollback() {
        val lastCommand = popFromHistory()
        val invertedCommand = inverseCommand(lastCommand)
        Logger.log("Rollback:")
        Logger.indent()
        Logger.log("Last command: ${lastCommand.toString()}")
        Logger.log("Inverted cmd: ${invertedCommand.toString()}")
        Logger.outdent()
        moveCar(invertedCommand)
    }

    protected fun rollback(steps: Int) {
        Logger.log("=== Starting rollback for $steps steps ===")
        Logger.indent()
        var stepsRemaining = steps
        while (stepsRemaining > 0 && history.size > 0) {
            Logger.log("Step: ${steps - stepsRemaining + 1}")
            rollback()
            stepsRemaining--
        }
        Logger.outdent()
        Logger.log("=== Finished rollback ===")
    }

    protected abstract fun getCarState(anglesDistances: Map<Angle, AngleData>): CarState?
    protected abstract fun getCommand(anglesDistances: Map<Angle, AngleData>, state: CarState): RouteMetricRequest?
    protected abstract fun afterGetCommand(route: RouteMetricRequest)
    abstract fun isCompleted(): Boolean

}