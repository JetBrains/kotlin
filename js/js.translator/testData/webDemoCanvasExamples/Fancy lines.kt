/*
This example is based on example from html5 canvas2D docs:
  http://www.w3.org/TR/2dcontext/
Note that only a subset of the api is supported for now.
*/

package fancylines

import jquery.*
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import kotlin.browser.document
import kotlin.browser.window
import kotlin.random.*

fun main(args: Array<String>) {
    jq {
        FancyLines().run()
    }
}

val canvas: HTMLCanvasElement
    get() {
        return window.document.getElementsByTagName("canvas").item(0) as HTMLCanvasElement
    }

class FancyLines() {
    val context = canvas.getContext("2d") as CanvasRenderingContext2D
    val height = canvas.height.toDouble()
    val width = canvas.width.toDouble()
    fun nextX() = Random.nextDouble(width)
    fun nextY() = Random.nextDouble(height)
    var x = nextX()
    var y = nextY()
    var hue = 0

    fun line() {
        context.save();

        context.beginPath();

        context.lineWidth = Random.nextDouble(20.0)
        context.moveTo(x, y);

        x = nextX()
        y = nextY()

        context.bezierCurveTo(nextX(), nextY(),
                              nextX(), nextY(), x, y)

        hue += Random.nextInt(10)

        context.strokeStyle = "hsl($hue, 50%, 50%)";

        context.shadowColor = "white";
        context.shadowBlur = 10.0;

        context.stroke();

        context.restore();
    }

    fun blank() {
        context.fillStyle = "rgba(255,255,1,0.1)";
        context.fillRect(0.0, 0.0, width, height);
    }

    fun run() {
        window.setInterval({ line() }, 40);
        window.setInterval({ blank() }, 100);
    }
}
