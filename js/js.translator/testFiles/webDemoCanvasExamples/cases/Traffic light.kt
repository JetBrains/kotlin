package traffic

import java.util.ArrayList
import js.dom.html5.CanvasContext
import js.dom.html5.HTMLCanvasElement

import js.dom.html.HTMLImageElement
import js.dom.html.window
import js.jquery.*
import js.dom.html.HTMLElement

fun getImage(path: String): HTMLImageElement {
    val image = window.document.createElement("img") as HTMLImageElement
    image.src = path
    return image
}

val canvas: HTMLCanvasElement
    get() {
        return window.document.getElementsByTagName("canvas").item(0)!! as HTMLCanvasElement
    }

val context: CanvasContext
    get() {
        return canvas.getContext("2d")!!
    }


val PATH_TO_IMAGES = "http://kotlin-demo.jetbrains.com/static/images/canvas/"



var _state: CanvasState? = null
val state: CanvasState
    get() {
        if (_state == null) {
            _state = CanvasState(canvas)
        }
        return _state!!
    }

val colors: Colors
    get() {
        return Colors()
    }

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

class Image(val src: String, override var pos: Vector, var imageSize: Vector): Shape() {
    override fun draw() {
        state.context.drawImage(getImage(src), 0, 0,
                imageSize.x.toInt(), imageSize.y.toInt(),
                pos.x.toInt(), pos.y.toInt(),
                imageSize.x.toInt(), imageSize.y.toInt())
    }

    fun contains(mousePos: Vector): Boolean = mousePos.isInRect(pos, imageSize)
}

class Button(val src: String, override var pos: Vector, var imageSize: Vector): Shape() {
    var isMouseOver = false
    var isMouseDown = false

    override fun draw() {
        if (isMouseOver) {
            state.context.shadowed(v(- 3.0, 3.0), 1.2) {
                state.context.drawImage(getImage(src), 0, 0,
                        imageSize.x.toInt(), imageSize.y.toInt(),
                        pos.x.toInt(), pos.y.toInt(),
                        imageSize.x.toInt(), imageSize.y.toInt())
            }
        } else if (isMouseDown) {
            state.context.shadowed(v(- 3.0, 3.0), 0.8) {
                state.context.drawImage(getImage(src), 0, 0,
                        imageSize.x.toInt(), imageSize.y.toInt(),
                        pos.x.toInt(), pos.y.toInt(),
                        imageSize.x.toInt(), imageSize.y.toInt())
            }
        } else {
            state.context.drawImage(getImage(src), 0, 0,
                    imageSize.x.toInt(), imageSize.y.toInt(),
                    pos.x.toInt(), pos.y.toInt(),
                    imageSize.x.toInt(), imageSize.y.toInt())
        }
    }

    fun mouseClick() {
        isMouseDown = true
        window.setTimeout({
            isMouseDown = false
        }, 1000)
    }

    fun mouseOver() {
        isMouseOver = true
        window.setTimeout({
            isMouseOver = false
        }, 1000)
    }


    fun contains(mousePos: Vector): Boolean = mousePos.isInRect(pos, imageSize)
}

class Border(): Shape() {
    override var pos = Vector(4.0, 4.0);

    override fun draw() {
        state.context.fillStyle = colors.white
        state.context.fillRect(2, 4, 10, 292)
        state.context.fillRect(330, 4, 370, 292)
        state.context.fillRect(2, 2, 330, 10)
        state.context.fillRect(4, 265, 340, 380)
        state.context.strokeStyle = colors.black
        state.context.lineWidth = 4.0
        state.context.strokeRect(0, 0, state.width.toInt(), state.height.toInt())
    }

}

class Timer(override var pos: Vector): Shape() {
    var timeLeftForChangeColor: Char = 'c'
    var timeStartLastChangeColor = Date().getTime();
    var timerLength = 13

