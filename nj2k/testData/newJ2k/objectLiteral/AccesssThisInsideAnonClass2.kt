import Test.FrameCallback

class Foo {
    fun fail() {
        object : FrameCallback {
            override fun doFrame() {
                Test().postFrameCallbackDelayed(this)
            }
        }
    }
}

internal class Test {
    interface FrameCallback {
        fun doFrame()
    }

    fun postFrameCallbackDelayed(callback: FrameCallback?) {}
}
