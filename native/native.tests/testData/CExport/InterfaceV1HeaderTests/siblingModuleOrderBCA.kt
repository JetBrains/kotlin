// Fixture pinning C-export declaration order for libraries that do NOT depend on each other.
//
// The `BCA` suffix is the order the modules are declared in, hence the order they are `-Xinclude`d: b, c, a.
//
// Three modules all declare `package foo`, in a diamond:
//     a : class AlphaFromA        (no dependencies)
//     b : class BetaFromB         (uses AlphaFromA, so `b` depends on `a`)
//     c : class GammaFromC        (uses AlphaFromA, so `c` depends on `a`)
//
// `a` is topologically before both `b` and `c`, but `b` and `c` are INCOMPARABLE -- no dependency edge relates
// them. `moduleDependencyOrder.kt` covers the comparable case; this fixture covers the incomparable one, where
// the two orderings the exporter can choose from are equally valid topologically.
//
// The modules are declared -- hence `-Xinclude`d -- in the order `b`, `c`, `a`. Two separate properties are
// pinned here:
//
//   * `a` is emitted FIRST even though it is included LAST, because `CAdapterGenerator` ranks package fragments
//     by `KlibDAG.librariesReverseTopoSorted`, which is dependency-first. This is the same property
//     `moduleDependencyOrder.kt` guards.
//
//   * `BetaFromB` precedes `GammaFromC`, matching the order `b` and `c` were included on the CLI. A
//     reverse-topological rank cannot separate two libraries that no dependency edge relates, so for such a
//     pair the rank falls back to the order that seeds the topological sort, i.e. the command line.
//
// So the sibling order here IS `-Xinclude`-order-dependent: see `siblingModuleOrderCBA.kt`, which declares
// the same three modules as `c`, `b`, `a` and gets `GammaFromC` before `BetaFromB`. The pair of fixtures records
// that as the current, deliberate behaviour -- consistent with every Kotlin/Native release since 2.4.20-dev-7885
// -- so that any change to it shows up as a golden diff in both files rather than silently.

// MODULE: b(a)
// FILE: b.kt
package foo

class BetaFromB {
    fun useAlpha(): Int = AlphaFromA().fromA() + 1
}

// MODULE: c(a)
// FILE: c.kt
package foo

class GammaFromC {
    fun useAlpha(): Int = AlphaFromA().fromA() + 2
}

// MODULE: a
// FILE: a.kt
package foo

class AlphaFromA {
    fun fromA(): Int = 1
}
