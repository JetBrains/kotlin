package example.library

import org.w3c.dom.Text
import kotlin.browser.*

public class Counter(val el: Text) {
    fun step(n: Int) {
        document.title = "Counter: ${n}"
        window.setTimeout({step(n+1)}, 1000)
    }

    fun start() {
        step(0)
    }
}
