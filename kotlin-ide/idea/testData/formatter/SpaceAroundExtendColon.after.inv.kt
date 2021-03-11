import java.util.ArrayList
import java.lang.Iterable

class Some: java.util.ArrayList<Int>() {
    fun some<T: Iterable<Int>>(array: Array<String>): Int {
        val test: Int = 12
        return test
    }
}

// SET_TRUE: SPACE_BEFORE_EXTEND_COLON
// SET_FALSE: SPACE_AFTER_EXTEND_COLON