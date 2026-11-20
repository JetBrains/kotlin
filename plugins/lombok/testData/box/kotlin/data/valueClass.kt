// WITH_STDLIB

import lombok.Data
import lombok.EqualsAndHashCode
import lombok.ToString
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@JvmInline
@Data
value class DataValue(val x: Int)

@JvmInline
value class EqualsAndHashCodeValue(val x: Int)

@JvmInline
@ToString
value class ToStringValue(val x: Int)

fun box(): String {
    assertEquals(DataValue(1), DataValue(1))
    assertTrue(DataValue(1) != DataValue(2))
    assertEquals(DataValue(1).hashCode(), DataValue(1).hashCode())
    assertEquals("DataValue(x=1)", DataValue(1).toString())

    assertEquals(EqualsAndHashCodeValue(1), EqualsAndHashCodeValue(1))
    assertTrue(EqualsAndHashCodeValue(1) != EqualsAndHashCodeValue(2))
    assertEquals(EqualsAndHashCodeValue(1).hashCode(), EqualsAndHashCodeValue(1).hashCode())

    assertEquals("ToStringValue(x=1)", ToStringValue(1).toString())

    val boxed: Any = DataValue(3)
    assertEquals<Any>(DataValue(3), boxed)
    assertEquals("DataValue(x=3)", boxed.toString())

    return "OK"
}
