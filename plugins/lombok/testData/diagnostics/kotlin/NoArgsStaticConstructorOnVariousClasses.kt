// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-91272, KT_91394
// FIR_DUMP

import lombok.NoArgsConstructor

class A {
    <!ANNOTATION_HAS_NO_EFFECT!>@NoArgsConstructor(staticName = "of")<!>
    companion object {
        val count: Int = 1
        val name: String = "object"
    }
}

<!ANNOTATION_HAS_NO_EFFECT!>@NoArgsConstructor(staticName = "of")<!>
object B {
    val x: Int = 2
    val z: Boolean = true
}

<!ANNOTATION_HAS_NO_EFFECT!>@NoArgsConstructor(staticName = "of")<!>
interface C {
    val s: String
}

<!ANNOTATION_HAS_NO_EFFECT, NO_ARGS_CONSTRUCTOR_FORCE_REQUIRED, NO_NOARG_CONSTRUCTOR_IN_SUPERCLASS!>@NoArgsConstructor(staticName = "of")<!>
enum class E(val y: Int = 0) {
    <!WRONG_ANNOTATION_TARGET!>@NoArgsConstructor(staticName = "of")<!>
    FIRST,
    <!WRONG_ANNOTATION_TARGET!>@NoArgsConstructor(staticName = "of")<!>
    SECOND(4);
}

<!ANNOTATION_HAS_NO_EFFECT, NO_ARGS_CONSTRUCTOR_FORCE_REQUIRED!>@NoArgsConstructor(staticName = "of")<!>
annotation class Ann(val s: String)

fun foo() {
    <!ANNOTATION_HAS_NO_EFFECT!>@NoArgsConstructor(staticName = "of")<!>
    class A(val count: Int, val name: String)

    <!LOCAL_INTERFACE_NOT_ALLOWED!><!ANNOTATION_HAS_NO_EFFECT!>@NoArgsConstructor(staticName = "of")<!>
    interface C<!> {
        val s: String
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, companionObject, enumDeclaration, enumEntry,
functionDeclaration, integerLiteral, interfaceDeclaration, localClass, objectDeclaration, primaryConstructor,
propertyDeclaration, stringLiteral */
