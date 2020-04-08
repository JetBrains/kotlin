// "Import" "true"
// ERROR: Classifier 'ArrayList' does not have a companion object, and thus must be initialized here

// KT-4000

package testing

class Test {
    fun foo(a: Collection<String>) {

    }
}

fun test() {
    val t = Test()
    t.foo(<caret>ArrayList)
}