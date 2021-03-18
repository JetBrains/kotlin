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
    @Serializable class B // nested classes are allowed

    <!INNER_CLASSES_NOT_SUPPORTED!>@Serializable<!> inner class C // inner classes are not

    @Serializable object F {} // regular named object, OK
}