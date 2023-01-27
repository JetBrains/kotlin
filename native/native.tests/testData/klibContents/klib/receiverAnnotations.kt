// KT-56218: Fix receiver annotations for properties
// MUTED_WHEN: K2
package test

annotation class Ann
@Ann fun @receiver:Ann Int.foo(@Ann arg: Int) = 10
@Ann val @receiver:Ann Int.bar
    get() = 5

class A {
    @Ann fun @receiver:Ann Int.foo(@Ann arg: Int) = 10
    @Ann val @receiver:Ann Int.bar
        get() = 5
}