    override fun draw() {
        timeLeftForChangeColor = ("" + (timerLength - (Date().getTime() - timeStartLastChangeColor) / 1000)).get(0)
        state.context.font = "bold 9px Arial, serif"
        state.context.fillStyle = colors.black
        state.context.fillText("" + timeLeftForChangeColor, pos.x.toInt(), pos.y.toInt())
    }

    fun resetTimer() {
        timeStartLastChangeColor = Date().getTime()
        timerLength = 10
    }
}

//Colors constants
class Colors() {
    val black: String = "#000000"
    val white = "#FFFFFF"
    val grey = "#C0C0C0"
    val red = "#EF4137"
    val yellow = "#FCE013"
    val green = "#0E9648"
}

class TrafficLight(override var pos: Vector, val direction: String, val startColor: String): Shape() {
    val list = ArrayList<TrafficLightItem>()
    var size = Vector(27.0, 34.0);
    var timer = Timer(Vector(pos.x + 6, pos.y + 12))
    var currentColor = startColor;
    var isForceColorChange = false
    var changeColorForward = (startColor == "red")

    {
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
        if (changeColorForward) changeColorForward() else changeColorBackward()
    }

    fun changeColorForward() {
        changeColorForward = false
        currentColor = "yellow"
        window.setTimeout({
            if (!isForceColorChange) timer.resetTimer() else isForceColorChange = false
            currentColor = "green"
        }, 3000)
    }


    fun changeColorBackward() {
        changeColorForward = true
        currentColor = "green_flash"
        window.setTimeout({
            currentColor = "yellow"
            window.setTimeout({
                if (!isForceColorChange) timer.resetTimer() else isForceColorChange = false
                currentColor = "red"
            }, 1000)
        }, 2000)
    }

    fun canMove(): Boolean {
        return (currentColor != "red" && currentColor != "yellow")
    }
}


//One element from Traffic light
class TrafficLightItem(override var pos: Vector, val imageSrc: String): Shape() {
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
                state.context.drawImage(getImage(PATH_TO_IMAGES + "green_color.png"), 0, 0,
                        imageSize.x.toInt(), imageSize.y.toInt(),
                        pos.x.toInt(), pos.y.toInt(),
                        size.x.toInt(), size.y.toInt())
                if (countOfFlash > 6) {
                    isFlashNow = true
                    countOfFlash = 0
                } else {
                    countOfFlash++
                }
            }
        } else {
            state.context.drawImage(getImage(imageSrc), 0, 0,
                    imageSize.x.toInt(), imageSize.y.toInt(),
                    pos.x.toInt(), pos.y.toInt(),
                    size.x.toInt(), size.y.toInt())
        }
    }
}


class Car(override var pos: Vector, val direction: String, val color: String): Shape() {
    val imageSize = v(25.0, 59.0)
    var speed = getRandomArbitary(2, 10);

    override fun draw() {
        if (direction == "up" || direction == "down") {
            state.context.drawImage(getImage(PATH_TO_IMAGES + color + "_car.png"), 0, 0,
                    imageSize.x.toInt(), imageSize.y.toInt(),
                    pos.x.toInt(), pos.y.toInt(),
                    imageSize.x.toInt(), imageSize.y.toInt())
            if ((!isNearStopLine()) || (trafficLightUp.canMove() && isNearStopLine()) ) {
                move()
            } else {
                speed = getRandomArbitary(2, 10)
            }
        } else {
            state.context.drawImage(getImage(PATH_TO_IMAGES + color + "_car.png"), 0, 0,
                    imageSize.y.toInt(), imageSize.x.toInt(),
                    pos.x.toInt(), pos.y.toInt(),
                    imageSize.y.toInt(), imageSize.x.toInt())
            if ((!isNearStopLine()) || (trafficLightLeft.canMove() && isNearStopLine()) ) {
                move()
            } else {
                speed = getRandomArbitary(2, 10)
            }

        }

    }

