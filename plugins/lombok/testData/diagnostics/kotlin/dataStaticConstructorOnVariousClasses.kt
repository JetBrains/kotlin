// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-91272, KT_91394
// FIR_DUMP

import lombok.Data

class A {
    @Data(staticConstructor = "of")
    companion object {
        val count: Int = 1
        val name: String = "object"
    }
}

@Data(staticConstructor = "of")
object B {
    val x: Int = 2
    val z: Boolean = true
}

<!ANNOTATION_HAS_NO_EFFECT!>@Data(staticConstructor = "of")<!>
interface C {
    val s: String
}

<!ANNOTATION_HAS_NO_EFFECT!>@Data(staticConstructor = "of")<!>
annotation class Ann(val s: String)

fun foo() {
    @Data(staticConstructor = "of")
    class A(val count: Int, val name: String)

    <!LOCAL_INTERFACE_NOT_ALLOWED!><!ANNOTATION_HAS_NO_EFFECT!>@Data(staticConstructor = "of")<!>
    interface C<!> {
        val s: String
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, companionObject, enumDeclaration, enumEntry,
functionDeclaration, integerLiteral, interfaceDeclaration, localClass, objectDeclaration, primaryConstructor,
propertyDeclaration, stringLiteral */
