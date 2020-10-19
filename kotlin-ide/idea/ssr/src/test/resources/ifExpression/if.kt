fun a(): Boolean {
    var b = false
    <warning descr="SSR">if(true) b = true</warning>
    <warning descr="SSR">if(true) {
        b = true
    }</warning>
    return b
}

