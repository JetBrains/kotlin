/*
    In this example strange creatures are watching the kotlin logo. You can drag'n'drop them as well as the logo.
    Doubleclick to add more creatures but be careful. They may be watching you!
*/
package creatures

import js.dom.html5.CanvasContext
import js.jquery.*
import js.dom.html5.CanvasGradient
import js.dom.html5.HTMLCanvasElement
import java.util.ArrayList
import js.dom.html.window
import js.dom.html.HTMLElement
import js.dom.html.HTMLImageElement


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

abstract class Shape() {

    abstract fun draw(state: CanvasState)
    // these two abstract methods defines that our shapes can be dragged
    abstract fun contains(mousePos: Vector): Boolean
    abstract var pos: Vector

    var selected: Boolean = false

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

val Kotlin = Logo(v(250.0, 75.0))

class Logo(override var pos: Vector) : Shape() {
    val relSize: Double = 0.25
    val shadowOffset = v(-3.0, 3.0)
    val imageSize = v(377.0, 393.0)
    var size: Vector = imageSize * relSize
    // get-only properties like this saves you lots of typing and are very expressive
    val position: Vector
        get() = if (selected) pos - shadowOffset else pos


    fun drawLogo(state: CanvasState) {
        size = imageSize * (state.size.x / imageSize.x) * relSize
        // getKotlinLogo() is a 'magic' function here defined only for purposes of demonstration but in fact it just find an element containing the logo
        state.context.drawImage(getImage("http://kotlin-demo.jetbrains.com/static/images/kotlinlogowobackground.png"), 0, 0,
                                imageSize.x.toInt(), imageSize.y.toInt(),
                                position.x.toInt(), position.y.toInt(),
                                size.x.toInt(), size.y.toInt())
    }

    override fun draw(state: CanvasState) {
        val context = state.context
        if (selected) {
            // using helper we defined in Shape class
            context.shadowed(shadowOffset, 0.2) {
                drawLogo(state)
            }
        }
        else {
            drawLogo(state)
        }
    }

    override fun contains(mousePos: Vector): Boolean = mousePos.isInRect(pos, size)

    val centre: Vector
        get() = pos + size * 0.5
}

val gradientGenerator: RadialGradientGenerator? = null
    get() {
        if ($gradientGenerator == null) {
            $gradientGenerator = RadialGradientGenerator(context)
        }
        return $gradientGenerator
    }

class Creature(override var pos: Vector, val state: CanvasState) : Shape() {

    val shadowOffset = v(-5.0, 5.0)
    val colorStops = gradientGenerator!!.getNext()
    val relSize = 0.05
    // these properties have no backing fields and in java/javascript they could be represented as little helper functions
    val radius: Double
        get() = state.width * relSize
    val position: Vector
        get() = if (selected) pos - shadowOffset else pos
    val directionToLogo: Vector
        get() = (Kotlin.centre - position).normalized

    //notice how the infix call can make some expressions extremely expressive
    override fun contains(mousePos: Vector) = pos distanceTo mousePos < radius

    // defining more nice extension functions
    fun CanvasContext.circlePath(position: Vector, rad: Double) {
        arc(position.x, position.y, rad, 0.0, 2 * Math.PI, false)
    }

    //notice we can use an extension function we just defined inside another extension function
    fun CanvasContext.fillCircle(position: Vector, rad: Double) {
        fillPath {
            circlePath(position, rad)
        }
    }

    override fun draw(state: CanvasState) {
        val context = state.context
        if (!selected) {
            drawCreature(context)
        }
        else {
            drawCreatureWithShadow(context)
        }
    }

    fun drawCreature(context: CanvasContext) {
        context.fillStyle = getGradient(context)
        context.fillPath {
            tailPath(context)
            circlePath(position, radius)
        }
        drawEye(context)
    }

    fun getGradient(context: CanvasContext): CanvasGradient {
        val gradientCentre = position + directionToLogo * (radius / 4)
        val gradient = context.createRadialGradient(gradientCentre.x, gradientCentre.y, 1.0, gradientCentre.x, gradientCentre.y, 2 * radius)!!
        for (colorStop in colorStops) {
            gradient.addColorStop(colorStop.first, colorStop.second)
        }
        return gradient
    }

    fun tailPath(context: CanvasContext) {
        val tailDirection = -directionToLogo
        val tailPos = position + tailDirection * radius * 1.0
        val tailSize = radius * 1.6
        val angle = Math.PI / 6.0
        val p1 = tailPos + tailDirection.rotatedBy(angle) * tailSize
        val p2 = tailPos + tailDirection.rotatedBy(-angle) * tailSize
        val middlePoint = position + tailDirection * radius * 1.0
        context.moveTo(tailPos.x.toInt(), tailPos.y.toInt())
        context.lineTo(p1.x.toInt(), p1.y.toInt())
        context.quadraticCurveTo(middlePoint.x, middlePoint.y, p2.x, p2.y)
        context.lineTo(tailPos.x.toInt(), tailPos.y.toInt())
    }

