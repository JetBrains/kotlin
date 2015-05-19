package utils

public class A(public val x: Int) {
    inline
    public fun plus(y: Int): Int = x + y
}