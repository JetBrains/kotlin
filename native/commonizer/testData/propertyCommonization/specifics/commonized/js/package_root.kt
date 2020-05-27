actual val delegatedProperty1: Int by lazy { 42 }
actual val delegatedProperty2: Int by lazy { 42 }
actual val delegatedProperty3: Int by mapOf("delegatedProperty3" to 42)
actual val delegatedProperty4: Int by mapOf("delegatedProperty4" to 42)

lateinit var lateinitProperty1: String
lateinit var lateinitProperty2: String

actual inline val inlineProperty1 get() = 42
actual inline val inlineProperty2 get() = 42
actual inline val inlineProperty3 get() = 42

actual inline var inlineProperty4
    get() = 42
    set(value) = Unit
actual inline var inlineProperty5
    get() = 42
    set(value) = Unit
actual inline var inlineProperty6
    get() = 42
    set(value) = Unit
actual inline var inlineProperty7
    get() = 42
    set(value) = Unit
actual inline var inlineProperty8
    get() = 42
    set(value) = Unit

actual external val externalProperty1: Int
actual external val externalProperty2: Int
