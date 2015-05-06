package kotlin.js.dom.html5

import kotlin.js.dom.html.HTMLElement
import org.w3c.dom.Element

deprecated("Use declarations from org.w3c.dom instead")
public native trait HTMLCanvasElement : HTMLElement {
    public var width: Double
    public var height: Double
    public fun getContext(contextId: String, vararg attributes: Any): CanvasContext?
    public fun toDataURL(string: String, args: Any): String
    public fun toDataURL(): String
    public fun toDataURL(string: String): String
}

deprecated("Use declarations from org.w3c.dom instead. See CanvasRenderingContext2D")
public native trait CanvasContext {
    public var canvas: HTMLCanvasElement
    public fun save(): Unit
    public fun restore(): Unit
    public fun scale(x: Number, y: Number): Unit
    public fun rotate(angle: Number): Unit
    public fun translate(x: Number, y: Number): Unit
    public fun transform(m11: Number, m12: Number, m21: Number, m22: Number, dx: Number, dy: Number): Unit
    public fun setTransform(m11: Number, m12: Number, m21: Number, m22: Number, dx: Number, dy: Number): Unit
    public var globalAlpha: Double
    public var globalCompositeOperation: String
    public var fillStyle: Any
    public var strokeStyle: Any
    public fun createLinearGradient(x0: Number, y0: Number, x1: Number, y1: Number): CanvasGradient?
    public fun createRadialGradient(x0: Number, y0: Number, r0: Number, x1: Number, y1: Number, r1: Number): CanvasGradient?
    public var lineWidth: Double
    public var lineCap: String
    public var lineJoin: String
    public var miterLimit: Double
    public var shadowOffsetX: Double
    public var shadowOffsetY: Double
    public var shadowBlur: Double
    public var shadowColor: String
    public fun clearRect(x: Number, y: Number, w: Number, h: Number): Unit
    public fun fillRect(x: Number, y: Number, w: Number, h: Number): Unit
    public fun strokeRect(x: Number, y: Number, w: Number, h: Number): Unit
    public fun beginPath(): Unit
    public fun closePath(): Unit
    public fun moveTo(x: Number, y: Number): Unit
    public fun lineTo(x: Number, y: Number): Unit
    public fun quadraticCurveTo(cpx: Number, cpy: Number, x: Number, y: Number): Unit
    public fun bezierCurveTo(cp1x: Number, cp1y: Number, cp2x: Number, cp2y: Number, x: Number, y: Number): Unit
    public fun arcTo(x1: Number, y1: Number, x2: Number, y2: Number, radius: Number): Unit
    public fun rect(x: Number, y: Number, w: Number, h: Number): Unit
    public fun arc(x: Number, y: Number, radius: Number, startAngle: Number, endAngle: Number, anticlockwise: Boolean): Unit
    public fun fill(): Unit
    public fun stroke(): Unit
    public fun clip(): Unit
    public fun isPointInPath(x: Number, y: Number): Boolean
    public fun drawFocusRing(element: Element, xCaret: Number, yCaret: Number, canDrawCustom: Boolean): Unit
    public fun drawFocusRing(element: Element, xCaret: Number, yCaret: Number): Unit
    public var font: String
    public var textAlign: String
    public var textBaseline: String
    public fun fillText(text: String, x: Number, y: Number, maxWidth: Number): Unit
    public fun fillText(text: String, x: Number, y: Number): Unit
    public fun strokeText(text: String, x: Number, y: Number, maxWidth: Number): Unit
    public fun strokeText(text: String, x: Number, y: Number): Unit
    public fun measureText(text: String): TextMetrics?
    public fun drawImage(image: HTMLElement, dx: Number, dy: Number): Unit
    public fun drawImage(image: HTMLElement, dx: Number, dy: Number, dw: Number, dh: Number): Unit
    public fun drawImage(image: HTMLElement, sx: Number, sy: Number, sw: Number, sh: Number, dx: Number, dy: Number, dw: Number, dh: Number): Unit
    public fun createImageData(imagedata: ImageData, sh: Number): ImageData?
    public fun createImageData(imagedata: ImageData): ImageData?
    public fun createImageData(sw: Number, sh: Number): ImageData?
    public fun createImageData(sw: Number): ImageData?
    public fun getImageData(sx: Number, sy: Number, sw: Number, sh: Number): ImageData?
    public fun putImageData(image_data: ImageData, dx: Number, dy: Number, dirtyX: Number, dirtyY: Number, dirtyWidth: Number, dirtyHeight: Number): Unit
    public fun putImageData(image_data: ImageData, dx: Number, dy: Number): Unit
    public fun putImageData(image_data: ImageData, dx: Number, dy: Number, dirtyX: Number): Unit
    public fun putImageData(image_data: ImageData, dx: Number, dy: Number, dirtyX: Number, dirtyY: Number): Unit
    public fun putImageData(image_data: ImageData, dx: Number, dy: Number, dirtyX: Number, dirtyY: Number, dirtyWidth: Number): Unit
}

deprecated("Use declarations from org.w3c.dom instead")
public native trait CanvasGradient {
    public fun addColorStop(offset: Number, color: String): Unit
}

deprecated("Use declarations from org.w3c.dom instead")
public native trait ImageData {
    public var data: CanvasPixelArray
    public var width: Double
    public var height: Double
}

public native trait CanvasPixelArray {
    public var length: Double
}

deprecated("Use declarations from org.w3c.dom instead")
public native trait TextMetrics {
    public var width: Double
}

