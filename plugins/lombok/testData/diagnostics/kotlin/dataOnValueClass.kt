// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// WITH_STDLIB

import lombok.*

// Kotlin reserves `equals`/`hashCode` in a value class, so neither annotation implying them should generate anything;
// however we shouldn't report anything on @Data, as e.g. toString can be generated.
@JvmInline
@Data
value class DataValue(val x: Int)

@JvmInline
<!ANNOTATION_IS_NOT_SUPPORTED_ON_CLASS!>@EqualsAndHashCode<!>
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

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, integerLiteral, primaryConstructor,
propertyDeclaration, value */
