// MODULE: main
// FILE: sample/ReifiedClash.kt
package sample

// `reified` is not part of the ID, so the inline/reified and the plain spellings collide;
// the compiler reports both as conflicting overloads.
inline fun <reified T> f(x: T) {}
fun <T> f(x: T) {}
