fun a(): Boolean {
    var b = false
    <warning descr="SSR">when(b) {
        true -> b = false
        false -> b = true
    }</warning>
    return b
}

