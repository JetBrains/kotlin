// ISSUE: KT-89189

// FILE: JavaBase.java

public class JavaBase {
    protected boolean canEqual(Object other) {
        return other instanceof JavaBase;
    }
}

// FILE: test.kt

import lombok.EqualsAndHashCode

// `callSuper = true` here used to crash the compiler: deciding whether the generated `canEqual` needs `override`
// walks the whole ancestor chain, and `JavaBase`'s own `canEqual(Object other)` still had an unresolved Java
// parameter type at that point ("Unexpected returnTypeRef. Expected is FirResolvedTypeRef, but was
// FirJavaTypeRef"). `JavaBase` doesn't override `equals`/`hashCode` itself, so with `callSuper = true` the
// generated ones chain to identity-based `Object` semantics - expected, and unrelated to `canEqual` - so only
// reflexive equality is asserted here.
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
