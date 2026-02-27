// MODULE: main
// FILE: varianceAnnotations.kt

external class Covariant<out T: JsAny> : JsAny {
    val value: T
}

external class Contravariant<in T: JsAny> : JsAny {
    fun consume(value: T)
}

external class Invariant<T: JsAny> : JsAny {
    var value: T
}

@JsExport
fun acceptCovariant(a: Covariant<JsString>) {}

@JsExport
fun acceptContravariant(a: Contravariant<JsAny>) {}

@JsExport
fun acceptInvariant(a: Invariant<JsNumber>) {}