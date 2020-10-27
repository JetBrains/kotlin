import kotlin.native.concurrent.*

class Z(val x: Int) {
    fun bar(s: String) = s + x.toString()
}

fun foo(x: Int) {
    val worker = Worker.start()
    worker.execute(TransferMode.SAFE, { "zzz" }, Z(x)::bar)
}
