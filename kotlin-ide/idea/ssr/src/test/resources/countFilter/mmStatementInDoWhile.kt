fun foo() {
    var x = 0

    <warning descr="SSR">do {

    } while (false)</warning>

    <warning descr="SSR">do {
        x += 1
    } while (false)</warning>

    <warning descr="SSR">do {
        x += 1
        x *= 2
    } while (false)</warning>

    do {
        x += 1
        x *= 2
        x *= x
    } while (false)

    print(x)
}
