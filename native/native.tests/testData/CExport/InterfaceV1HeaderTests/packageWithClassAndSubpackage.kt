// Regression fixture for the package-scope ordering of the K1 vs IR discovery modes.
// The setup is:
// package `foo`:
//     foo.kt (class Bar)
//     foo2.kt (class Bar2)
// package `foo.sub.sub2`:
//     foosub.kt (class Baz)
//
// There is NO declaration in the intermediate package `foo.sub` — it is empty. K2/IR emits no file for an empty
// package, so the IR discovery mode has to reconstruct `foo.sub` to keep the scope chain unbroken.
//
// We process a package one package fragment (source file) at a time, materializing a subpackage lazily the first
// time it is reached. So it emits `Bar` (first file's class), then descends through the empty `foo.sub` into
// `foo.sub.sub2` and emits `Baz`, then `Bar2` (second file's class).
// So the resulting order is the following:
// foo.Bar
// foo.sub.sub2.Baz
// foo.Bar2
//
// The `// FILE:` markers are required both because a package can be declared only once per source file and because
// the two `foo` files (fragments) are exactly what drives the interleaving.

// FILE: foo.kt
package foo

class Bar

// FILE: foo2.kt
package foo

class Bar2

// FILE: foosub.kt
package foo.sub.sub2

class Baz