import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

<warning descr="SSR">fun a() = object : MouseAdapter() { }</warning>

fun b() = object { }