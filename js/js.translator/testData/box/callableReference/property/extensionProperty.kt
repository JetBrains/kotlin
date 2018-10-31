// EXPECTED_REACHABLE_NODES: 1300
package foo

import kotlin.reflect.KMutableProperty1

open class A(var msg:String) {
}

class B:A("FromB") {
}

var global:String = ""

var A.ext:String
    get() = ":A.ext ${this.msg}:"
    set(value) { global = ":A.ext ${value}" }

var B.ext:String
    get() = ":B.ext ${this.msg}:"
    set(value) { global = ":B.ext ${value}" }

fun box(): String {
    val a = A("Test")

    var refAExt = A::ext
    var refBExt: KMutableProperty1<B, String> = B::ext

    assertEquals("ext", refAExt.name)
    assertEquals("ext", refBExt.name)

    assertEquals(":A.ext Test:", refAExt.get(a))
    assertEquals(":B.ext FromB:", refBExt.get(B()))

    refAExt.set(a, "newA")
    assertEquals(":A.ext newA", global)

    global = ""
    refBExt.set(B(), "newB")
    assertEquals(":B.ext newB", global)

    return "OK"
}
