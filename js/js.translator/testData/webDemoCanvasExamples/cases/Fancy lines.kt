/*
This example is based on example from html5 canvas2D docs:
  http://www.w3.org/TR/2dcontext/
Note that only a subset of the api is supported for now.
*/

package fancylines

import js.dom.html5.*
import js.dom.html.window
import js.jquery.*

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
    val context = canvas.getContext("2d")!!
    val height = canvas.height
    val width = canvas.width
    var x = width * Math.random()
    var y = height * Math.random()
    var hue = 0;

    fun line() {
        context.save();

        context.beginPath();

        context.lineWidth = 20.0 * Math.random();
        context.moveTo(x.toInt(), y.toInt());

        x = width * Math.random();
        y = height * Math.random();

        context.bezierCurveTo(width * Math.random(), height * Math.random(),
                              width * Math.random(), height * Math.random(), x, y);

        hue += (Math.random() * 10).toInt();

        context.strokeStyle = "hsl($hue, 50%, 50%)";

        context.shadowColor = "white";
        context.shadowBlur = 10.0;

        context.stroke();

        context.restore();
    }

    fun blank() {
        context.fillStyle = "rgba(255,255,1,0.1)";
        context.fillRect(0, 0, width, height);
    }

    fun run() {
        window.setInterval({ line() }, 40);
        window.setInterval({ blank() }, 100);
    }
}
