// FIR_DUMP
// WITH_STDLIB

import lombok.*

// Kotlin reserves `equals`/`hashCode` in a value class, so neither annotation implying them should generate anything,
// and they should be reported like `@NoArgsConstructor` is (`ANNOTATION_IS_NOT_SUPPORTED_ON_CLASS`).
@JvmInline
@Data
value class DataValue(val x: Int)

@JvmInline
@EqualsAndHashCode
value class EqualsAndHashCodeValue(val x: Int)

// `toString` is allowed in a value class.
@JvmInline
@ToString
value class ToStringValue(val x: Int)

fun test(a: DataValue, b: EqualsAndHashCodeValue, c: ToStringValue) {
    a == DataValue(1)
    a.hashCode()
    b == EqualsAndHashCodeValue(1)
    b.hashCode()
    c.toString()
}
