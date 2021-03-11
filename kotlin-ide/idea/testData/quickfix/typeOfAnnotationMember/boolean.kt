// "Replace array of boxed with array of primitive" "true"
annotation class SuperAnnotation(
        val boo: <caret>Array<Boolean>,
        val str: Array<String>
)