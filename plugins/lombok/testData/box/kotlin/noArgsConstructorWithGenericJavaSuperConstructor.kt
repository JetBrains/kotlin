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

import lombok.NoArgsConstructor
import kotlin.test.assertEquals

// The superclass's no-args constructor is generic on its own: its type parameter `U` can't be inferred
// from no arguments, so the generated super call gets its erased upper bound `Any?` for it,
// after `String` for the class's `T`.
@NoArgsConstructor
class Derived(var x: Int) : JavaBase<String>("")

// A bounded `U`: its erased upper bound is `Number?`.
@NoArgsConstructor
class DerivedNumber(var x: Int) : JavaNumberBase<String>(1)

// A recursively bounded `U`: its erased upper bound is `Comparable<*>?`.
@NoArgsConstructor
class DerivedComparable(var x: Int) : JavaComparableBase<String>(1)

fun box(): String {
    assertEquals(0, Derived().x)
    assertEquals(0, DerivedNumber().x)
    assertEquals(0, DerivedComparable().x)
    return "OK"
}
