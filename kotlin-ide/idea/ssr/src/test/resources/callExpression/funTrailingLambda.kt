fun a(b: () -> String) {
    b() // prevent unused warning
    <warning descr="SSR">a({"foo"})</warning>
    <warning descr="SSR">a{"foo"}</warning>
}