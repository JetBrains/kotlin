fun a(): Int {
    var a = 1
    <warning descr="SSR">if (a == 1) {
        a = 2
    } else {
        a = 3
    }</warning>

    if (a == 0) {
        a = 2
    } else {
        a = 3
    }
    return a
}