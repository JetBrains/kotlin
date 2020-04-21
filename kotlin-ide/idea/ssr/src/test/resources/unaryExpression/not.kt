fun not(a: Boolean): Boolean {
    return <warning descr="SSR">!a</warning>
}