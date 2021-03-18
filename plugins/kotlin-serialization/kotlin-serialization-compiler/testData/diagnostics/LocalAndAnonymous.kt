// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE
// WITH_RUNTIME
// SKIP_TXT

import kotlinx.serialization.*

fun container() {
    <!LOCAL_CLASSES_NOT_SUPPORTED!>@Serializable<!>
    class X

    val y = <!LOCAL_CLASSES_NOT_SUPPORTED!>@Serializable<!> object {}
}

val topLevelAnon = <!LOCAL_CLASSES_NOT_SUPPORTED!>@Serializable<!> object {}

@Serializable class A {
    @Serializable class B // nesting classes are allowed
}