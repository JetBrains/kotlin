/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

private object NotEmptyMap : MutableMap<Any, Int> {
    override fun containsKey(key: Any): Boolean = true
    override fun containsValue(value: Int): Boolean = true

    // non-special bridges get(Object)Integer -> get(Object)I
    override fun get(key: Any): Int = 1
    override fun remove(key: Any): Int = 1

    override val size: Int get() = 0
    override fun isEmpty(): Boolean = true
    override fun put(key: Any, value: Int): Int? = throw UnsupportedOperationException()
    override fun putAll(from: Map<out Any, Int>): Unit = throw UnsupportedOperationException()
    override fun clear(): Unit = throw UnsupportedOperationException()
    override val entries: MutableSet<MutableMap.MutableEntry<Any, Int>> get() = null!!
    override val keys: MutableSet<Any> get() = null!!
    override val values: MutableCollection<Int> get() = null!!
}

fun box(): String {
    val n = NotEmptyMap as MutableMap<Any?, Any?>

    if (n.get(null) != null) return "FAIL 1: $n"
    if (n.containsKey(null)) return "FAIL 2: $n"
    if (n.containsValue(null)) return "FAIL 3: $n"
    if (n.remove(null) != null) return "FAIL 4: $n"

    if (n.get(1) == null) return "FAIL 5: $n"
    if (!n.containsKey("")) return "FAIL 6: $n"
    if (!n.containsValue(3)) return "FAIL 7: $n"
    if (n.remove("") == null) return "FAIL 8: $n"

    return "OK"
}
