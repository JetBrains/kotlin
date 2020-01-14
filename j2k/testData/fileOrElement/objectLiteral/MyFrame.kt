package demo

internal interface WindowListener {
    fun windowClosing()
}

internal interface EmptyWindowListener

internal open class EmptyWindowAdapter : EmptyWindowListener

internal open class WindowAdapter : WindowListener {
    override fun windowClosing() {}
}

internal open class Frame {
    fun addWindowListener(listener: WindowListener) {}
}

internal class Client : Frame() {
    init {
        val a = object : WindowAdapter() {
            override fun windowClosing() {}
        }

        addWindowListener(a)

        addWindowListener(
                object : WindowAdapter() {
                    override fun windowClosing() {}
                }
        )

        val b = object : EmptyWindowListener {

        }
        val c = object : EmptyWindowAdapter() {

        }
    }
}