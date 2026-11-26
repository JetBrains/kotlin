// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-91320
// WITH_STDLIB
// Review of the PR around KT-91320: the clash check became an exact FIR type comparison (`equalWhenResolved`),
// while what actually clashes is the JVM signature. Before the PR, `of` with the same name & arity was a clash,
// so nothing below was generated. Now the factory is generated next to the declared function and
// the JVM backend meets two `of` methods with the same signature (`CONFLICTING_JVM_DECLARATIONS`) instead of
// the plugin skipping generation and reporting `STATIC_CONSTRUCTOR_ALREADY_EXISTS`.

import lombok.*

// String vs String?: the same JVM signature `of(Ljava/lang/String;)`
<!STATIC_CONSTRUCTOR_ALREADY_EXISTS!>@Data(staticConstructor = "of")<!>
class ClashByNullability(val name: String) {
    companion object {
        fun of(name: String?): ClashByNullability = ClashByNullability(name ?: "")
    }
}

// The class type parameter `T` vs the function type parameter `T`: never equal in FIR, the same erasure on JVM
<!STATIC_CONSTRUCTOR_ALREADY_EXISTS!>@Data(staticConstructor = "of")<!>
class ClashByTypeParameter<T>(val value: T) {
    companion object {
        fun <T> of(value: T): ClashByTypeParameter<T> = ClashByTypeParameter(value)
    }
}

// List<String> vs List<Int>: the same erasure
<!STATIC_CONSTRUCTOR_ALREADY_EXISTS!>@Data(staticConstructor = "of")<!>
class ClashByTypeArgument(val list: List<String>) {
    companion object {
        fun of(list: List<Int>): ClashByTypeArgument = ClashByTypeArgument(list.map { it.toString() })
    }
}

// The `@JvmStatic` factory also gets a static copy in the class itself, clashing with an instance `of(String?)`
<!STATIC_CONSTRUCTOR_ALREADY_EXISTS!>@Data(staticConstructor = "of")<!>
class ClashByNullabilityInClass(val name: String) {
    fun of(name: String?) {}
}

fun test() {
    ClashByNullability.of("name")
    ClashByTypeParameter.of(1)
    ClashByTypeArgument.of(listOf(1))
}
