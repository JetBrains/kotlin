package test

// Doesn't mention V, yet the layout of Sub includes the unboxed field of Holder.
class Sub : Holder()

fun makeSub(): Holder = Sub()
