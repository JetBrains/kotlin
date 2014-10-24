// ERROR: This type is final, so it cannot be inherited from
// ERROR: This type is final, so it cannot be inherited from
// ERROR: This type is final, so it cannot be inherited from
package demo

trait WindowListener {
    public fun windowClosing()
}

class WindowAdapter : WindowListener {
    override fun windowClosing() {
    }
}

class Frame {
    public fun addWindowListener(listener: WindowListener) {
    }
}

public class Client : Frame() {
    {
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