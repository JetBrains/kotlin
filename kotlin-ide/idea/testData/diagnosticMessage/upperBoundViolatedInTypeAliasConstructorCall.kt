// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: TYPE_INFERENCE_UPPER_BOUND_VIOLATED
// !MESSAGE_TYPE: TEXT
// !LANGUAGE: -NewInference

class Num<Tn : Number>(val x: Tn)
typealias N<T> = Num<T>

val test = N("")