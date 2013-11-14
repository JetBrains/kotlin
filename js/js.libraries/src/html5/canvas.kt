package js.dom.html5

import js.native
import js.noImpl
import js.dom.html.HTMLElement
import org.w3c.dom.Element

public native trait HTMLCanvasElement : HTMLElement {
    public native var width: Double
    public native var height: Double
    public native fun getContext(context: String): CanvasContext?
    public native fun toDataURL(string: String, args: Any): String
    public native fun toDataURL(): String
    public native fun toDataURL(string: String): String
}

public native trait CanvasContext {
    public native var canvas: HTMLCanvasElement
    public native fun save(): Unit
    public native fun restore(): Unit
    public native fun scale(x: Number, y: Number): Unit
    public native fun rotate(angle: Number): Unit
    public native fun translate(x: Number, y: Number): Unit
    public native fun transform(m11: Number, m12: Number, m21: Number, m22: Number, dx: Number, dy: Number): Unit
    public native fun setTransform(m11: Number, m12: Number, m21: Number, m22: Number, dx: Number, dy: Number): Unit
    public native var globalAlpha: Double
    public native var globalCompositeOperation: String
    public native var fillStyle: Any
    public native var strokeStyle: Any
    public native fun createLinearGradient(x0: Number, y0: Number, x1: Number, y1: Number): CanvasGradient?
    public native fun createRadialGradient(x0: Number, y0: Number, r0: Number, x1: Number, y1: Number, r1: Number): CanvasGradient?
    public native var lineWidth: Double
    public native var lineCap: String
    public native var lineJoin: String
    public native var miterLimit: Double
    public native var shadowOffsetX: Double
    public native var shadowOffsetY: Double
    public native var shadowBlur: Double
    public native var shadowColor: String
    public native fun clearRect(x: Number, y: Number, w: Number, h: Number): Unit
    public native fun fillRect(x: Number, y: Number, w: Number, h: Number): Unit
    public native fun strokeRect(x: Number, y: Number, w: Number, h: Number): Unit
    public native fun beginPath(): Unit
    public native fun closePath(): Unit
    public native fun moveTo(x: Number, y: Number): Unit
    public native fun lineTo(x: Number, y: Number): Unit
    public native fun quadraticCurveTo(cpx: Number, cpy: Number, x: Number, y: Number): Unit
    public native fun bezierCurveTo(cp1x: Number, cp1y: Number, cp2x: Number, cp2y: Number, x: Number, y: Number): Unit
    public native fun arcTo(x1: Number, y1: Number, x2: Number, y2: Number, radius: Number): Unit
    public native fun rect(x: Number, y: Number, w: Number, h: Number): Unit
    public native fun arc(x: Number, y: Number, radius: Number, startAngle: Number, endAngle: Number, anticlockwise: Boolean): Unit
    public native fun fill(): Unit
    public native fun stroke(): Unit
    public native fun clip(): Unit
    public native fun isPointInPath(x: Number, y: Number): Boolean
    public native fun drawFocusRing(element: Element, xCaret: Number, yCaret: Number, canDrawCustom: Boolean): Unit
    public native fun drawFocusRing(element: Element, xCaret: Number, yCaret: Number): Unit
    public native var font: String
    public native var textAlign: String
    public native var textBaseline: String
    public native fun fillText(text: String, x: Number, y: Number, maxWidth: Number): Unit
    public native fun fillText(text: String, x: Number, y: Number): Unit
    public native fun strokeText(text: String, x: Number, y: Number, maxWidth: Number): Unit
    public native fun strokeText(text: String, x: Number, y: Number): Unit
    public native fun measureText(text: String): TextMetrics?
    public native fun drawImage(img_elem: HTMLElement, dx_or_sx: Number, dy_or_sy: Number, dw_or_sw: Number, dh_or_sh: Number, dx: Number, dy: Number, dw: Number, dh: Number): Unit
    public native fun drawImage(img_elem: HTMLElement, dx_or_sx: Number, dy_or_sy: Number): Unit
    public native fun drawImage(img_elem: HTMLElement, dx_or_sx: Number, dy_or_sy: Number, dw_or_sw: Number): Unit
    public native fun drawImage(img_elem: HTMLElement, dx_or_sx: Number, dy_or_sy: Number, dw_or_sw: Number, dh_or_sh: Number): Unit
    public native fun drawImage(img_elem: HTMLElement, dx_or_sx: Number, dy_or_sy: Number, dw_or_sw: Number, dh_or_sh: Number, dx: Number): Unit
    public native fun drawImage(img_elem: HTMLElement, dx_or_sx: Number, dy_or_sy: Number, dw_or_sw: Number, dh_or_sh: Number, dx: Number, dy: Number): Unit
    public native fun drawImage(img_elem: HTMLElement, dx_or_sx: Number, dy_or_sy: Number, dw_or_sw: Number, dh_or_sh: Number, dx: Number, dy: Number, dw: Number): Unit
    public native fun createImageData(imagedata: ImageData, sh: Number): ImageData?
    public native fun createImageData(imagedata: ImageData): ImageData?
    public native fun createImageData(sw: Number, sh: Number): ImageData?
    public native fun createImageData(sw: Number): ImageData?
    public native fun getImageData(sx: Number, sy: Number, sw: Number, sh: Number): ImageData?
    public native fun putImageData(image_data: ImageData, dx: Number, dy: Number, dirtyX: Number, dirtyY: Number, dirtyWidth: Number, dirtyHeight: Number): Unit
    public native fun putImageData(image_data: ImageData, dx: Number, dy: Number): Unit
    public native fun putImageData(image_data: ImageData, dx: Number, dy: Number, dirtyX: Number): Unit
    public native fun putImageData(image_data: ImageData, dx: Number, dy: Number, dirtyX: Number, dirtyY: Number): Unit
    public native fun putImageData(image_data: ImageData, dx: Number, dy: Number, dirtyX: Number, dirtyY: Number, dirtyWidth: Number): Unit
}

public native trait CanvasGradient {
    public native fun addColorStop(offset: Number, color: String): Unit
}

public native trait ImageData {
    public native var data: CanvasPixelArray
    public native var width: Double
    public native var height: Double
}

public native trait CanvasPixelArray {
    public native var length: Double
}

public native trait TextMetrics {
    public native var width: Double
}

