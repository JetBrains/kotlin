package foo

import js.*

native
fun paramCount(vararg a : Int) : Int = js.noImpl

fun count(vararg a : Int) = a.size

fun box() : Boolean {
    if (paramCount(1, 2 ,3) != 3) {
        return false;
    }
    if (paramCount() != 0) {
        return false;
    }
    if (count() != 0) {
        return false;
    }
    if (count(1, 1, 1, 1) != 4) {
        return false;
    }
    return true;
}