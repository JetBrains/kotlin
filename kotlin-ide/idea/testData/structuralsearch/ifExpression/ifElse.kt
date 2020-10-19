fun a(): Int {
    return <warning descr="SSR">if(true) 1 else 2</warning>
}