/*
 * In this example you can see a crossroads. Traffic light change color by timer,
 * but you can change it manually using controls at the right part of screen.
 */
package traffic

import jquery.*
import org.w3c.dom.*
import kotlin.browser.document
import kotlin.browser.window
import kotlin.math.*
import kotlin.random.*
import kotlin.js.Date

fun getImage(path: String): HTMLImageElement {
    val image = window.document.createElement("img") as HTMLImageElement
    image.src = path
    return image
}

val canvas: HTMLCanvasElement
    get() {
        return window.document.getElementsByTagName("canvas").item(0)!! as HTMLCanvasElement
    }

val context: CanvasRenderingContext2D
    get() {
        return canvas.getContext("2d") as CanvasRenderingContext2D
    }


val PATH_TO_IMAGES = "https://try.kotlinlang.org/static/images/canvas/"


val state: CanvasState by lazy { CanvasState(canvas) }

var trafficLightUp = TrafficLight(v(180.0, 181.0), "up", "red")
var trafficLightDown = TrafficLight(v(100.0, 77.0), "down", "red")
var trafficLightLeft = TrafficLight(v(228.0, 109.0), "left", "green")
var trafficLightRight = TrafficLight(v(55.0, 145.0), "right", "green")


fun main(args: Array<String>) {
    state.addShape(Map(v(10.0, 10.0)))

    state.addShape(trafficLightLeft)
    state.addShape(trafficLightUp)
    state.addShape(trafficLightDown)
    state.addShape(trafficLightRight)
    state.addShape(Car(v(178.0, 205.0), "up", "red"))
    state.addShape(Car(v(95.0, 4.0), "down", "white"))
    state.addShape(Car(v(278.0, 108.0), "left", "blue"))
    state.addShape(Car(v(0.0, 142.0), "right", "black"))
    state.addShape(Border())
    state.addShape(Image(PATH_TO_IMAGES + "controls.png", v(380.0, 10.0), v(190.0, 56.0)))
    state.addShape(Button(PATH_TO_IMAGES + "lr.png", v(420.0, 70.0), v(120.0, 50.0)))
    state.addShape(Button(PATH_TO_IMAGES + "ud.png", v(455.0, 120.0), v(50.0, 120.0)))
}

fun v(x: Double, y: Double) = Vector(x, y)

class Image(val src: String, override var pos: Vector, var imageSize: Vector) : Shape() {
    override fun draw() {
        state.context.drawImage(getImage(src), 0.0, 0.0,
                                imageSize.x, imageSize.y,
                                pos.x, pos.y,
                                imageSize.x, imageSize.y)
    }

    fun contains(mousePos: Vector): Boolean = mousePos.isInRect(pos, imageSize)
}

class Button(val src: String, override var pos: Vector, var imageSize: Vector) : Shape() {
    var isMouseOver = false
    var isMouseDown = false

    override fun draw() {
        if (isMouseOver) {
            state.context.shadowed(v(-3.0, 3.0), 1.2) {
                state.context.drawImage(getImage(src), 0.0, 0.0,
                                        imageSize.x, imageSize.y,
                                        pos.x, pos.y,
                                        imageSize.x, imageSize.y)
            }
        } else if (isMouseDown) {
            state.context.shadowed(v(-3.0, 3.0), 0.8) {
                state.context.drawImage(getImage(src), 0.0, 0.0,
                                        imageSize.x, imageSize.y,
                                        pos.x, pos.y,
                                        imageSize.x, imageSize.y)
            }
        } else {
            state.context.drawImage(getImage(src), 0.0, 0.0,
                                    imageSize.x, imageSize.y,
                                    pos.x, pos.y,
                                    imageSize.x, imageSize.y)
        }
    }

    fun mouseClick() {
        isMouseDown = true
        window.setTimeout({
                              isMouseDown = false
                              Unit
                          }, 1000)
    }

