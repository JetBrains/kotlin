// TARGET_BACKEND: JS_IR
// ONLY_IR_DCE
// CHECK_OPTIMIZED_JS

object Global { var x = 10 }

// FUNCTION_HAS_EFFECTS: function=write WRITE
fun write(): Int { Global.x += 15; return Global.x }
// FUNCTION_HAS_EFFECTS: function=read READ
fun read() = Global.x
// FUNCTION_HAS_EFFECTS: function=pure PURE
fun pure() = 5

// FUNCTION_HAS_EFFECTS: function=withConst PURE
// FUNCTION_HAS_EFFECTS: function=withPure PURE
// FUNCTION_HAS_EFFECTS: function=withRead READ
// FUNCTION_HAS_EFFECTS: function=withWrite WRITE
fun withConst(n: Int = 10) = n
fun withPure(n: Int = pure()) = n
fun withRead(n: Int = read()) = n
fun withWrite(n: Int = write()) = n

// FUNCTION_HAS_EFFECTS: function=defaultWithConst PURE
// FUNCTION_HAS_EFFECTS: function=constWithConst PURE
fun defaultWithConst() = withConst()
fun constWithConst() = withConst(5)

// FUNCTION_HAS_EFFECTS: function=defaultWithPure PURE
// FUNCTION_HAS_EFFECTS: function=constWithPure PURE
fun defaultWithPure() = withPure()
fun constWithPure() = withPure(5)

// FUNCTION_HAS_EFFECTS: function=defaultWithRead READ
// FUNCTION_HAS_EFFECTS: function=constWithRead READ
fun defaultWithRead() = withRead()
fun constWithRead() = withRead(5)

// FUNCTION_HAS_EFFECTS: function=defaultWithWrite WRITE
// FUNCTION_HAS_EFFECTS: function=constWithWrite WRITE
fun defaultWithWrite() = withWrite()
fun constWithWrite() = withWrite(5)

fun box(): String {
    defaultWithConst()
    constWithConst()
    defaultWithPure()
    constWithPure()
    defaultWithRead()
    constWithRead()
    defaultWithWrite()
    constWithWrite()
    return "OK"
}
