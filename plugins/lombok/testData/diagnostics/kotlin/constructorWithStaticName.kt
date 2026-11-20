// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-91183
// FIR_DUMP
// WITH_STDLIB
import lombok.*

@Data(staticConstructor = "of")
class DataWithStatic(val name: String)

@NoArgsConstructor(staticName = "of", force = true)
class NoArgsWithStatic(val name: String)

class Outer {
    <!ANNOTATION_IS_NOT_SUPPORTED_ON_CLASS!>@NoArgsConstructor(staticName = "of", force = true)<!>
    inner class InnerNoArgs {
        var value: Int = 0
    }

    <!ANNOTATION_IS_NOT_SUPPORTED_ON_CLASS!>@Data(staticConstructor = "of")<!>
    inner class InnerData {
        private val value = 0
    }

    <!ANNOTATION_IS_NOT_SUPPORTED_ON_CLASS!>@EqualsAndHashCode<!>
    inner class InnerEqualsAndHashCode {
        private val value = 0
    }
}

class GenericOuter<T> {
    <!ANNOTATION_IS_NOT_SUPPORTED_ON_CLASS!>@NoArgsConstructor(staticName = "of", force = true)<!>
    inner class InnerNoArgs {
        var value: T? = null
    }

    <!ANNOTATION_IS_NOT_SUPPORTED_ON_CLASS!>@Data(staticConstructor = "of")<!>
    inner class InnerData {
        var value: T? = null
    }
}

fun test(outer: Outer, genericOuter: GenericOuter<String>) {
    <!NO_VALUE_FOR_PARAMETER!>DataWithStatic<!>()
    DataWithStatic.of("name")

    <!INVISIBLE_REFERENCE!>NoArgsWithStatic<!>()
    NoArgsWithStatic.of()

    Outer.InnerNoArgs.<!UNRESOLVED_REFERENCE!>of<!>()
    Outer.InnerData.<!UNRESOLVED_REFERENCE!>of<!>(1)
    GenericOuter.InnerNoArgs.<!UNRESOLVED_REFERENCE!>of<!>()
    GenericOuter.InnerData.<!UNRESOLVED_REFERENCE!>of<!>("name")

    outer.InnerNoArgs()
    genericOuter.InnerNoArgs()

    outer.InnerData(<!TOO_MANY_ARGUMENTS!>1<!>)
    genericOuter.InnerData(<!TOO_MANY_ARGUMENTS!>"name"<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inner, integerLiteral, nullableType, primaryConstructor,
propertyDeclaration, stringLiteral, typeParameter */
