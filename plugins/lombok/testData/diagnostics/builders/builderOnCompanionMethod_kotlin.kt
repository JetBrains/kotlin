// WITH_STDLIB
// ISSUE: KT-87683

import lombok.Builder
import lombok.Singular

// The builder class name is inferred from the return type, so it has to be spelled out.
class ImplicitReturnType private constructor(val id: Int) {
    companion object {
        <!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
        fun create(id: Int) = ImplicitReturnType(id)
    }
}

// No diagnostic: a block body without a declared type means `Unit`, so `UnitBuilder` is inferred from it,
// the same way Lombok infers `VoidBuilder` for a Java `void` method.
class UnitReturnType {
    companion object {
        <!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
        fun create(id: Int) {
        }
    }
}

// No diagnostic: there is nothing to infer, the builder class name is given explicitly.
class ImplicitReturnTypeWithBuilderClassName private constructor(val id: Int) {
    companion object {
        <!ANNOTATION_IS_NOT_SUPPORTED!>@Builder(builderClassName = "ExplicitBuilder")<!>
        fun create(id: Int) = ImplicitReturnTypeWithBuilderClassName(id)
    }
}

// No diagnostic: the return type is explicit, so `IdHolderBuilder` can be inferred from it.
class IdHolder private constructor(val id: Int) {
    companion object {
        <!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
        fun create(id: Int): IdHolder = IdHolder(id)
    }
}

class CompanionSingularCannotSingularize private constructor(val sheep: List<String>) {
    companion object {
        <!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
        fun create(@Singular sheep: List<String>): CompanionSingularCannotSingularize =
            CompanionSingularCannotSingularize(sheep)
    }
}

class CompanionSingularUnsupportedType private constructor(val things: Array<String>) {
    companion object {
        <!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
        fun create(@Singular("thing") things: Array<String>): CompanionSingularUnsupportedType =
            CompanionSingularUnsupportedType(things)
    }
}

class CompanionParameterDefaultIgnored private constructor(val id: Int) {
    companion object {
        <!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
        fun create(id: Int = 0): CompanionParameterDefaultIgnored =
            CompanionParameterDefaultIgnored(id)
    }
}
