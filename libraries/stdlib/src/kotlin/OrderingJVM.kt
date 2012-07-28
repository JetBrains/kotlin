package kotlin

import java.util.Comparator

/**
* Helper method for implementing [[Comparable]] methods using a list of functions
* to calculate the values to compare
*/
inline fun <T> compareBy(a: T?, b: T?, vararg functions: Function1<T, Any?>): Int {
    require(functions.size > 0)
    if (a === b) return 0
    if (a == null) return - 1
    if (b == null) return 1
    for (fn in functions) {
        val v1 = fn(a)
        val v2 = fn(b)
        val diff = compareValues(v1, v2)
        if (diff != 0) return diff
    }
    return 0
}

/**
 * Compares the two values which may be [[Comparable]] otherwise
 * they are compared via [[#equals()]] and if they are not the same then
 * the [[#hashCode()]] method is used as the difference
 */
public inline fun <T> compareValues(a: T?, b: T?): Int {
    if (a === b) return 0
    if (a == null) return - 1
    if (b == null) return 1
    if (a is Comparable<*>) {
        return (a as Comparable<Any?>).compareTo(b)
    }
    if (a == b) {
        return 0
    }
    if (a is Object && b is Object) {
        val diff = a.hashCode() - b.hashCode()
        return if (diff == 0) 1 else diff
    } else {
        // TODO???
        return 1
    }
}

/**
 * Creates a comparator using the sequence of functions used to calculate a value to compare on
 */
public inline fun <T> comparator(vararg functions: Function1<T,Any?>): Comparator<T> {
    return FunctionComparator<T>(functions)
}


private class FunctionComparator<T>(val functions: Array<Function1<T,Any?>>):  Comparator<T> {

    public override fun toString(): String {
        return "FunctionComparator${functions.toList()}"
    }

    public override fun compare(o1: T?, o2: T?): Int {
        return compareBy<T>(o1, o2, *functions)
    }

    public override fun equals(obj: Any?): Boolean {
        return this == obj
    }
}

/**
 * Creates a comparator using the sequence of functions used to calculate a value to compare on
 */
public inline fun <T> comparator(fn: (T,T) -> Int): Comparator<T> {
    return Function2Comparator<T>(fn)
}
private class Function2Comparator<T>(val compareFn: (T,T) -> Int):  Comparator<T> {

    public override fun toString(): String {
        return "Function2Comparator${compareFn}"
    }

    public override fun compare(a: T?, b: T?): Int {
        if (a === b) return 0
        if (a == null) return - 1
        if (b == null) return 1
        return (compareFn)(a, b)
    }

    public override fun equals(obj: Any?): Boolean {
        return this == obj
    }
}
