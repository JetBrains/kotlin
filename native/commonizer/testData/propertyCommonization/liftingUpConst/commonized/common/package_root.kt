const val property1 = 42
expect val property2: Int
expect val property3: Int
expect val property4: Int

const val property5: Byte = 42
expect val property6: Byte
const val property7: Short = 42
expect val property8: Short
const val property9: Long = 42
expect val property10: Long
const val property11: Double = 4.2
expect val property12: Double
const val property13: Float = 4.2f
expect val property14: Float
const val property15 = true
expect val property16: Boolean
const val property17 = "42"
expect val property18: String
const val property19: Char = 42.toChar()
expect val property20: Char

// Optimistic Number Commonization: KT-48455, KT-48568
// Mismatched const types should be commonized as expect val's
@kotlinx.cinterop.UnsafeNumber(["js: kotlin.Short", "jvm: kotlin.Byte"])
expect val property22: Byte
@kotlinx.cinterop.UnsafeNumber(["js: kotlin.Int", "jvm: kotlin.Short"])
expect val property23: Short
@kotlinx.cinterop.UnsafeNumber(["js: kotlin.Long", "jvm: kotlin.Int"])
expect val property24: Int
@kotlinx.cinterop.UnsafeNumber(["js: kotlin.Float", "jvm: kotlin.Double"])
expect val property26: Float