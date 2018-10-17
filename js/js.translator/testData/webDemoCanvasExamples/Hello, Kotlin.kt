/*
  This shows simple text floating around.
*/
package hello

import jquery.*
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import kotlin.browser.document
import kotlin.browser.window
import kotlin.random.*
import kotlin.math.*

val canvas: HTMLCanvasElement
    get() {
        return window.document.getElementsByTagName("canvas").item(0)!! as HTMLCanvasElement
    }

val context: CanvasRenderingContext2D
    get() {
        return canvas.getContext("2d") as CanvasRenderingContext2D
    }


val width: Int
    get() {
        return canvas.width
    }

val height: Int
    get() {
        return canvas.height
    }


// class representing a floating text
class HelloKotlin() {
    var relX = Random.nextDouble(0.2, 0.4)
    var relY = Random.nextDouble(0.4, 0.6)

    val absX: Double
        get() = (relX * width)
    val absY: Double
        get() = (relY * height)

    var relXVelocity = randomVelocity()
    var relYVelocity = randomVelocity()


    val message = "Hello, Kotlin!"
    val textHeightInPixels = 20
    init {
        context.font = "bold ${textHeightInPixels}px Georgia, serif"
    }
    val textWidthInPixels = context.measureText(message).width

    fun draw() {
        context.save()
        move()
        // if you using chrome chances are good you wont see the shadow
        context.shadowColor = "#000000"
        context.shadowBlur = 5.0
        context.shadowOffsetX = -4.0
        context.shadowOffsetY = 4.0
        context.fillStyle = "rgb(242,160,110)"
        context.fillText(message, absX, absY)
        context.restore()
    }

    fun move() {
        val relTextWidth = textWidthInPixels / width
        if (relX > (1.0 - relTextWidth - relXVelocity.absoluteValue) || relX < relXVelocity.absoluteValue) {
            relXVelocity *= -1
        }
        val relTextHeight = textHeightInPixels / height
        if (relY > (1.0 - relYVelocity.absoluteValue) || relY < relYVelocity.absoluteValue + relTextHeight) {
            relYVelocity *= -1
        }
        relX += relXVelocity
        relY += relYVelocity
    }

    fun randomVelocity() = Random.nextDouble(-0.03, 0.03) // same as 0.03 * Math.random() * (if (Math.random() < 0.5) 1 else -1)

}

fun renderBackground() {
    context.save()
    context.fillStyle = "#5C7EED"
    context.fillRect(0.0, 0.0, width.toDouble(), height.toDouble())
    context.restore()
}

fun main(args: Array<String>) {
    jq {
        val interval = 50
        // we pass a literal that constructs a new HelloKotlin object
        val logos = Array(3) {
            HelloKotlin()
        }

        window.setInterval({
                               renderBackground()
                               for (logo in logos) {
                                   logo.draw()
                               }
                           }, interval)
    }
}