    fun drawEye(context: CanvasContext) {
        val eyePos = directionToLogo * radius * 0.6 + position
        val eyeRadius = radius / 3
        val eyeLidRadius = eyeRadius / 2
        context.fillStyle = "#FFFFFF"
        context.fillCircle(eyePos, eyeRadius)
        context.fillStyle = "#000000"
        context.fillCircle(eyePos, eyeLidRadius)
    }

    fun drawCreatureWithShadow(context: CanvasContext) {
        context.shadowed(shadowOffset, 0.7) {
            context.fillStyle = getGradient(context)
            fillPath {
                tailPath(context)
                context.circlePath(position, radius)
            }
        }
        drawEye(context)
    }
}

class CanvasState(val canvas: HTMLCanvasElement) {
    var width = canvas.width
    var height = canvas.height
    val size: Vector
        get() = v(width, height)
    val context = creatures.context
    var valid = false
    var shapes = ArrayList<Shape>()
    var selection: Shape? = null
    var dragOff = Vector()
    val interval = 1000 / 30

    {
        jq(canvas).mousedown {
            valid = false
            selection = null
            val mousePos = mousePos(it)
            for (shape in shapes) {
                if (mousePos in shape) {
                    dragOff = mousePos - shape.pos
                    shape.selected = true
                    selection = shape
                    break
                }
            }
        }

        jq(canvas).mousemove {
            if (selection != null) {
                selection!!.pos = mousePos(it) - dragOff
                valid = false
            }
        }

        jq(canvas).mouseup {
            if (selection != null) {
                selection!!.selected = false
            }
            selection = null
            valid = false
        }

        jq(canvas).dblclick {
            val newCreature = Creature(mousePos(it), this @CanvasState)
            addShape(newCreature)
            valid = false
        }

        window.setInterval({
                               draw()
                           }, interval)
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

    fun addShape(shape: Shape) {
        shapes.add(shape)
        valid = false
    }

    fun clear() {
        context.fillStyle = "#FFFFFF"
        context.fillRect(0, 0, width.toInt(), height.toInt())
        context.strokeStyle = "#000000"
        context.lineWidth = 4.0
        context.strokeRect(0, 0, width.toInt(), height.toInt())
    }

    fun draw() {
        if (valid) return

        clear()
        for (shape in shapes.reversed()) {
            shape.draw(this)
        }
        Kotlin.draw(this)
        valid = true
    }
}

class RadialGradientGenerator(val context: CanvasContext) {
    val gradients = ArrayList<Array<Pair<Double, String>>>()
    var current = 0

    fun newColorStops(vararg colorStops: Pair<Double, String>) {
        gradients.add(colorStops)
    }

    {
        newColorStops(Pair(0.0, "#F59898"), Pair(0.5, "#F57373"), Pair(1.0, "#DB6B6B"))
        newColorStops(Pair(0.39, "rgb(140,167,209)"), Pair(0.7, "rgb(104,139,209)"), Pair(0.85, "rgb(67,122,217)"))
        newColorStops(Pair(0.0, "rgb(255,222,255)"), Pair(0.5, "rgb(255,185,222)"), Pair(1.0, "rgb(230,154,185)"))
        newColorStops(Pair(0.0, "rgb(255,209,114)"), Pair(0.5, "rgb(255,174,81)"), Pair(1.0, "rgb(241,145,54)"))
        newColorStops(Pair(0.0, "rgb(132,240,135)"), Pair(0.5, "rgb(91,240,96)"), Pair(1.0, "rgb(27,245,41)"))
        newColorStops(Pair(0.0, "rgb(250,147,250)"), Pair(0.5, "rgb(255,80,255)"), Pair(1.0, "rgb(250,0,217)"))
    }

    fun getNext(): Array<Pair<Double, String>> {
        val result = gradients.get(current)
        current = (current + 1) % gradients.size()
        return result
    }
}

fun v(x: Double, y: Double) = Vector(x, y)

class Vector(val x: Double = 0.0, val y: Double = 0.0) {
    fun plus(v: Vector) = v(x + v.x, y + v.y)
    fun minus() = v(-x, -y)
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

fun main(args: Array<String>) {
    jq {
        val state = CanvasState(canvas)
        state.addShape(Kotlin)
        state.addShape(Creature(state.size * 0.25, state))
        state.addShape(Creature(state.size * 0.75, state))
        window.setTimeout({
                              state.valid = false
                          }, 1000)
    }
}

fun <T> List<T>.reversed(): List<T> {
    val result = ArrayList<T>()
    var i = size()
    while (i > 0) {
        result.add(get(--i))
    }
    return result
}