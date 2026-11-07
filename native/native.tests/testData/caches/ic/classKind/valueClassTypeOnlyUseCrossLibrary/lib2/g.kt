package test

// Wrapped appears here only as a type. The inlined `let` is what makes the value-class world
// record a lib1|w.kt edge; the plain-class world records no edge for this file.
fun pass(x: Wrapped): Wrapped = x.let { x }
