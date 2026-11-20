// WITH_STDLIB
// DUMP_KT_IR
// FILE: JavaBase.java

public class JavaBase<T> {
    public <U> JavaBase() {}
    public <U> JavaBase(U u) {}
}

// FILE: JavaNumberBase.java

public class JavaNumberBase<T> {
    public <U extends Number> JavaNumberBase() {}
    public <U extends Number> JavaNumberBase(U u) {}
}

// FILE: JavaComparableBase.java

public class JavaComparableBase<T> {
    public <U extends Comparable<U>> JavaComparableBase() {}
    public <U extends Comparable<U>> JavaComparableBase(U u) {}
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

// A generic Java super constructor: `U` can't be inferred from no arguments, so it gets its erased upper bound,
// `<String, Any?>` here.
@NoArg class Test4(val b: String) : JavaBase<String>("")

// A bounded `U`: `<String, Number?>`.
@NoArg class Test5(val b: String) : JavaNumberBase<String>(1)

// A recursively bounded `U`: `<String, Comparable<*>?>`.
@NoArg class Test6(val b: String) : JavaComparableBase<String>(1)

fun box(): String {
    Test1::class.java.newInstance()
    val test2 = Test2::class.java.newInstance()
    if (test2.k != null || test2.v != null) return "fail@test2"
    Test3::class.java.newInstance()
    Test4::class.java.newInstance()
    Test5::class.java.newInstance()
    Test6::class.java.newInstance()
    return "OK"
}
