// ISSUE: KT-90879
// WITH_STDLIB

import lombok.Builder
import lombok.Singular

@Builder
class WithoutConstructor {
    var varWithoutDefault: Int = 0
    val valWithoutDefault: Int = 0
    @Builder.Default
    var varWithDefault: Int = 0
    <!BUILDER_FIELD_ANNOTATION_ON_BODY_PROPERTY!>@Builder.Default<!>
    val valWithDefault: Int = 0
    @Singular
    var singularVars: List<String> = emptyList()
    <!BUILDER_FIELD_ANNOTATION_ON_BODY_PROPERTY!>@Singular<!>
    val singularVals: List<String> = emptyList()

    fun test() {
        val correct = builder().varWithoutDefault(1).varWithDefault(2).singularVar("3")
        correct.<!UNRESOLVED_REFERENCE!>valWithoutDefault<!>(4)
        correct.<!UNRESOLVED_REFERENCE!>valWithDefault<!>(5)
        correct.<!UNRESOLVED_REFERENCE!>singularVal<!>("6")
    }
}

@Builder
class WithConstructor(var constructorProperty: Int) {
    var varWithoutDefault: Int = 0
    val valWithoutDefault: Int = 0
    @Builder.Default
    var varWithDefault: Int = 0
    <!BUILDER_FIELD_ANNOTATION_ON_BODY_PROPERTY!>@Builder.Default<!>
    val valWithDefault: Int = 0
    @Singular
    var singularVars: List<String> = emptyList()
    <!BUILDER_FIELD_ANNOTATION_ON_BODY_PROPERTY!>@Singular<!>
    val singularVals: List<String> = emptyList()

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
