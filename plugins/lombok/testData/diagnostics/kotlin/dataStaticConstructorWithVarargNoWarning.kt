// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// Review of the PR around KT-91320: for a class with a vararg primary constructor parameter no factory is generated
// (`RequiredArgsConstructorGeneratorPart.getKotlinConstructorInfo` returns null), so no warning is expected.
// But `primaryConstructorParameters()` no longer filters vararg classes out, so `FirLombokConstructorsChecker`
// (via `Data.asRequiredArgsConstructor(...).matchesGivenFunction`) now matches `of(vararg items: String)`
// and reports a false positive `STATIC_CONSTRUCTOR_ALREADY_EXISTS`.

import lombok.*

@Data(staticConstructor = "of")
class DataWithVararg(vararg val items: String) {
    companion object {
        fun of(vararg items: String): DataWithVararg = DataWithVararg(*items)
    }
}

fun test() {
    DataWithVararg.of("a", "b")
}
