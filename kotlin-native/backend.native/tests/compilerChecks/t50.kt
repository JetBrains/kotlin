import kotlin.native.concurrent.*

class Z(val x: Int) {
    fun bar(s: String) = s + x.toString()
}

class Q(x: Int) {
    init {
        val worker = Worker.start()
        worker.execute(TransferMode.SAFE, { "zzz" }, Z(x)::bar)
    }
}

