package foo

import js.*

/*function g should return 0 if a parameter is undefined 1 if parameter is 1 and 3 if parameter is 3*/
native
fun f(g: (Int?) -> Int): Boolean = js.noImpl


fun  box(): Boolean {
    val b = {
        (a: Int?) ->
        if (a == null) {
            0
        }
        else if (a == 1) {
            1
        }
        else {
            3
        }
    }
    return f(b)
}