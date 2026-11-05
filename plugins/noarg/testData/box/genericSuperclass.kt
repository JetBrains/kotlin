// WITH_STDLIB
// DUMP_KT_IR
// FILE: JavaBase.java

public class JavaBase<T> {
    public <U> JavaBase() {}
    public <U> JavaBase(U u) {}
}

// FILE: test.kt

annotation class NoArg

open class Base<T>
open class Base2<K, V>(val k: K? = null, val v: V? = null)

// The generated no-args constructor's super call must carry the supertype's type arguments: `<String>` here.
@NoArg class Test1(val b: String) : Base<String>()

// A super constructor taking only parameters with default values: `<Int, String>`.
@NoArg class Test2(val b: String) : Base2<Int, String>()

// A type argument referring to the class's own type parameter: `<List<E>>`.
@NoArg class Test3<E>(val e: E) : Base<List<E>>()

// A generic Java super constructor: `U` can't be inferred from no arguments, so `<String, Any?>`.
@NoArg class Test4(val b: String) : JavaBase<String>("")

fun box(): String {
    Test1::class.java.newInstance()
    val test2 = Test2::class.java.newInstance()
    if (test2.k != null || test2.v != null) return "fail@test2"
    Test3::class.java.newInstance()
    Test4::class.java.newInstance()
    return "OK"
}
