import kotlinx.cinterop.*
import kotlin.test.*
import kotlin.random.*

import cpptypes.*
/*
@Test
fun test_retByValue(k: Int) {
    memScoped {
        val x: CppTest = retByValue(k).getPointer(memScope).pointed
        assertEquals(k, x.get())
    }
}
*/

@Test
fun test_retByPtr(k: Int) {
    val x = interpretPointed<CppTest>(retByPtr(k).rawValue)
    assertEquals(k, x.get())
}

@Test
fun test_retByPtrConst(k: Int) {
    val x = interpretPointed<CppTest>(retByPtrConst(k).rawValue)
    assertEquals(k, x.get())
}

@Test
fun test_retByRef(k: Int) {
    val x = interpretPointed<CppTest>(retByRef(k).rawValue)
    assertEquals(k, x.get())
}

@Test
fun test_retByRefConst(k: Int) {
    val x = interpretPointed<CppTest>(retByRefConst(k).rawValue)
    assertEquals(k, x.get())
}
/*
@Test
fun test_paramByValue(k: Int) {
    val x = nativeHeap.alloc<CppTest>() {}
    CppTest.__init__(x.ptr, k)
    assertEquals(k, paramByValue(x.readValue()))
    nativeHeap.free(x)
}
*/
@Test
fun test_paramByPtr(k: Int) {
    val x = nativeHeap.alloc<CppTest>() {}
    CppTest.__init__(x.ptr, k)
    assertEquals(k, paramByPtr(x.ptr))
    nativeHeap.free(x)
}

@Test
fun test_paramByPtrConst(k: Int) {
    val x = nativeHeap.alloc<CppTest>() {}
    CppTest.__init__(x.ptr, k)
    assertEquals(k, paramByPtrConst(x.ptr))
    nativeHeap.free(x)
}

@Test
fun test_paramByRef(k: Int) {
    val x = nativeHeap.alloc<CppTest>() {}
    CppTest.__init__(x.ptr, k)
    assertEquals(k, paramByRef(x.ptr))
    nativeHeap.free(x)
}

@Test
fun test_paramByRefConst(k: Int) {
    val x = nativeHeap.alloc<CppTest>() {}
    CppTest.__init__(x.ptr, k)
    assertEquals(k, paramByRefConst(x.ptr))
    nativeHeap.free(x)
}

fun main() {
    val seed = Random.nextInt()
    val r = Random(seed)

    //test_retByValue(r.nextInt())
    test_retByPtr(r.nextInt())
    test_retByPtrConst(r.nextInt())
    test_retByRef(r.nextInt())
    test_retByRefConst(r.nextInt())
    //test_paramByValue(r.nextInt())
    test_paramByPtr(r.nextInt())
    test_paramByPtrConst(r.nextInt())
    test_paramByRef(r.nextInt())
    test_paramByRefConst(r.nextInt())
}
