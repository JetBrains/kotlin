/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package examples.classes

public fun testFun(): Int = 42
public fun <T> consume(arg: T) = Unit
public inline fun testInlineFun() = Unit
public const val con: String = "I'm a constant!"
public const val intCon: Int = 42
public val l: Long = 0xc001
public val i: Int = 0xc002
public var r: Float = 3.14f
public var d: Double = 3.14
public val a = Any()

public annotation class A
public annotation class AA
public annotation class AAA

public interface I
public interface II
public fun interface FI {
    fun a(): Unit
}

public data class D(val x: Int)
public class C(public val v: Any) {
    public fun m() = Unit
}

public class IC : II

public object O
public object OO

public enum class E { A, B, C }
public enum class EE { AA, BB, CC }

public abstract class AC {
    public abstract fun a()
    public fun b() = Unit
}

public open class OC {
    constructor(i: Int)
    constructor(l: Long)
    constructor(s: String)

    public var x: Int = 1
    public var y: Int = 2
    public var z: Int = 3
    public val ix: Int = 4
    public val iy: Long = 5L
    public val iz: String = ""
    public open fun o(): Int = 42
    public fun c() = Unit
}

public class Outer {
    public class Nested {
        public inner class Inner {

        }

        public inner class YetAnotherInner {

        }

        enum class NE { A, B, C }
    }
}
