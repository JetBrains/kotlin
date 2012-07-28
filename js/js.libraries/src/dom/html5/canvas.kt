package js.dom.html5

import js.native
import js.noImpl
import js.dom.html.HTMLElement
import js.dom.core.Element

public native trait HTMLCanvasElement : HTMLElement {
    public native var width: Double = js.noImpl
    public native var height: Double = js.noImpl
    public native fun getContext(context: String): CanvasContext? = js.noImpl
    public native fun toDataURL(string: String, args: Any): String = js.noImpl
    public native fun toDataURL(): String = js.noImpl
    public native fun toDataURL(string: String): String = js.noImpl
}

public native trait CanvasContext {
    public native var canvas: HTMLCanvasElement = js.noImpl
    public native fun save(): Unit = js.noImpl
    public native fun restore(): Unit = js.noImpl
    public native fun scale(x: Number, y: Number): Unit = js.noImpl
    public native fun rotate(angle: Number): Unit = js.noImpl
    public native fun translate(x: Number, y: Number): Unit = js.noImpl
    public native fun transform(m11: Number, m12: Number, m21: Number, m22: Number, dx: Number, dy: Number): Unit = js.noImpl
    public native fun setTransform(m11: Number, m12: Number, m21: Number, m22: Number, dx: Number, dy: Number): Unit = js.noImpl
    public native var globalAlpha: Double = js.noImpl
    public native var globalCompositeOperation: String = js.noImpl
    public native var fillStyle: Any = js.noImpl
    public native var strokeStyle: Any = js.noImpl
    public native fun createLinearGradient(x0: Number, y0: Number, x1: Number, y1: Number): CanvasGradient? = js.noImpl
    public native fun createRadialGradient(x0: Number, y0: Number, r0: Number, x1: Number, y1: Number, r1: Number): CanvasGradient? = js.noImpl
    public native var lineWidth: Double = js.noImpl
    public native var lineCap: String = js.noImpl
    public native var lineJoin: String = js.noImpl
    public native var miterLimit: Double = js.noImpl
    public native var shadowOffsetX: Double = js.noImpl
    public native var shadowOffsetY: Double = js.noImpl
    public native var shadowBlur: Double = js.noImpl
    public native var shadowColor: String = js.noImpl
    public native fun clearRect(x: Number, y: Number, w: Number, h: Number): Unit = js.noImpl
    public native fun fillRect(x: Number, y: Number, w: Number, h: Number): Unit = js.noImpl
    public native fun strokeRect(x: Number, y: Number, w: Number, h: Number): Unit = js.noImpl
    public native fun beginPath(): Unit = js.noImpl
    public native fun closePath(): Unit = js.noImpl
    public native fun moveTo(x: Number, y: Number): Unit = js.noImpl
    public native fun lineTo(x: Number, y: Number): Unit = js.noImpl
    public native fun quadraticCurveTo(cpx: Number, cpy: Number, x: Number, y: Number): Unit = js.noImpl
    public native fun bezierCurveTo(cp1x: Number, cp1y: Number, cp2x: Number, cp2y: Number, x: Number, y: Number): Unit = js.noImpl
    public native fun arcTo(x1: Number, y1: Number, x2: Number, y2: Number, radius: Number): Unit = js.noImpl
    public native fun rect(x: Number, y: Number, w: Number, h: Number): Unit = js.noImpl
    public native fun arc(x: Number, y: Number, radius: Number, startAngle: Number, endAngle: Number, anticlockwise: Boolean): Unit = js.noImpl
    public native fun fill(): Unit = js.noImpl
    public native fun stroke(): Unit = js.noImpl
    public native fun clip(): Unit = js.noImpl
    public native fun isPointInPath(x: Number, y: Number): Boolean = js.noImpl
    public native fun drawFocusRing(element: Element, xCaret: Number, yCaret: Number, canDrawCustom: Boolean): Unit = js.noImpl
    public native fun drawFocusRing(element: Element, xCaret: Number, yCaret: Number): Unit = js.noImpl
    public native var font: String = js.noImpl
    public native var textAlign: String = js.noImpl
    public native var textBaseline: String = js.noImpl
    public native fun fillText(text: String, x: Number, y: Number, maxWidth: Number): Unit = js.noImpl
    public native fun fillText(text: String, x: Number, y: Number): Unit = js.noImpl
    public native fun strokeText(text: String, x: Number, y: Number, maxWidth: Number): Unit = js.noImpl
    public native fun strokeText(text: String, x: Number, y: Number): Unit = js.noImpl
    public native fun measureText(text: String): TextMetrics? = js.noImpl
    public native fun drawImage(img_elem: HTMLElement, dx_or_sx: Number, dy_or_sy: Number, dw_or_sw: Number, dh_or_sh: Number, dx: Number, dy: Number, dw: Number, dh: Number): Unit = js.noImpl
    public native fun drawImage(img_elem: HTMLElement, dx_or_sx: Number, dy_or_sy: Number): Unit = js.noImpl
    public native fun drawImage(img_elem: HTMLElement, dx_or_sx: Number, dy_or_sy: Number, dw_or_sw: Number): Unit = js.noImpl
    public native fun drawImage(img_elem: HTMLElement, dx_or_sx: Number, dy_or_sy: Number, dw_or_sw: Number, dh_or_sh: Number): Unit = js.noImpl
    public native fun drawImage(img_elem: HTMLElement, dx_or_sx: Number, dy_or_sy: Number, dw_or_sw: Number, dh_or_sh: Number, dx: Number): Unit = js.noImpl
    public native fun drawImage(img_elem: HTMLElement, dx_or_sx: Number, dy_or_sy: Number, dw_or_sw: Number, dh_or_sh: Number, dx: Number, dy: Number): Unit = js.noImpl
    public native fun drawImage(img_elem: HTMLElement, dx_or_sx: Number, dy_or_sy: Number, dw_or_sw: Number, dh_or_sh: Number, dx: Number, dy: Number, dw: Number): Unit = js.noImpl
    public native fun createImageData(imagedata: ImageData, sh: Number): ImageData? = js.noImpl
    public native fun createImageData(imagedata: ImageData): ImageData? = js.noImpl
    public native fun createImageData(sw: Number, sh: Number): ImageData? = js.noImpl
    public native fun createImageData(sw: Number): ImageData? = js.noImpl
    public native fun getImageData(sx: Number, sy: Number, sw: Number, sh: Number): ImageData? = js.noImpl
    public native fun putImageData(image_data: ImageData, dx: Number, dy: Number, dirtyX: Number, dirtyY: Number, dirtyWidth: Number, dirtyHeight: Number): Unit = js.noImpl
    public native fun putImageData(image_data: ImageData, dx: Number, dy: Number): Unit = js.noImpl
    public native fun putImageData(image_data: ImageData, dx: Number, dy: Number, dirtyX: Number): Unit = js.noImpl
    public native fun putImageData(image_data: ImageData, dx: Number, dy: Number, dirtyX: Number, dirtyY: Number): Unit = js.noImpl
    public native fun putImageData(image_data: ImageData, dx: Number, dy: Number, dirtyX: Number, dirtyY: Number, dirtyWidth: Number): Unit = js.noImpl
}

public native trait CanvasGradient {
    public native fun addColorStop(offset: Number, color: String): Unit = js.noImpl
}

public native trait ImageData {
    public native var data: CanvasPixelArray = js.noImpl
    public native var width: Double = js.noImpl
    public native var height: Double = js.noImpl
}

public native trait CanvasPixelArray {
    public native var length: Double = js.noImpl
}

public native trait TextMetrics {
    public native var width: Double = js.noImpl
}

