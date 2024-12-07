class Receiver()

class Scope() {
    fun <T : String> Receiver.testOverload(e: T) = "String"
    fun <T : CharSequence> Receiver.testOverload(e: T) = "CharSequence"
    fun <T : Any> Receiver.testOverload(e: T) = "Any"
}

class NullableScope() {
    fun <T : String?> Receiver.testOverload(e: T) = "String?"
    fun <T : String> Receiver.testOverload(e: T) = "String"
    fun <T : CharSequence> Receiver.testOverload(e: T) = "CharSequence"
    fun <T : Any?> Receiver.testOverload(e: T) = "Any?"
}

fun box(): String {
    val stringVal: String = "Stirng value"
    val charSequenceVal: CharSequence = "CharSequence value"
    val anyVal: Any = "Any value"

    val r = Receiver()

    Scope().apply {
        assertEquals("String", r.testOverload(stringVal))
        assertEquals("CharSequence", r.testOverload(charSequenceVal))
        assertEquals("Any", r.testOverload(anyVal))
    }

    val stringOrNullVal: String? = "Stirng? value"
    val charSequenceOrNullVal: CharSequence? = "CharSequence? value"
    val anyOrNullVal: Any? = "Any? value"

    NullableScope().apply {
        assertEquals("String", r.testOverload(stringVal))
        assertEquals("String?", r.testOverload(stringOrNullVal))
        assertEquals("CharSequence", r.testOverload(charSequenceVal))
        assertEquals("Any?", r.testOverload(charSequenceOrNullVal))
        assertEquals("Any?", r.testOverload(anyVal))
        assertEquals("Any?", r.testOverload(anyOrNullVal))
    }
    return "OK"
}
