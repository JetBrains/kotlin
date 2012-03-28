package html5

import js.native
import js.DomElement

native
class Context() {
    fun save() {
    }
    fun restore() {
    }

    fun scale(x : Double, y : Double) {
    }
    fun rotate(angle : Double) {
    }
    fun translate(x : Double, y : Double) {
    }

    fun clearRect(x : Double, y : Double, w : Double, h : Double) {
    }
    fun fillRect(x : Double, y : Double, w : Double, h : Double) {
    }
    fun strokeRect(x : Double, y : Double, w : Double, h : Double) {
    }

    var globalAlpha : Double = 1.0;
    var strokeStyle : Any = ""
    var fillStyle : Any = ""
    var lineWidth : Double = 1.0
    var shadowOffsetX : Double = 0.0
    var shadowOffsetY : Double = 0.0
    var shadowBlur : Double = 0.0
    var shadowColor : String = ""
    var font : String = ""

    fun beginPath() {
    }
    fun moveTo(x : Double, y : Double) {
    }
    fun closePath() {
    }

    fun lineTo(x : Double, y : Double) {
    }
    fun quadraticCurveTo(cpx : Double, cpy : Double, x : Double, y : Double) {
    }
    fun bezierCurveTo(cp1x : Double, cp1y : Double, cp2x : Double, cp2y : Double, x : Double, y : Double) {
    }
    fun arcTo(x1 : Double, y1 : Double, x2 : Double, y2 : Double, radius : Double) {
    }
    fun arc(x : Double, y : Double, radius : Double, startAngle : Double, endAngle : Double, anticlockwise : Boolean) {
    }

    fun rect(x : Double, y : Double, w : Double, h : Double) {
    }
    fun fill() {
    }
    fun stroke() {
    }

    fun fillText(text : String, x : Double, y : Double) {
    }
    fun fillText(text : String, x : Double, y : Double, maxWidth : Double) {
    }
    fun strokeText(text : String, x : Double, y : Double) {
    }
    fun strokeText(text : String, x : Double, y : Double, maxWidth : Double) {
    }

    fun measureText(text : String)  : TextMetrics = TextMetrics();

    fun drawImage(image : HTMLImageElement, dx : Double, dy : Double) {
    }
    fun drawImage(image : HTMLImageElement, dx: Double, dy: Double, dw: Double, dh: Double) {
    }
    fun drawImage(image : HTMLImageElement, sx: Double, sy: Double,
            sw: Double, sh: Double, dx: Double, dy: Double, dw: Double, dh: Double) {
    }

    fun createLinearGradient(x0 : Double, y0 : Double, x1 : Double, y1 : Double) : CanvasGradient = CanvasGradient()
    fun createRadialGradient(x0 : Double, y0 : Double, r0 : Double, x1 : Double, y1 : Double, r1 : Double) : CanvasGradient = CanvasGradient();

    fun getImageData(sx : Int, sy : Int, sw : Int, sh : Int) : ImageData = js.noImpl
    fun putImageData(data : ImageData, dx : Int, dy : Int) : Unit = js.noImpl
}

native
open class HTMLImageElement() : DomElement() {
}

native
class CanvasGradient() {
    fun addColorStop(offset : Double, color : String) {
    }
}

native
class ImageData() {
    //    readonly attribute unsigned long width;
    //    readonly attribute unsigned long height;
    //    readonly attribute Uint8ClampedArray data;
    val width : Int = js.noImpl
    val height : Int = js.noImpl
    val data : Array<Int> = js.noImpl
}


native
class Canvas() : DomElement() {
    var width : Int = js.noImpl;
    var height : Int = js.noImpl;

    //DOMString toDataURL(in optional DOMString type, in any... args);
    fun toDataURL() : String = js.noImpl
    fun toDataURL(typ : String) : String = js.noImpl
}

native
class TextMetrics() {
    val width : Int = 0
}

/*custom helpers*/
native
fun getContext() : Context = Context();
native
fun getCanvas() : Canvas = Canvas();
native
fun getKotlinLogo() : HTMLImageElement = HTMLImageElement();
native
fun getImage(src: String) : HTMLImageElement = HTMLImageElement();