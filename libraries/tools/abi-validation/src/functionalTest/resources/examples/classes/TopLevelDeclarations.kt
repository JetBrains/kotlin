/*
 * Copyright 2016-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package examples.classes

public fun testFun(): Int = 42
public fun <T> consume(arg: T) = Unit
public inline fun testInlineFun() = Unit
public const val con: String = "I'm a constant!"
public val l: Long = 0xc001
public var r: Float = 3.14f

public annotation class A
public interface I
public data class D(val x: Int)
public class C(public val v: Any) {
    public fun m() = Unit
}

public object O
public enum class E { A, B, C }
public abstract class AC {
    public abstract fun a()
    public fun b() = Unit
}
public open class OC {
    public open fun o(): Int = 42
    public fun c() = Unit
}
public class Outer {
    public class Nested {
        public inner class Inner {

        }
    }
}
