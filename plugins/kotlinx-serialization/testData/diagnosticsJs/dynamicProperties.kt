// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

import kotlinx.serialization.*

// `@Contextual` hides `dynamic` from SERIALIZER_NOT_FOUND, which is how this reached the backend and crashed it
// with a ClassCastException on IrDynamicTypeImpl. See KT-59088.
@Serializable
class WithContextualDynamic {
    @Contextual
    val a: <!DYNAMIC_TYPE_NOT_SUPPORTED!>dynamic<!> = ""
}

@Serializable
class WithDynamic {
    val a: <!DYNAMIC_TYPE_NOT_SUPPORTED!>dynamic<!> = ""
}

@Serializable
class WithDynamicInCtor(val a: <!DYNAMIC_TYPE_NOT_SUPPORTED!>dynamic<!>)

// @Transient properties are not serialized at all, so `dynamic` is fine there.
@Serializable
class WithTransientDynamic(val i: Int) {
    @Transient
    val a: dynamic = ""
}

@Serializable
class Statically(val s: String, val i: Int)

fun notAProperty(): dynamic = ""
