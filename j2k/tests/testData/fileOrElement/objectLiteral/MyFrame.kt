package demo

class WindowAdapter {
    public fun windowClosing() {
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