    fun mouseOver() {
        isMouseOver = true
        window.setTimeout({
                              isMouseOver = false
                              Unit
                          }, 1000)
    }


    operator fun contains(mousePos: Vector): Boolean = mousePos.isInRect(pos, imageSize)
}

class Border() : Shape() {
    override var pos = Vector(4.0, 4.0);

    override fun draw() {
        state.context.fillStyle = Colors.white
        state.context.fillRect(2.0, 4.0, 10.0, 292.0)
        state.context.fillRect(330.0, 4.0, 370.0, 292.0)
        state.context.fillRect(2.0, 2.0, 330.0, 10.0)
        state.context.fillRect(4.0, 265.0, 340.0, 380.0)
        state.context.strokeStyle = Colors.black
        state.context.lineWidth = 4.0
        state.context.strokeRect(0.0, 0.0, state.width.toDouble(), state.height.toDouble())
    }

}

class Timer(override var pos: Vector) : Shape() {
    var timeLeftForChangeColor: Char = 'c'
    var timeStartLastChangeColor = Date().getTime();
    var timerLength = 13

    override fun draw() {
        timeLeftForChangeColor = ("" + (timerLength - (Date().getTime() - timeStartLastChangeColor) / 1000)).get(0)
        state.context.font = "bold 9px Arial, serif"
        state.context.fillStyle = Colors.black
        state.context.fillText("" + timeLeftForChangeColor, pos.x, pos.y)
    }

    fun resetTimer() {
        timeStartLastChangeColor = Date().getTime()
        timerLength = 10
    }
}

//Colors constants
object Colors {
    val black: String = "#000000"
    val white = "#FFFFFF"
    val grey = "#C0C0C0"
    val red = "#EF4137"
    val yellow = "#FCE013"
    val green = "#0E9648"
}

class TrafficLight(override var pos: Vector, val direction: String, val startColor: String) : Shape() {
    val list = mutableListOf<TrafficLightItem>()
    var size = Vector(27.0, 34.0);
    var timer = Timer(Vector(pos.x + 6, pos.y + 12))
    var currentColor = startColor;
    var isForceColorChange = false
    var shouldChangeColorForward = (startColor == "red")

    init {
        list.add(TrafficLightItem(v(pos.x, pos.y), PATH_TO_IMAGES + "red_color.png"))
        list.add(TrafficLightItem(v(pos.x, pos.y), PATH_TO_IMAGES + "yellow_color.png"))
        list.add(TrafficLightItem(v(pos.x, pos.y), PATH_TO_IMAGES + "green_color.png"))
        list.add(TrafficLightItem(v(pos.x, pos.y), PATH_TO_IMAGES + "green_color_flash.png"))
    }

    override fun draw() {
        when (currentColor) {
            "red" -> list.get(0).draw()
            "yellow" -> list.get(1).draw()
            "green" -> list.get(2).draw()
            "green_flash" -> list.get(3).draw()
            else -> {
            }
        }
        timer.draw()
    }

    fun setRed() {
        if (currentColor != "red" && currentColor != "yellow" && currentColor != "green_flash") {
            isForceColorChange = true
            changeColor()
        }
    }

    fun setGreen() {
        if (currentColor != "green" && currentColor != "green_flash" && currentColor != "yellow") {
            isForceColorChange = true
            changeColor()
        }
    }

    fun changeColor() {
        if (shouldChangeColorForward) changeColorForward() else changeColorBackward()
    }

    fun changeColorForward() {
        shouldChangeColorForward = false
        currentColor = "yellow"
        window.setTimeout({
                              if (!isForceColorChange) timer.resetTimer() else isForceColorChange = false
                              currentColor = "green"
                              Unit
                          }, 3000)
    }


    fun changeColorBackward() {
        shouldChangeColorForward = true
        currentColor = "green_flash"
        window.setTimeout({
                              currentColor = "yellow"
                              window.setTimeout({
                                                    if (!isForceColorChange) timer.resetTimer() else isForceColorChange = false
                                                    currentColor = "red"
                                                    Unit
                                                }, 1000)
                          }, 2000)
    }

