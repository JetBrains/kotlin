// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// FIR_DUMP

import lombok.*

// No companion object is declared, so whether the factory is generated decides whether a companion object is generated,
// and this is asked while the companion objects are being generated, before the parameter types of `of` are resolved

// The factory `of(name: String)` would be shadowed by the declared one, so it is not generated
<!STATIC_CONSTRUCTOR_ALREADY_EXISTS!>@Data(staticConstructor = "of")<!>
class ClashInClass(val name: String) {
    fun of(name: String) {}
}

// The factory `of(name: String)` is an overload of the declared `of(name: Int)`, so it is generated
@Data(staticConstructor = "of")
class NoClashByDifferentTypesInClass(val name: String) {
    fun of(name: Int) {}
}

fun test() {
    NoClashByDifferentTypesInClass.of("name")
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, primaryConstructor, propertyDeclaration, stringLiteral */
