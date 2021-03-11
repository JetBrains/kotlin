// FIR_COMPARISON
// we disabled parameter name/type completion because not all types are acceptable and it's not very useful in such context anyway

class FooBar

class Boo

annotation class A(val b<caret>)

// NUMBER: 0
