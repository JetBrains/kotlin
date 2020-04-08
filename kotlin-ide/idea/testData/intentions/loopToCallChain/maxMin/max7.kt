// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'map{}.max()'"
// INTENTION_TEXT_2: "Replace with 'asSequence().map{}.max()'"
import java.lang.Math.max

fun getMaxLineWidth(count: Int): Double {
    var m = 0.0
    <caret>for (i in 0..count-1) {
        m = max(m, getLineWidth(i))
    }
    return m
}

fun getLineWidth(i: Int): Double = TODO()
