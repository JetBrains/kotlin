import kotlinx.cinterop.*
import kotlin.test.*

import skia.*

fun hello() {
    memScoped {
        val cString = "Hello Skia".cstr.getPointer(memScope)
        val s = alloc<SkString>() {
            SkString.__init__(ptr, cString)
        }
        println(s.c_str()?.toKString())
    }
}

fun SkString_create(from: String): SkString {
    val res = nativeHeap.alloc<SkString>() {}
    memScoped {
        val cString = from.cstr.getPointer(memScope)
        SkString.__init__(res.ptr, cString)
    }
    return res
}

fun SkString.clone(): SkString {
    return nativeHeap.alloc<SkString>() {
        SkString.__init__(ptr, this@clone.ptr)
    }
}

fun SkString.delete(): Unit {
    SkString.__destroy__(this.ptr)
    nativeHeap.free(this)
}

fun SkString.toKString() = this.c_str()!!.toKString()


fun main() {

    hello()

    /*
    with (CppContext) {
        val go = SkString("Let's go fishing!")
        val goback = SkString(go)
        val pos = goback.find("fish")
        println(go)
        if (pos >= 0) {
            goback.insert(pos, "back!")
            goback.resize((pos + "back!".length)
        }
        println(goback)
        oback.swap(go)
        goback.resize(goback.size() - 1U)
        goback.append(" again!")
        println(goback)
    }
    */
    val go = SkString_create("Let's go fishing!")
    println(go.toKString())

    val goback = go.clone()
    memScoped {
        val pos = goback.find("fish".cstr.getPointer(memScope))
        if (pos >= 0) {
            goback.insert(pos.convert<ULong>(), "back!".cstr.getPointer(memScope))
            goback.resize((pos + "back!".length).convert<ULong>())
        }
        println(goback.toKString())

        goback.swap(go.ptr)

        goback.resize(goback.size() - 1U)
        goback.append(" again!".cstr.getPointer(memScope))
        println(goback.toKString())
    }
    go.delete()
    goback.delete()
}