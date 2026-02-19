// MODULE: main
// FILE: jsPrimitives.kt

@JsExport
fun produceBoolean(): JsBoolean = true.toJsBoolean()

@JsExport
fun produceNumber(): JsNumber = Int.MAX_VALUE.toJsNumber()

@JsExport
fun produceBigInt(): JsBigInt = Long.MAX_VALUE.toJsBigInt()

@JsExport
fun produceString(): JsString = "OK".toJsString()

@JsExport
fun produceAny(): JsAny = 42.toJsNumber()

@JsExport
fun consumeBoolean(x: JsBoolean): String = x.toBoolean().toString()

@JsExport
fun consumeNumber(x: JsNumber): String = x.toInt().toString()

@JsExport
fun consumeBigInt(x: JsBigInt): String = x.toLong().toString()

@JsExport
fun consumeString(x: JsString): String = x.toString()

@JsExport
fun consumeAny(x: JsAny): String = x.toString()