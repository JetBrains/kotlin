package lib

class Outer {
    object Public : Private

    private interface Private: Secret.EffectivelyPrivate

    private object Secret {
        interface EffectivelyPrivate {
            fun result(): String = "OK"
        }
    }
}
