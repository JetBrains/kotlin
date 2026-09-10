// ISSUE: KT-88659
// FIR_DUMP
// DUMP_KT_IR

import lombok.NoArgsConstructor

// The KT-88659 repro: the property initializer references a non-property constructor parameter. fir2ir used to
// inline such initializers into the generated constructor, where `input` is unbound, and the JVM backend
// crashed with "No mapping for symbol". The body is now built in the IR backend without an
// `IrInstanceInitializerCall`, the way the noarg plugin builds its constructors: the generated constructor only
// calls the superclass one, and initializers do not run in it at all.
@NoArgsConstructor
class WithReferencedParameter(input: Int) {
    var inputValue: Int? = input
}

// Initializers and `init` blocks do not run in the generated constructor - every field keeps its JVM default.
// Lombok's Java output differs, with `javac` inlining field initializers into every constructor, but a Java
// field initializer cannot reference constructor parameters, so this class shape has no Java ground truth;
// the noarg plugin's semantics are followed instead.
@NoArgsConstructor(force = true)
class WithInitializers(val text: String) {
    val initialized: Int = 5
    var fromInitBlock: String? = null

    init {
        fromInitBlock = "init:$text"
    }
}

fun box(): String {
    val withReferencedParameter = WithReferencedParameter()
    assertEquals(null, withReferencedParameter.inputValue)

    val withInitializers = WithInitializers()
    assertEquals(null, withInitializers.text)
    assertEquals(0, withInitializers.initialized)
    assertEquals(null, withInitializers.fromInitBlock)

    return "OK"
}
