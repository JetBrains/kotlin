package html5

import js.native
import js.DomElement

native
class Context() {
    fun save() : Unit = js.noImpl

    fun restore() : Unit = js.noImpl


    fun scale(x : Double, y : Double) : Unit = js.noImpl

    fun rotate(angle : Double) : Unit = js.noImpl

    fun translate(x : Double, y : Double) : Unit = js.noImpl


    fun clearRect(x : Double, y : Double, w : Double, h : Double) : Unit = js.noImpl

    fun fillRect(x : Double, y : Double, w : Double, h : Double) : Unit = js.noImpl

    fun strokeRect(x : Double, y : Double, w : Double, h : Double) : Unit = js.noImpl


    var globalAlpha : Double = 1.0;
    var strokeStyle : Any = ""
    var fillStyle : Any = ""
    var lineWidth : Double = 1.0
    var shadowOffsetX : Double = 0.0
    var shadowOffsetY : Double = 0.0
    var shadowBlur : Double = 0.0
    var shadowColor : String = ""
    var font : String = ""

    fun beginPath() : Unit = js.noImpl

    fun moveTo(x : Double, y : Double) : Unit = js.noImpl

    fun closePath() : Unit = js.noImpl


    fun lineTo(x : Double, y : Double) : Unit = js.noImpl

    fun quadraticCurveTo(cpx : Double, cpy : Double, x : Double, y : Double) : Unit = js.noImpl

    fun bezierCurveTo(cp1x : Double, cp1y : Double, cp2x : Double, cp2y : Double, x : Double, y : Double) : Unit = js.noImpl

    fun arcTo(x1 : Double, y1 : Double, x2 : Double, y2 : Double, radius : Double) : Unit = js.noImpl

    fun arc(x : Double, y : Double, radius : Double, startAngle : Double, endAngle : Double, anticlockwise : Boolean) : Unit = js.noImpl


    fun rect(x : Double, y : Double, w : Double, h : Double) : Unit = js.noImpl

    fun fill() : Unit = js.noImpl

    fun stroke() : Unit = js.noImpl


    fun fillText(text : String, x : Double, y : Double) : Unit = js.noImpl

    fun fillText(text : String, x : Double, y : Double, maxWidth : Double) : Unit = js.noImpl

    fun strokeText(text : String, x : Double, y : Double) : Unit = js.noImpl

    fun strokeText(text : String, x : Double, y : Double, maxWidth : Double) : Unit = js.noImpl


    fun measureText(text : String) : TextMetrics = TextMetrics();

    fun drawImage(image : HTMLImageElement, dx : Int, dy : Int) : Unit = js.noImpl
    fun drawImage(image : HTMLImageElement, dx : Int, dy : Int, dw : Int, dh : Int) : Unit = js.noImpl
    fun drawImage(image : HTMLImageElement, sx : Int, sy : Int,
            sw : Int, sh : Int, dx : Int, dy : Int, dw : Int, dh : Int) : Unit = js.noImpl


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
    fun addColorStop(offset : Double, color : String) : Unit = js.noImpl
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
    val width : Int = js.noImpl
}


/*custom helpers*/
native
fun getContext() : Context = js.noImpl
native
fun getCanvas() : Canvas = js.noImpl
native
fun getKotlinLogo() : HTMLImageElement = js.noImpl
native
fun getImage(src : String) : HTMLImageElement = js.noImpl