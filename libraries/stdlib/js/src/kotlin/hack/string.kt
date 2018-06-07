package kotlin.text

@kotlin.internal.InlineOnly
public inline fun String.toByteArray(): ByteArray = asDynamic().toInt8Array()

@kotlin.internal.InlineOnly
public inline fun String(bytes: ByteArray): String {
    return js("String.fromUTF8Array").call(String, bytes)
}