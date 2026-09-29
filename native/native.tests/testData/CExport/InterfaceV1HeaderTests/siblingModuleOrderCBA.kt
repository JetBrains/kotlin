// Fixture pinning C-export declaration order for libraries that do NOT depend on each other.
//
// The `CBA` suffix is the order the modules are declared in, hence the order they are `-Xinclude`d: c, b, a.
//
// Same three modules and same diamond as `siblingModuleOrderBCA.kt`:
//     a : class AlphaFromA        (no dependencies)
//     b : class BetaFromB         (uses AlphaFromA, so `b` depends on `a`)
//     c : class GammaFromC        (uses AlphaFromA, so `c` depends on `a`)
//
// The only difference is the declaration -- hence `-Xinclude` -- order: `c`, `b`, `a` instead of `b`, `c`, `a`.
//
//   * `a` is still emitted FIRST even though it is included LAST: the dependency-first rank from
//     `KlibDAG.librariesReverseTopoSorted` pins it, and swapping two libraries that both merely depend on `a`
//     cannot change that.
//
//   * `GammaFromC` now precedes `BetaFromB` -- the mirror image of `siblingModuleOrderBCA.kt`. `b` and `c` are
//     incomparable, so no reverse-topological rank can separate them, and the order falls back to the sequence
//     that seeds the topological sort, i.e. the command line.
//
// Read this file together with `siblingModuleOrderBCA.kt`: the pair is what makes the `-Xinclude`-order dependence
// of the sibling order explicit.

// MODULE: c(a)
// FILE: c.kt
package foo

class GammaFromC {
    fun useAlpha(): Int = AlphaFromA().fromA() + 2
}

// MODULE: b(a)
// FILE: b.kt
package foo

class BetaFromB {
    fun useAlpha(): Int = AlphaFromA().fromA() + 1
}

// MODULE: a
// FILE: a.kt
package foo

class AlphaFromA {
    fun fromA(): Int = 1
}