    fun isNearStopLine(): Boolean {
        when (direction) {
            "up" ->  return (pos.y > 198 && pos.y < 208)
            "down" -> return (pos.y > 10 && pos.y < 20)
            "right" -> return (pos.x > - 8 && pos.x < 2)
            "left" -> return (pos.x > 243 && pos.x < 253)
            else -> return false
        }

    }

    fun move() {
        var x = pos.x
        var y = pos.y

        when (direction) {
            "up" -> if (pos.y < - 50) y = 250.0 else y = pos.y - speed
            "down" -> if (pos.y > 300) y = 0.0 else y = pos.y + speed
            "right" -> if (pos.x > 300) x = - 10.0 else x = pos.x + speed
            "left" -> if (pos.x < - 50) x = 340.0 else x = pos.x - speed
            else -> {
            }
        }

        pos = v(x, y)
    }
}

class Map(override var pos: Vector): Shape() {
    val relSize: Double = 0.8
    val imageSize = v(420.0, 323.0)
    var size: Vector = imageSize * relSize

    override fun draw() {
        size = imageSize * relSize
        state.context.drawImage(getImage(PATH_TO_IMAGES + "crossroads.jpg"), 0, 0,
                imageSize.x.toInt(), imageSize.y.toInt(),
                pos.x.toInt(), pos.y.toInt(),
                size.x.toInt(), size.y.toInt())
    }
}


class CanvasState(val canvas: HTMLCanvasElement) {
    val context = traffic.context
    var shapes = ArrayList<Shape>()

    var width = canvas.width
    var height = canvas.height

    val size: Vector
        get() = v(width, height)

    fun addShape(shape: Shape) {
        shapes.add(shape)
    }

    {
        jq(canvas).click {
            val mousePos = mousePos(it)
            for (shape in shapes) {
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
                        else -> continue
                    }

                }
            }
        }

        jq(canvas).mousemove {
            val mousePos = mousePos(it)
            for (shape in shapes) {
                if (shape is Button && mousePos in shape) {
                    val name = shape.src
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
            val el: HTMLElement = element!!
            offset += Vector(el.offsetLeft, el.offsetTop)
            element = el.offsetParent
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
        context.fillStyle = colors.white
        context.fillRect(0, 0, width.toInt(), height.toInt())
    }
}

abstract class Shape() {
    abstract var pos: Vector
    abstract fun draw()

    // a couple of helper extension methods we'll be using in the derived classes
    fun CanvasContext.shadowed(shadowOffset: Vector, alpha: Double, render: CanvasContext.() -> Unit) {
        save()
        shadowColor = "rgba(100, 100, 100, $alpha)"
        shadowBlur = 5.0
        shadowOffsetX = shadowOffset.x
        shadowOffsetY = shadowOffset.y
        render()
        restore()
    }

    fun CanvasContext.fillPath(constructPath: CanvasContext.() -> Unit) {
        beginPath()
        constructPath()
        closePath()
        fill()
    }

}

class Vector(val x: Double = 0.0, val y: Double = 0.0) {
    fun plus(v: Vector) = v(x + v.x, y + v.y)
    fun minus() = v(- x, - y)
    fun minus(v: Vector) = v(x - v.x, y - v.y)
    fun times(koef: Double) = v(x * koef, y * koef)
    fun distanceTo(v: Vector) = Math.sqrt((this - v).sqr)
    fun rotatedBy(theta: Double): Vector {
        val sin = Math.sin(theta)
        val cos = Math.cos(theta)
        return v(x * cos - y * sin, x * sin + y * cos)
    }

    fun isInRect(topLeft: Vector, size: Vector) = (x >= topLeft.x) && (x <= topLeft.x + size.x) &&
    (y >= topLeft.y) && (y <= topLeft.y + size.y)

    val sqr: Double
        get() = x * x + y * y
    val normalized: Vector
        get() = this * (1.0 / Math.sqrt(sqr))
}

fun getRandomArbitary(min: Int, max: Int): Double {
    return Math.random() * (max - min) + min;
}
