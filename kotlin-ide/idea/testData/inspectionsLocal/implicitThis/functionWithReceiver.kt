fun CharSequence.foo(bar: CharSequence.() -> Unit){
    <caret>bar()
}