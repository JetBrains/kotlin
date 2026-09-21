// Regression fixture for C-export module-fragment ordering when several inter-dependent libraries are included
// in an order that contradicts their dependency order.
//
// Two modules both declare `package foo`:
//     a : class AlphaFromA        (no dependencies)
//     c : class GammaFromC        (uses AlphaFromA, so `c` depends on `a`)
//
// Both are `-Xinclude`d into one library, and `c` is declared -- hence included on the CLI -- BEFORE `a`, so the
// CLI include order contradicts the topological order. C export must emit the merged `foo` scope in a stable
// dependency-first (reverse-topological) order regardless of the CLI order, so `AlphaFromA` must precede
// `GammaFromC` in the golden. Emitting declarations in the raw `-Xinclude` CLI order instead would list
// `GammaFromC` first -- the regression this fixture guards against.

// MODULE: c(a)
// FILE: c.kt
package foo

class GammaFromC {
    fun useAlpha(): Int = AlphaFromA().fromA()
}

// MODULE: a
// FILE: a.kt
package foo

class AlphaFromA {
    fun fromA(): Int = 1
}
