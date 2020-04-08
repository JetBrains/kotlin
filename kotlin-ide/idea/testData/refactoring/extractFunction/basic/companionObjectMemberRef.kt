// PARAM_DESCRIPTOR: value-parameter s: kotlin.String defined in Outer.O.f
// PARAM_TYPES: kotlin.String
class Outer {
    object O {
        fun f(s: String) {
            <selection>s.funFromCompanion()</selection>
        }
    }

    companion object {
        fun String.funFromCompanion(): String = ""
    }
}