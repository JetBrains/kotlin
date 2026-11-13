// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-91320
// FIR_DUMP
// WITH_STDLIB

import lombok.*

// The factory `of(name: String)` clashes with the declared one, so it is not generated:
// `STATIC_CONSTRUCTOR_ALREADY_EXISTS` is expected here, but is missing.
@Data(staticConstructor = "of")
class ClashBySameArity(val name: String) {
    companion object {
        fun of(name: String): ClashBySameArity = ClashBySameArity(name + "!")
    }
}

// The factory `of(name: String)` is an overload of the declared `of()`, so it is generated: no warning is expected.
<!STATIC_CONSTRUCTOR_ALREADY_EXISTS!>@Data(staticConstructor = "of")<!>
class NoClashByDifferentArity(val name: String) {
    companion object {
        fun of(): NoClashByDifferentArity = NoClashByDifferentArity("default")
    }
}

// The same for a function declared in the class itself.
<!STATIC_CONSTRUCTOR_ALREADY_EXISTS!>@Data(staticConstructor = "of")<!>
class NoClashByDifferentArityInClass(val name: String) {
    fun of() {}
}

// An explicit constructor annotation replaces the one `@Data` implies, `staticConstructor` included,
// so `of` is never generated and no warning is expected.
@Data(staticConstructor = "of")
<!STATIC_CONSTRUCTOR_ALREADY_EXISTS!>@NoArgsConstructor(force = true)<!>
class DataWithNoArgs(val x: Int) {
    companion object {
        fun of(): DataWithNoArgs = DataWithNoArgs(1)
    }
}

// A vararg parameter: no factory, no warning expected.
<!STATIC_CONSTRUCTOR_ALREADY_EXISTS!>@Data(staticConstructor = "of")<!>
class DataWithVararg(vararg val items: String) {
    companion object {
        fun of(): DataWithVararg = DataWithVararg()
    }
}

// An object has no constructor to call, so nothing is generated and no warning is expected.
@Data(staticConstructor = "of")
object DataObject {
    fun of() {}
}

class WithDataCompanion {
    @Data(staticConstructor = "of")
    companion object {
        fun of() {}
    }
}

fun test() {
    ClashBySameArity.of("name")
    NoClashByDifferentArity.of("name")
    NoClashByDifferentArity.of()
    NoClashByDifferentArityInClass.of("name")
    DataWithNoArgs.of()
    DataWithVararg.of()
    DataObject.of()
    WithDataCompanion.of()
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, companionObject, functionDeclaration, integerLiteral,
objectDeclaration, outProjection, primaryConstructor, propertyDeclaration, stringLiteral, vararg */