    fun canMove(): Boolean {
        return (currentColor != "red" && currentColor != "yellow")
    }
}


//One element from Traffic light
class TrafficLightItem(override var pos: Vector, val imageSrc: String) : Shape() {
    val relSize: Double = 0.5
    val imageSize = v(33.0, 33.0)
    var size: Vector = imageSize * relSize

    var isFlashing = (imageSrc == PATH_TO_IMAGES + "green_color_flash.png")
    var isFlashNow = false
    var countOfFlash = 0

    override fun draw() {
        size = imageSize * relSize
        if (isFlashing) {
            if (isFlashNow) {
                if (countOfFlash > 6) {
                    isFlashNow = false
                    countOfFlash = 0
                } else {
                    countOfFlash++
                }
            } else {
                state.context.drawImage(getImage(PATH_TO_IMAGES + "green_color.png"), 0.0, 0.0,
                                        imageSize.x, imageSize.y,
                                        pos.x, pos.y,
                                        size.x, size.y)
                if (countOfFlash > 6) {
                    isFlashNow = true
                    countOfFlash = 0
                } else {
                    countOfFlash++
                }
            }
        } else {
            state.context.drawImage(getImage(imageSrc), 0.0, 0.0,
                                    imageSize.x, imageSize.y,
                                    pos.x, pos.y,
                                    size.x, size.y)
        }
    }
}


class Car(override var pos: Vector, val direction: String, val color: String) : Shape() {
    val imageSize = v(25.0, 59.0)
    fun randomSpeed() = Random.nextDouble(2.0, 10.0)
    var speed = randomSpeed()

    override fun draw() {
        if (direction == "up" || direction == "down") {
            state.context.drawImage(getImage(PATH_TO_IMAGES + color + "_car.png"), 0.0, 0.0,
                                    imageSize.x, imageSize.y,
                                    pos.x, pos.y,
                                    imageSize.x, imageSize.y)
            if ((!isNearStopLine()) || (trafficLightUp.canMove() && isNearStopLine()) ) {
                move()
            } else {
                speed = randomSpeed()
            }
        } else {
            state.context.drawImage(getImage(PATH_TO_IMAGES + color + "_car.png"), 0.0, 0.0,
                                    imageSize.y, imageSize.x,
                                    pos.x, pos.y,
                                    imageSize.y, imageSize.x)
            if ((!isNearStopLine()) || (trafficLightLeft.canMove() && isNearStopLine()) ) {
                move()
            } else {
                speed = randomSpeed()
            }

        }

    }

    fun isNearStopLine(): Boolean {
        when (direction) {
            "up" -> return (pos.y > 198 && pos.y < 208)
            "down" -> return (pos.y > 10 && pos.y < 20)
            "right" -> return (pos.x > -8 && pos.x < 2)
            "left" -> return (pos.x > 243 && pos.x < 253)
            else -> return false
        }

    }

    fun move() {
        var x = pos.x
        var y = pos.y

        when (direction) {
            "up" -> if (pos.y < -50) y = 250.0 else y = pos.y - speed
            "down" -> if (pos.y > 300) y = 0.0 else y = pos.y + speed
            "right" -> if (pos.x > 300) x = -10.0 else x = pos.x + speed
            "left" -> if (pos.x < -50) x = 340.0 else x = pos.x - speed
            else -> {
            }
        }

        pos = v(x, y)
    }
}

class Map(override var pos: Vector) : Shape() {
    val relSize: Double = 0.8
    val imageSize = v(420.0, 323.0)
    var size: Vector = imageSize * relSize

    override fun draw() {
        size = imageSize * relSize
        state.context.drawImage(getImage(PATH_TO_IMAGES + "crossroads.jpg"), 0.0, 0.0,
                                imageSize.x, imageSize.y,
                                pos.x, pos.y,
                                size.x, size.y)
    }
}


