// TARGET_BACKEND: JS_IR
// ONLY_IR_DCE
// CHECK_OPTIMIZED_JS

object Pure { var x = 10 }

// FUNCTION_HAS_EFFECTS: function=write WRITE
fun write(): Int { Pure.x = 15; return 42 }
// FUNCTION_HAS_EFFECTS: function=read READ
fun read() = Pure.x

object Read {
    var y = read()
}

object Write {
    var z = write()
}

// FUNCTION_HAS_EFFECTS: function=pureRef PURE
// FUNCTION_HAS_EFFECTS: function=pureR READ
// FUNCTION_HAS_EFFECTS: function=pureW WRITE
// FUNCTION_HAS_EFFECTS: function=pureRW WRITE
fun pureRef() = Pure
fun pureR() = Pure.x
fun pureW() { Pure.x = 16 }
fun pureRW() { Pure.x -= 1 }

// FUNCTION_HAS_EFFECTS: function=readRef READ
// FUNCTION_HAS_EFFECTS: function=readR READ
// FUNCTION_HAS_EFFECTS: function=readW WRITE
// FUNCTION_HAS_EFFECTS: function=readRW WRITE
fun readRef() = Read
fun readR() = Read.y
fun readW() { Read.y = 16 }
fun readRW() { Read.y -= 1 }

// FUNCTION_HAS_EFFECTS: function=writeRef WRITE
// FUNCTION_HAS_EFFECTS: function=writeR WRITE
// FUNCTION_HAS_EFFECTS: function=writeW WRITE
// FUNCTION_HAS_EFFECTS: function=writeRW WRITE
fun writeRef() = Write
fun writeR() = Write.z
fun writeW() { Write.z = 16 }
fun writeRW() { Write.z -= 1 }

fun box(): String {
    pureRef()
    pureR()
    pureW()
    pureRW()
    readRef()
    readR()
    readW()
    readRW()
    writeRef()
    writeR()
    writeW()
    writeRW()
    return "OK"
}
