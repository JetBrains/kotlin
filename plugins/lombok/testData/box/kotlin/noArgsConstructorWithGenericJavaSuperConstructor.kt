// DUMP_KT_IR
// FILE: JavaBase.java

public class JavaBase<T> {
    public <U> JavaBase() {}
    public <U> JavaBase(U u) {}
}

// FILE: test.kt

import lombok.NoArgsConstructor
import kotlin.test.assertEquals

// The superclass's no-args constructor is generic on its own: its type parameter `U` can't be inferred
// from no arguments, so the generated super call gets `Any?` for it, after `String` for the class's `T`.
@NoArgsConstructor
class Derived(var x: Int) : JavaBase<String>("")

fun box(): String {
    assertEquals(0, Derived().x)
    return "OK"
}
