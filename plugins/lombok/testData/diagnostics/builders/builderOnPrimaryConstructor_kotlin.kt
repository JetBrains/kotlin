// WITH_STDLIB
// FILE: usage.kt

fun usage() {
    val plain: Plain = Plain.builder().id(1).name("a").raw(2).build()
    Plain.builder().<!UNRESOLVED_REFERENCE!>body<!>(3)

    val copy: WithToBuilder = WithToBuilder.builder().id(1).build().toBuilder().id(2).build()

    val empty: Empty = Empty.builder().build()
    Empty.builder().<!UNRESOLVED_REFERENCE!>x<!>(1)

    val both: BothAnnotations = BothAnnotations.builder().id(1).build()
}

// FILE: test.kt

import lombok.Builder
import lombok.Singular

class Plain @Builder constructor(val id: Int, var name: String, raw: Int) {
    var body: Int = raw
}

class WithToBuilder @Builder(toBuilder = true) constructor(val id: Int)

class ToBuilderCannotObtain @Builder(toBuilder = true) constructor(val id: Int, <!TO_BUILDER_CANNOT_OBTAIN!>lost: Int<!>)

// A constructor builder does not look at the promoted properties, so `@Builder.Default` on one is ignored, and the
// default value is reported as ignored the same way as for a secondary constructor
class Defaults @Builder constructor(@Builder.Default val id: Int = <!BUILDER_WILL_IGNORE_INITIALIZING_EXPRESSION!>0<!>, val other: Int = <!BUILDER_WILL_IGNORE_INITIALIZING_EXPRESSION!>1<!>)

class SingularOnPrimary @Builder constructor(@Singular val names: List<String>, <!CANNOT_SINGULARIZE_NAME!>@Singular<!> val sheep: List<String>)

class Empty @Builder constructor() {
    var x: Int = 0
}

@Builder
class BothAnnotations @Builder constructor(val id: Int)
