package demo

interface WindowListener {
    public fun windowClosing()
}

open class WindowAdapter : WindowListener {
    override fun windowClosing() {
    }
}

open class Frame {
    public fun addWindowListener(listener: WindowListener) {
    }
}

public class Client : Frame() {
    init {
        val a = object : WindowAdapter() {
            override fun windowClosing() {
            }
        }

        addWindowListener(a)

        addWindowListener(object : WindowAdapter() {
            override fun windowClosing() {
            }
        })
    }
}