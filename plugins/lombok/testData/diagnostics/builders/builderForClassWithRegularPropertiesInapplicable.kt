// ISSUE: KT-90879
// WITH_STDLIB
// RENDER_DIAGNOSTICS_FULL_TEXT

import lombok.Builder
import lombok.Singular

@Builder
class WithoutConstructor {
    var varWithoutDefault: Int = 0
    val <!BUILDER_PROPERTY_NOT_INCLUDED!>valWithoutDefault<!>: Int = 0
    @Builder.Default
    var varWithDefault: Int = 0
    <!BUILDER_FIELD_ANNOTATION_ON_BODY_PROPERTY!>@Builder.Default<!>
    val <!BUILDER_PROPERTY_NOT_INCLUDED!>valWithDefault<!>: Int = 0
    @Singular
    var singularVars: List<String> = emptyList()
    <!BUILDER_FIELD_ANNOTATION_ON_BODY_PROPERTY!>@Singular<!>
    val <!BUILDER_PROPERTY_NOT_INCLUDED!>singularVals<!>: List<String> = emptyList()

    val valWithoutField: Int
        get() = valWithoutDefault
    var varWithoutField: Int
        get() = varWithoutDefault
        set(arg) {
            varWithoutDefault = arg
        }

    fun test() {
        val correct = builder().varWithoutDefault(1).varWithDefault(2).singularVar("3")
        correct.<!UNRESOLVED_REFERENCE!>valWithoutDefault<!>(4)
        correct.<!UNRESOLVED_REFERENCE!>valWithDefault<!>(5)
        correct.<!UNRESOLVED_REFERENCE!>singularVal<!>("6")
    }
}

@Builder
class WithConstructor(var constructorProperty: Int) {
    var <!BUILDER_PROPERTY_NOT_INCLUDED!>varWithoutDefault<!>: Int = 0
    val <!BUILDER_PROPERTY_NOT_INCLUDED!>valWithoutDefault<!>: Int = 0
    @Builder.Default
    var <!BUILDER_PROPERTY_NOT_INCLUDED!>varWithDefault<!>: Int = 0
    <!BUILDER_FIELD_ANNOTATION_ON_BODY_PROPERTY!>@Builder.Default<!>
    val <!BUILDER_PROPERTY_NOT_INCLUDED!>valWithDefault<!>: Int = 0
    @Singular
    var <!BUILDER_PROPERTY_NOT_INCLUDED!>singularVars<!>: List<String> = emptyList()
    <!BUILDER_FIELD_ANNOTATION_ON_BODY_PROPERTY!>@Singular<!>
    val <!BUILDER_PROPERTY_NOT_INCLUDED!>singularVals<!>: List<String> = emptyList()

    val valWithoutField: Int
        get() = valWithoutDefault
    var varWithoutField: Int
        get() = varWithoutDefault
        set(arg) {
            varWithoutDefault = arg
        }

    fun test() {
        val correct = builder().constructorProperty(0)
        correct.<!UNRESOLVED_REFERENCE!>varWithoutDefault<!>(1)
        correct.<!UNRESOLVED_REFERENCE!>varWithDefault<!>(2)
        correct.<!UNRESOLVED_REFERENCE!>singularVal<!>("3")
        correct.<!UNRESOLVED_REFERENCE!>valWithoutDefault<!>(4)
        correct.<!UNRESOLVED_REFERENCE!>valWithDefault<!>(5)
        correct.<!UNRESOLVED_REFERENCE!>singularVal<!>("6")
    }
}
