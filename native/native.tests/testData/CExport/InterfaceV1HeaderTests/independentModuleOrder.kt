// Regression fixture for C-export module-fragment ordering when several included libraries do not depend on each
// other.
//
// Eight modules `l1`..`l8` declare `package foo` and do not depend on each other; `hub` depends on all of them.
// The topological order leaves the relative order of `l1`..`l8` open, and C export must still emit the merged `foo`
// scope in the same order on every run and for every order of the inputs: by unique name, `Lib1Type` to `Lib8Type`,
// although the modules are declared and passed in a different order. Taking the order of a hash set of dependencies
// made it change between runs of the same compilation.

// MODULE: hub(l5, l2, l8, l1, l7, l3, l6, l4)
// FILE: hub.kt
package hub

fun hub(): Int =
    foo.Lib1Type().value + foo.Lib2Type().value + foo.Lib3Type().value + foo.Lib4Type().value +
            foo.Lib5Type().value + foo.Lib6Type().value + foo.Lib7Type().value + foo.Lib8Type().value

// MODULE: l5
// FILE: l5.kt
package foo

class Lib5Type(val value: Int = 5)

// MODULE: l2
// FILE: l2.kt
package foo

class Lib2Type(val value: Int = 2)

// MODULE: l8
// FILE: l8.kt
package foo

class Lib8Type(val value: Int = 8)

// MODULE: l1
// FILE: l1.kt
package foo

class Lib1Type(val value: Int = 1)

// MODULE: l7
// FILE: l7.kt
package foo

class Lib7Type(val value: Int = 7)

// MODULE: l3
// FILE: l3.kt
package foo

class Lib3Type(val value: Int = 3)

// MODULE: l6
// FILE: l6.kt
package foo

class Lib6Type(val value: Int = 6)

// MODULE: l4
// FILE: l4.kt
package foo

class Lib4Type(val value: Int = 4)
