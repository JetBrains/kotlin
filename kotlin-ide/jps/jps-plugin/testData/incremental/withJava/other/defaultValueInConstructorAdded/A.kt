package foo

open class A(x: Int)

// The use of annotation here is intentional, so no change for "fun A" is detected,
// but after adding default value to A constructor, we want to force resolve to the constructor
@Deprecated("Warning", level = DeprecationLevel.WARNING)
fun A() = A(30)