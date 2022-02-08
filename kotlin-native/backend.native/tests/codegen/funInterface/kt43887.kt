/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.funInterface.kt43887

import kotlin.test.*

import kotlinx.cinterop.*
typealias heap_t = IntVar
fun heap_create(size: Long): CPointer<heap_t>? = null
fun heap_alloc(heap: CPointer<heap_t>?, size: ULong): CPointer<*>? = null
fun heap_free(heap: CPointer<heap_t>?, ptr: CPointer<*>?): CPointer<BooleanVar>? = null
inline fun Heap(size: Long): CPointer<heap_t>? {
    return heap_create(size)
}
inline fun <reified T : CVariable> CPointer<heap_t>?.use(f: (CPointer<T>) -> Unit) {
    alloc<T>()
            ?.also(f)
            ?.also(::free)
}
inline fun <reified T : CVariable> CPointer<heap_t>?.alloc(): CPointer<T>? {
    return heap_alloc(this, sizeOf<T>().toULong())?.reinterpret()
}
inline fun CPointer<heap_t>?.free(ptr: CPointer<*>?): Boolean {
    return heap_free(this, ptr)?.pointed?.value ?: false
}

@Test
fun runTest(): Unit = memScoped {
    val heap = Heap(1024)

    heap.use<IntVar> { ptr ->
        ptr.pointed.value = 40
        //println("PTR ${ptr.pointed}")
    }
}
