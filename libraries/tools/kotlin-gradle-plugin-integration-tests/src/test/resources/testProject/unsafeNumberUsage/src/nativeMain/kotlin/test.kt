import kotlinx.cinterop.UnsafeNumber

@UnsafeNumber([])
typealias UnsafeTypeAlias = Int

@UnsafeNumber([])
fun useUnsafeNumberOnFunction(): Int = TODO()

@UnsafeNumber([])
val useUnsafeNumberOnProperty: Int
    get() = TODO()
