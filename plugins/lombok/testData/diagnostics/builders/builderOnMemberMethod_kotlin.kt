// WITH_STDLIB
// ISSUE: KT-87683

import lombok.Builder
import lombok.Singular

// The builder class name is inferred from the return type, so it has to be spelled out.
class ImplicitReturnType {
    <!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
    fun create(id: Int) = id.toString()
}

// No diagnostic: a block body without a declared type means `Unit`, so `UnitBuilder` is inferred from it,
// the same way Lombok infers `VoidBuilder` for a Java `void` method.
class UnitReturnType {
    <!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
    fun init(id: Int) {
    }
}

// No diagnostic: there is nothing to infer, the builder class name is given explicitly.
class ImplicitReturnTypeWithBuilderClassName {
    <!ANNOTATION_IS_NOT_SUPPORTED!>@Builder(builderClassName = "ExplicitBuilder")<!>
    fun create(id: Int) = id.toString()
}

// No diagnostic: the return type is explicit, so `StringBuilder` can be inferred from it.
class ExplicitReturnType {
    <!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
    fun create(id: Int): String = id.toString()
}

class SingularCannotSingularize {
    <!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
    fun accept(@Singular sheep: List<String>) {
    }
}

class SingularUnsupportedType {
    <!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
    fun accept(@Singular("thing") things: Array<String>) {
    }
}

class ParameterDefaultIgnored {
    <!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
    fun init(id: Int = 0) {
    }
}

// Every value parameter of a `@Builder` function becomes a builder field with a setter named after it. An
// extension receiver has no name to derive one from, and a context parameter, which usually does, could just as
// reasonably become a builder field, a context parameter of `builder()`, or one of `build()`. Both are unique to
// Kotlin and unmodelled by Lombok, so neither is supported rather than picking one of those meanings.
class ExtensionReceiver {
    <!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
    fun String.accept(x: Int): String = this
}

class ContextParameter {
    context(prefix: String)
    <!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
    fun accept(x: Int): String = prefix
}

// The same on a companion factory function.
class CompanionExtensionReceiver {
    companion object {
        <!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
        fun String.create(x: Int): String = this
    }
}
