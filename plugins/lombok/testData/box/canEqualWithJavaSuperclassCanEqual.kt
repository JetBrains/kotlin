// ISSUE: KT-89189

// FILE: JavaBase.java

public class JavaBase {
    protected boolean canEqual(Object other) {
        return other instanceof JavaBase;
    }
}

// FILE: test.kt

import lombok.EqualsAndHashCode

// Used to crash the compiler: the Java `canEqual` parameter type was not resolved yet when checking whether the
// generated `canEqual` overrides it. `JavaBase` inherits identity-based `equals`, so only reflexivity is asserted.
@EqualsAndHashCode(callSuper = true)
open class DerivedFromJavaWithCallSuper(val x: Int) : JavaBase()

// Without `callSuper`, `equals`/`hashCode` are based on `x` alone, so cross-instance behavior can be checked too.
@EqualsAndHashCode
open class DerivedFromJavaWithoutCallSuper(val x: Int) : JavaBase()

fun box(): String {
    val a = DerivedFromJavaWithCallSuper(1)
    if (a != a) return "FAIL"

    val b1 = DerivedFromJavaWithoutCallSuper(1)
    val b2 = DerivedFromJavaWithoutCallSuper(1)
    val b3 = DerivedFromJavaWithoutCallSuper(2)
    if (b1 != b2) return "FAIL"
    if (b1 == b3) return "FAIL"
    if (b1.hashCode() != b2.hashCode()) return "FAIL"

    return "OK"
}
