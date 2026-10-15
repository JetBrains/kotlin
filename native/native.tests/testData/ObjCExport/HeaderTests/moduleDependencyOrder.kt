// Regression fixture for ObjC-export file-class name mangling across several included libraries. It pins both
// halves of the module order: the part fixed by the dependency graph, and the part that falls back to the
// order the libraries were passed in.
//
// Four modules, all declaring `package foo`, and all holding a file named `Utils.kt`:
//     a : fun alphaUtil()     no dependencies
//     b : fun betaUtil()      declares a dependency on `a`, but references NOTHING from it
//     c : fun gammaUtil()     declares a dependency on `a` AND calls `alphaUtil()`
//     d : fun deltaUtil()     unrelated to all of them
//
// `ObjCExportNamerImpl.getFileClassName` keys an always-conflicting `GlobalNameMapping` on the *file name*
// alone, so all four `Utils.kt` collide, and the namer resolves the collision first-caller-wins: the first
// file it is asked about gets the clean `MDOUtilsKt`, the next `MDOUtilsKt_`, and so on. It is asked in module
// iteration order, because `makeFilesOrderStable` sorts by `SourceFile.name` only -- all four tie on it -- and
// the stable sort leaves them as the modules were iterated.
//
// The modules are declared, hence `-Xinclude`d, in the order `c`, `b`, `d`, `a`. The expected emission order
// is `b`, `d`, `a`, `c`, which is only explained by both rules together:
//
//   * `c` is included FIRST but emitted LAST. It imports `alphaUtil` from `a`, so there is a real edge in the
//     KLIB dependency graph and `a` has to precede it, whatever the command line says. This is the property
//     `KlibDAG.librariesReverseTopoSorted` exists for.
//
//   * `b` is emitted BEFORE `a`, even though its manifest says `depends=a`. `KlibDAGBuilder` derives edges
//     from imported signatures, not from the manifest, so a dependency that is declared but never used is not
//     an edge and imposes no order. Worth pinning deliberately: the manifest's `depends` property is on its
//     way out, so "declared" must not be mistaken for "ordered".
//
//   * `b`, `d` and `a` are emitted in their relative `-Xinclude` order. None of the three waits for anything,
//     so all are eligible from the start and are taken in load order.
//
// So the golden below changes if either rule breaks: losing the edge would move `c` back to the front, and
// losing the tie-break would scramble `b`, `d`, `a`.

// MODULE: c(a)
// FILE: Utils.kt
package foo

fun gammaUtil(): Int = alphaUtil() + 3

// MODULE: b(a)
// FILE: Utils.kt
package foo

// Note: deliberately does NOT reference anything from `a`, so that no dependency edge is recorded.
fun betaUtil(): Int = 2

// MODULE: d
// FILE: Utils.kt
package foo

fun deltaUtil(): Int = 4

// MODULE: a
// FILE: Utils.kt
package foo

fun alphaUtil(): Int = 1
