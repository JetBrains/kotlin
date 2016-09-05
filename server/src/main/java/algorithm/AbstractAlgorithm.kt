package algorithm

import CodedInputStream
import Logger
import SonarRequest
import SonarResponse
import algorithm.geometry.Angle
import algorithm.geometry.AngleData
import objects.Car
import roomScanner.CarController.Direction.*
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

abstract class AbstractAlgorithm(val thisCar: Car) {

    open val ATTEMPTS = 1
    open val SMOOTHING = SonarRequest.Smoothing.NONE
    open val WINDOW_SIZE = 0

    private val historySize = 10
    private val history = Stack<RouteData>()

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

    fun iterate() {
        Logger.log("============= STARTING ITERATION $iterationCounter ============")
        Logger.indent()
        iterationCounter++
        if (RoomModel.finished) {
            return
        }
        val angles = getAngles()
        val future = thisCar.scan(angles.map { it.degs() }.toIntArray(), ATTEMPTS, WINDOW_SIZE, SMOOTHING)
        val bytes: ByteArray
        try {
            bytes = future.get(300, TimeUnit.SECONDS).responseBodyAsBytes
        } catch (e: TimeoutException) {
            println("time out")
            return
        }
        val sonarResponse = SonarResponse.BuilderSonarResponse(IntArray(0)).build()
        sonarResponse.mergeFrom(CodedInputStream(bytes))
        val distances = sonarResponse.distances

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
        Logger.outDent()
        println(Arrays.toString(command.directions))
        println(Arrays.toString(command.distances))

        this.prevSonarDistances = anglesDistances
        this.prevState = state

        command.distances.forEachIndexed { idx, distance ->
            thisCar.moveCar(distance, command.directions[idx])
        }

        Logger.outDent()
        Logger.log("============= FINISHING ITERATION $iterationCounter ============")
        Logger.log("")
    }

    private fun addCancelIterationToLog() {
        Logger.log("iteration cancelled. need more data from sonar")
        Logger.outDent()
        Logger.log("============= FINISHING ITERATION $iterationCounter ============")
        Logger.log("")
    }


    private fun getAngles(): Array<Angle> {
        return requiredAngles
    }

    private fun addToHistory(command: RouteData) {
        history.push(command)
        while (history.size > historySize) {
            history.removeAt(0)
        }
    }

    private fun popFromHistory(): RouteData {
        return history.pop()
    }

    private fun inverseCommand(command: RouteData): RouteData {
        val res = RouteData(command.distances, command.directions)
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
        return res
    }

    protected fun rollback() {
        val lastCommand = popFromHistory()
        val invertedCommand = inverseCommand(lastCommand)
        Logger.log("Rollback:")
        Logger.indent()
        Logger.log("Last command: ${lastCommand.toString()}")
        Logger.log("Inverted cmd: ${invertedCommand.toString()}")
        Logger.outDent()
        invertedCommand.distances.forEachIndexed { idx, distance ->
            thisCar.moveCar(distance, invertedCommand.directions[idx])
        }
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
        Logger.outDent()
        Logger.log("=== Finished rollback ===")
    }

    protected abstract fun getCarState(anglesDistances: Map<Angle, AngleData>): CarState?
    protected abstract fun getCommand(anglesDistances: Map<Angle, AngleData>, state: CarState): RouteData?
    protected abstract fun afterGetCommand(route: RouteData)
    abstract fun isCompleted(): Boolean

}