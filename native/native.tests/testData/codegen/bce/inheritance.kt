/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import kotlin.reflect.KProperty
import kotlin.test.*

var needSmallArray = true

class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Array<Int> {
        return if (needSmallArray)
            Array(10) { 100 }
        else
            Array(100) { 100 }
    }
}

open class Base {
    open val array = Array(10) { 100 }
    val array1 by Delegate()
}

class Child : Base() {
    override val array: Array<Int>
        get() = if (needSmallArray)
            Array(10) { 100 }
        else
            Array(100) { 100 }
}

fun box(): String {
    val obj = Child()
    val base = Base()
    needSmallArray = false
    assertFailsWith<IndexOutOfBoundsException> {
        for (i in 0..obj.array.size-1) {
            needSmallArray = true
            obj.array[i] = 6
            needSmallArray = false
        }
    }

    needSmallArray = false
    assertFailsWith<IndexOutOfBoundsException> {
        for (i in 0..obj.array1.size-1) {
            needSmallArray = true
            obj.array1[i] = 6
            needSmallArray = false
        }
    }

    needSmallArray = false
    assertFailsWith<IndexOutOfBoundsException> {
        for (i in 0..obj.array.size-1) {
            needSmallArray = true
            base.array[i] = 6
            needSmallArray = false
        }
    }
    return "OK"
}
