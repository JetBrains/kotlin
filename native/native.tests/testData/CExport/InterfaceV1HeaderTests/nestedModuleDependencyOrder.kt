// Regression fixture for C-export declaration ordering inside a NESTED package shared by several inter-dependent
// libraries included in an order that contradicts their dependency order.
//
// Two modules both contribute to `foo` (a direct subpackage of the root) AND to the nested `foo.bar`:
//     a : foo.AlphaFromA, foo.bar.AlphaBarFromA        (no dependencies)
//     c : foo.GammaFromC, foo.bar.GammaBarFromC        (uses `a`, so `c` depends on `a`)
//
// Both are `-Xinclude`d into one library, and `c` is declared -- hence included on the CLI -- BEFORE `a`, so the
// CLI include order contradicts the topological order. C export must emit every merged package scope in a stable
// dependency-first (reverse-topological) order regardless of the CLI order, so `AlphaFromA` must precede
// `GammaFromC` in `foo` AND `AlphaBarFromA` must precede `GammaBarFromC` in the nested `foo.bar`. Emitting
// declarations in the raw `-Xinclude` CLI order instead would list the `GammaFrom*` classes first -- the regression
// this fixture guards against. The nested `foo.bar` scope is reached through a different traversal path than the
// top-level `foo` scope, so it needs the same dependency-first ordering.

// MODULE: c(a)
// FILE: c_foo.kt
package foo

class GammaFromC {
    fun useAlpha(): Int = AlphaFromA().fromA()
}

// FILE: c_bar.kt
package foo.bar

class GammaBarFromC {
    fun useAlphaBar(): Int = AlphaBarFromA().barFromA()
}

// MODULE: a
// FILE: a_foo.kt
package foo

class AlphaFromA {
    fun fromA(): Int = 1
}

// FILE: a_bar.kt
package foo.bar

class AlphaBarFromA {
    fun barFromA(): Int = 10
}
