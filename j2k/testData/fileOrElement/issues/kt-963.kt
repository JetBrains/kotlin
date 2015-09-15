// ERROR: Variable cannot be initialized before declaration
package demo

internal class C(a: Int) {
    init {
        abc = a * 2
    }

    internal var abc = 0
}