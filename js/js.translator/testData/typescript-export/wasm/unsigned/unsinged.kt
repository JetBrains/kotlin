// MODULE: main
// FILE: unsigned.kt

@JsExport
fun produceUByte(): UByte = UByte.MAX_VALUE

@JsExport
fun produceUShort(): UShort = UShort.MAX_VALUE

@JsExport
fun produceUInt(): UInt = UInt.MAX_VALUE

@JsExport
fun produceULong(): ULong = ULong.MAX_VALUE

@JsExport
fun produceFunction(): () -> UInt = ::produceUInt

@JsExport
fun consumeUByte(x: UByte): String = x.toString()

@JsExport
fun consumeUShort(x: UShort): String = x.toString()

@JsExport
fun consumeUInt(x: UInt): String = x.toString()

@JsExport
fun consumeULong(x: ULong): String = x.toString()

@JsExport
fun consumeFunction(fn: (String) -> UInt): UInt = fn("42")
