package language

import java.lang.Double as jDouble
import java.lang.Float as jFloat
import org.junit.Test as test
import kotlin.test.*

public class RangeJVMTest {

    test fun doubleRange() {
        val range = -1.0..3.14159265358979
        assertFalse(jDouble.NEGATIVE_INFINITY in range)
        assertFalse(jDouble.POSITIVE_INFINITY in range)
        assertFalse(jDouble.NaN in range)
    }

    test fun floatRange() {
        val range = -1.0f..3.14159f
        assertFalse(jFloat.NEGATIVE_INFINITY in range)
        assertFalse(jFloat.POSITIVE_INFINITY in range)

        assertFalse(jFloat.NaN in range)
    }

    test fun illegalProgressionCreation() {
        // create Progression explicitly with increment = 0
        failsWith(javaClass<IllegalArgumentException>()) { IntProgression(0, 5, 0) }
        failsWith(javaClass<IllegalArgumentException>()) { ByteProgression(0, 5, 0) }
        failsWith(javaClass<IllegalArgumentException>()) { ShortProgression(0, 5, 0) }
        failsWith(javaClass<IllegalArgumentException>()) { LongProgression(0, 5, 0) }
        failsWith(javaClass<IllegalArgumentException>()) { CharProgression('a', 'z', 0) }
        failsWith(javaClass<IllegalArgumentException>()) { DoubleProgression(0.0, 5.0, 0.0) }
        failsWith(javaClass<IllegalArgumentException>()) { FloatProgression(0.0f, 5.0f, 0.0f) }

        failsWith(javaClass<IllegalArgumentException>()) { 0..5 step 0 }
        failsWith(javaClass<IllegalArgumentException>()) { 0.toByte()..5.toByte() step 0 }
        failsWith(javaClass<IllegalArgumentException>()) { 0.toShort()..5.toShort() step 0  }
        failsWith(javaClass<IllegalArgumentException>()) { 0L..5L step 0L }
        failsWith(javaClass<IllegalArgumentException>()) { 'a'..'z' step 0 }
        failsWith(javaClass<IllegalArgumentException>()) { 0.0..5.0 step 0.0 }
        failsWith(javaClass<IllegalArgumentException>()) { 0.0f..5.0f step 0.0f }

        failsWith(javaClass<IllegalArgumentException>()) { 0 downTo -5 step 0 }
        failsWith(javaClass<IllegalArgumentException>()) { 0.toByte() downTo -5.toByte() step 0 }
        failsWith(javaClass<IllegalArgumentException>()) { 0.toShort() downTo -5.toShort() step 0  }
        failsWith(javaClass<IllegalArgumentException>()) { 0L downTo -5L step 0L }
        failsWith(javaClass<IllegalArgumentException>()) { 'z' downTo 'a' step 0 }
        failsWith(javaClass<IllegalArgumentException>()) { 0.0 downTo -5.0 step 0.0 }
        failsWith(javaClass<IllegalArgumentException>()) { 0.0f downTo -5.0f step 0.0f }

        failsWith(javaClass<IllegalArgumentException>()) { 0..5 step -2 }
        failsWith(javaClass<IllegalArgumentException>()) { 0.toByte()..5.toByte() step -2 }
        failsWith(javaClass<IllegalArgumentException>()) { 0.toShort()..5.toShort() step -2  }
        failsWith(javaClass<IllegalArgumentException>()) { 0L..5L step -2L }
        failsWith(javaClass<IllegalArgumentException>()) { 'a'..'z' step -2 }
        failsWith(javaClass<IllegalArgumentException>()) { 0.0..5.0 step -0.5 }
        failsWith(javaClass<IllegalArgumentException>()) { 0.0f..5.0f step -0.5f }

        failsWith(javaClass<IllegalArgumentException>()) { 0 downTo -5 step -2 }
        failsWith(javaClass<IllegalArgumentException>()) { 0.toByte() downTo -5.toByte() step -2 }
        failsWith(javaClass<IllegalArgumentException>()) { 0.toShort() downTo -5.toShort() step -2  }
        failsWith(javaClass<IllegalArgumentException>()) { 0L downTo -5L step -2L }
        failsWith(javaClass<IllegalArgumentException>()) { 'z' downTo 'a' step -2 }
        failsWith(javaClass<IllegalArgumentException>()) { 0.0 downTo -5.0 step -0.5 }
        failsWith(javaClass<IllegalArgumentException>()) { 0.0f downTo -5.0f step -0.5f }

        // NaN increment or step
        failsWith(javaClass<IllegalArgumentException>()) { DoubleProgression(0.0, 5.0, jDouble.NaN) }
        failsWith(javaClass<IllegalArgumentException>()) { FloatProgression(0.0f, 5.0f, jFloat.NaN) }

        failsWith(javaClass<IllegalArgumentException>()) { 0.0..5.0 step jDouble.NaN }
        failsWith(javaClass<IllegalArgumentException>()) { 0.0f..5.0f step jFloat.NaN }

        failsWith(javaClass<IllegalArgumentException>()) { 5.0 downTo 0.0 step jDouble.NaN }
        failsWith(javaClass<IllegalArgumentException>()) { 5.0f downTo 0.0f step jFloat.NaN }
    }
}