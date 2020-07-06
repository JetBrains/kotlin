class ClassOne {
    <warning descr="SSR">val valOne: (() -> Unit)? = {}</warning>
    <warning descr="SSR">val valTwo: ClassOne? = null</warning>
}