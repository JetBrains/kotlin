// ERROR: Variable cannot be initialized before declaration
package demo

class C(a: Int) {
    init {
        abc = a * 2
    }

    var abc = 0
}