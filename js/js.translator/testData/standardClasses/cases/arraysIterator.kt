package foo

val a1 = Array<Int>(3, {(i: Int) -> i })

fun box(): Boolean {
    val i = a1.iterator()
    return ((i.hasNext() == true) && (i.next() == 0) &&
    (i.hasNext() == true) && (i.next() == 1) &&
    (i.hasNext() == true) && (i.next() == 2) &&
    (i.hasNext() == false));
}