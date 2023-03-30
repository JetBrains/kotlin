/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(FreezingIsDeprecated::class, kotlin.native.runtime.NativeRuntimeApi::class)

package runtime.workers.freeze6

import kotlin.test.*
import kotlin.native.concurrent.*
import kotlin.native.ref.*

data class Hi(val s: String)
data class Nested(val hi: Hi)

@Test
fun ensureNeverFrozenNoFreezeChild(){
    val noFreeze = Hi("qwert")
    noFreeze.ensureNeverFrozen()

    val nested = Nested(noFreeze)
    assertFails { nested.freeze() }

    println("OK")
}

@Test
fun ensureNeverFrozenFailsTarget(){
    val noFreeze = Hi("qwert")
    noFreeze.ensureNeverFrozen()

    assertFalse(noFreeze.isFrozen)
    assertFails { noFreeze.freeze() }
    assertFalse(noFreeze.isFrozen)
    println("OK")
}

fun createRef1(): FreezableAtomicReference<Any?> {
    val ref = FreezableAtomicReference<Any?>(null)
    ref.value = ref
    ref.freeze()
    ref.value = null
    return ref
}

var global = 0

@Test
fun ensureFreezableHandlesCycles1() {
    val ref = createRef1()
    kotlin.native.runtime.GC.collect()

    val obj: Any = ref
    global = obj.hashCode()
}

class Node(var ref: Any?)

/**
 *    ref1 -> Node <- ref3
 *             |        /\
 *             V        |
 *             ref2  ---
 */
fun createRef2(): Pair<FreezableAtomicReference<Node?>, Any> {
    val ref1 = FreezableAtomicReference<Node?>(null)

    val node = Node(null)
    val ref3 = FreezableAtomicReference<Any?>(node)
    val ref2 = FreezableAtomicReference<Any?>(ref3)

    node.ref = ref2
    ref1.value = node

    ref1.freeze()
    ref3.value = null

    assertTrue(node.isFrozen)
    assertTrue(ref1.isFrozen)
    assertTrue(ref2.isFrozen)
    assertTrue(ref3.isFrozen)

    return ref1 to ref2
}

@Test
fun ensureFreezableHandlesCycles2() {
    val (ref, obj) = createRef2()
    kotlin.native.runtime.GC.collect()

    assertTrue(obj.toString().length > 0)
    global = ref.value!!.ref!!.hashCode()
}

fun createRef3(): FreezableAtomicReference<Any?> {
    val ref = FreezableAtomicReference<Any?>(null)
    val node = Node(ref)
    ref.value = node
    ref.freeze()

    assertTrue(node.isFrozen)
    assertTrue(ref.isFrozen)

    return ref
}

@Test
fun ensureFreezableHandlesCycles3() {
    val ref = createRef3()
    ref.value = null
    kotlin.native.runtime.GC.collect()

    val obj: Any = ref
    assertTrue(obj.toString().length > 0)
    global = obj.hashCode()
}

lateinit var weakRef: WeakReference<Any>

fun createRef4(): FreezableAtomicReference<Any?> {
    val ref = FreezableAtomicReference<Any?>(null)
    val node = Node(ref)
    weakRef = WeakReference(node)
    ref.value = node
    ref.freeze()
    assertTrue(weakRef.get() != null)
    return ref
}

@Test
fun ensureWeakRefNotLeaks1() {
    val ref = createRef4()
    ref.value = null
    // We cannot check weakRef.get() here, as value read will be stored in the stack slot,
    // and thus hold weak reference from release.
    kotlin.native.runtime.GC.collect()

    assertTrue(weakRef.get() == null)
}

lateinit var node1: Node
lateinit var weakNode2: WeakReference<Node>

fun createRef5() {
    val ref = FreezableAtomicReference<Any?>(null)
    node1 = Node(ref)
    val node2 = Node(node1)
    weakNode2 = WeakReference(node2)
    ref.value = node2
    node1.freeze()
    assertTrue(weakNode2.get() != null)
    ref.value = null
}

@Test
fun ensureWeakRefNotLeaks2() {
    createRef5()
    kotlin.native.runtime.GC.collect()
    assertTrue(weakNode2.get() == null)
}