class CanvasState(val canvas: HTMLCanvasElement) {
    val context = traffic.context
    var shapes = mutableListOf<Shape>()

    var width = canvas.width
    var height = canvas.height

    val size: Vector
        get() = v(width.toDouble(), height.toDouble())

    fun addShape(shape: Shape) {
        shapes.add(shape)
    }

    init {
        jq(canvas).click {
            val mousePos = mousePos(it)
            shapeLoop@ for (shape in shapes) {
                if (shape is Button && mousePos in shape) {
                    val name = shape.src
                    shape.mouseClick()
                    when (name) {
                        PATH_TO_IMAGES + "lr.png" -> {

                            trafficLightUp.setRed()
                            trafficLightDown.setRed()
                            trafficLightLeft.setGreen()
                            trafficLightRight.setGreen()
                        }
                        PATH_TO_IMAGES + "ud.png" -> {

                            trafficLightLeft.setRed()
                            trafficLightRight.setRed()
                            trafficLightUp.setGreen()
                            trafficLightDown.setGreen()

                        }
                        else -> continue@shapeLoop
                    }

                }
            }
        }

        jq(canvas).mousemove {
            val mousePos = mousePos(it)
            for (shape in shapes) {
                if (shape is Button && mousePos in shape) {
                    shape.mouseOver()
                }
            }
        }

        window.setInterval({
                               draw()
                           }, 1000 / 30)

        window.setInterval({
                               trafficLightUp.changeColor()
                               trafficLightLeft.changeColor()
                               trafficLightRight.changeColor()
                               trafficLightDown.changeColor()
                           }, 10000)


    }


    fun mousePos(e: MouseEvent): Vector {
        var offset = Vector()
        var element: HTMLElement? = canvas
        while (element != null) {
            val el: HTMLElement = element
            offset += Vector(el.offsetLeft.toDouble(), el.offsetTop.toDouble())
            element = el.offsetParent as HTMLElement?
        }
        return Vector(e.pageX, e.pageY) - offset
    }

    fun draw() {
        clear()
        for (shape in shapes) {
            shape.draw()
        }
    }

    fun clear() {
        context.fillStyle = Colors.white
        context.fillRect(0.0, 0.0, width.toDouble(), height.toDouble())
    }
}

abstract class Shape() {
    abstract var pos: Vector
    abstract fun draw()

    // a couple of helper extension methods we'll be using in the derived classes
    fun CanvasRenderingContext2D.shadowed(shadowOffset: Vector, alpha: Double, render: CanvasRenderingContext2D.() -> Unit) {
        save()
        shadowColor = "rgba(100, 100, 100, $alpha)"
        shadowBlur = 5.0
        shadowOffsetX = shadowOffset.x
        shadowOffsetY = shadowOffset.y
        render()
        restore()
    }

    fun CanvasRenderingContext2D.fillPath(constructPath: CanvasRenderingContext2D.() -> Unit) {
        beginPath()
        constructPath()
        closePath()
        fill()
    }

}

class Vector(val x: Double = 0.0, val y: Double = 0.0) {
    operator fun plus(v: Vector) = v(x + v.x, y + v.y)
    operator fun minus(v: Vector) = v(x - v.x, y - v.y)
    operator fun times(koef: Double) = v(x * koef, y * koef)
    fun distanceTo(v: Vector) = sqrt((this - v).sqr)
    fun rotatedBy(theta: Double): Vector {
        val sin = sin(theta)
        val cos = cos(theta)
        return v(x * cos - y * sin, x * sin + y * cos)
    }

    fun isInRect(topLeft: Vector, size: Vector) = (x >= topLeft.x) && (x <= topLeft.x + size.x) &&
            (y >= topLeft.y) && (y <= topLeft.y + size.y)

    val sqr: Double
        get() = x * x + y * y
    val normalized: Vector
        get() = this * (1.0 / sqrt(sqr))
}

