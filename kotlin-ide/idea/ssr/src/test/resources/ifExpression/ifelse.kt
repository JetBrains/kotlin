fun a(): Int {
    var b: Int
    <warning descr="SSR">if(true) b = 1 else b = 2</warning>
    return b
}