import kotlinx.interop.wasm.dom.*
import kotlinx.wasm.jsinterop.*

fun main(args: Array<String>) {

    val canvas = document.getElementById("myCanvas").asCanvas
    val ctx = canvas.getContext("2d")
    val rect = canvas.getBoundingClientRect()
    val rectLeft = rect.left
    val rectTop = rect.top

    var mouseX: Int = 0
    var mouseY: Int = 0
    var draw: Boolean = false

    document.setter("onmousemove") { arguments: ArrayList<JsValue> ->
        val event = MouseEvent(arguments[0])
        mouseX = event.getInt("clientX") - rectLeft
        mouseY = event.getInt("clientY") - rectTop

        if (mouseX < 0) mouseX = 0
        if (mouseX > 639) mouseX = 639
        if (mouseY < 0) mouseY = 0
        if (mouseY > 479) mouseY = 479
    }

    document.setter("onmousedown") {
        draw = true
    }

    document.setter("onmouseup") {
        draw = false
    }

    setInterval(10) {
        if (draw) {
            ctx.strokeStyle = "#222222"
            ctx.lineTo(mouseX, mouseY)
            ctx.stroke()
        } else {
            ctx.moveTo(mouseX, mouseY)
            ctx.stroke()
        }
    }
}

