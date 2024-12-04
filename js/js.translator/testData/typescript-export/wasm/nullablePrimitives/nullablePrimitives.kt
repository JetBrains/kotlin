// MODULE: main
// FILE: nullablePrimitives.kt

@JsExport
fun produceBoolean(): Boolean? = true

@JsExport
fun produceByte(): Byte? = Byte.MAX_VALUE

@JsExport
fun produceShort(): Short? = Short.MAX_VALUE

@JsExport
fun produceInt(): Int? = Int.MAX_VALUE

@JsExport
fun produceLong(): Long? = Long.MAX_VALUE

@JsExport
fun produceChar(): Char? = 'a'

@JsExport
fun produceString(): String? = "OK"

@JsExport
fun produceFunction(): (() -> Int)? = { 42 }

@JsExport
fun consumeBoolean(x: Boolean?): String? = x?.toString()

@JsExport
fun consumeByte(x: Byte?): String? = x?.toString()

@JsExport
fun consumeShort(x: Short?): String? = x?.toString()

@JsExport
fun consumeInt(x: Int?): String? = x?.toString()

@JsExport
fun consumeLong(x: Long?): String? = x?.toString()

@JsExport
fun consumeChar(x: Char?): String? = x?.toString()

@JsExport
fun consumeString(x: String?): String? = x

@JsExport
fun consumeFunction(fn: ((String) -> Int)?): Int? = fn?.invoke("42")
