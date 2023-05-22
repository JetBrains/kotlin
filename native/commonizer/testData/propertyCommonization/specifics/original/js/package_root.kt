val delegatedProperty1: Int by lazy { 42 }
val delegatedProperty2: Int by lazy { 42 }
val delegatedProperty3: Int by mapOf("delegatedProperty3" to 42)
val delegatedProperty4: Int by mapOf("delegatedProperty4" to 42)

lateinit var lateinitProperty1: String
lateinit var lateinitProperty2: String

inline val inlineProperty1 get() = 42
inline val inlineProperty2 get() = 42
inline val inlineProperty3 get() = 42

inline var inlineProperty4
    get() = 42
    set(value) = Unit
inline var inlineProperty5
    get() = 42
    set(value) = Unit
inline var inlineProperty6
    get() = 42
    set(value) = Unit
inline var inlineProperty7
    get() = 42
    set(value) = Unit
inline var inlineProperty8
    get() = 42
    set(value) = Unit

external val externalProperty1: Int
external val externalProperty2: Int

var externalSetGet: Int
    external get
    external set