fun foo(p1: Int, p2: Int, p3: Int): Int {
    var p1 = p1
    var p3 = p3
    p1++
    if (p2 > 0) p3 = 0
    return p1 + p2 + p3
}