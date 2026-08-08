// WITH_STDLIB
// ISSUE: KT-87683

import lombok.Builder
import lombok.Singular

// The builder class name is inferred from the return type, so it has to be spelled out.
class ImplicitReturnType {
    <!BUILDER_REQUIRES_EXPLICIT_RETURN_TYPE!>@Builder<!>
    fun create(id: Int) = id.toString()
}

// No diagnostic: a block body without a declared type means `Unit`, so `UnitBuilder` is inferred from it,
// the same way Lombok infers `VoidBuilder` for a Java `void` method.
class UnitReturnType {
    @Builder
    fun init(id: Int) {
    }
}

// No diagnostic: there is nothing to infer, the builder class name is given explicitly.
class ImplicitReturnTypeWithBuilderClassName {
    @Builder(builderClassName = "ExplicitBuilder")
    fun create(id: Int) = id.toString()
}

// No diagnostic: the return type is explicit, so `StringBuilder` can be inferred from it.
class ExplicitReturnType {
    @Builder
    fun create(id: Int): String = id.toString()
}

class SingularCannotSingularize {
    @Builder
    fun accept(<!CANNOT_SINGULARIZE_NAME!>@Singular<!> sheep: List<String>) {
    }
}

class SingularUnsupportedType {
    @Builder
    fun accept(<!UNSUPPORTED_SINGULAR_TYPE!>@Singular("thing")<!> things: Array<String>) {
    }
}

class ParameterDefaultIgnored {
    @Builder
    fun init(id: Int = <!BUILDER_WILL_IGNORE_INITIALIZING_EXPRESSION!>0<!>) {
    }
}

// Every value parameter of a `@Builder` function becomes a builder field with a setter named after it, which
// neither an extension receiver nor a context parameter can provide a name for.
class ExtensionReceiver {
    <!BUILDER_WITH_RECEIVER_OR_CONTEXT_PARAMETERS!>@Builder<!>
    fun String.accept(x: Int): String = this
}

class ContextParameter {
    context(prefix: String)
    <!BUILDER_WITH_RECEIVER_OR_CONTEXT_PARAMETERS!>@Builder<!>
    fun accept(x: Int): String = prefix
}

// The same on a companion factory function.
class CompanionExtensionReceiver {
    companion object {
        <!BUILDER_WITH_RECEIVER_OR_CONTEXT_PARAMETERS!>@Builder<!>
        fun String.create(x: Int): String = this
    }
}
