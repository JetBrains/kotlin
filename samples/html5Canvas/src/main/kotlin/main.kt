import html5.minimal.*
import kotlinx.wasm.jsinterop.*
import konan.internal.ExportForCppRuntime

fun main(args: Array<String>) {

    val html5 = Html5()
    val document = html5.document

    val canvas = document.getElementById("myCanvas").asCanvas
    val ctx = canvas.getContext("2d")
    val rect = canvas.getBoundingClientRect()
    val rectLeft = rect.getInt("left")
    val rectTop = rect.getInt("top")

    var mouseX: Int = 0
    var mouseY: Int = 0
    var draw: Boolean = false

    document.setter("onmousemove") { args: ArrayList<JsValue> ->
        val event = args[0]
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

    html5.setInterval(10) {
        if (draw) {
            ctx.setter("strokeStyle", "#222222")
            ctx.lineTo(mouseX, mouseY)
            ctx.stroke()
        } else {
            ctx.moveTo(mouseX, mouseY)
            ctx.stroke()
        }
    }
}

