// "Replace array of boxed with array of primitive" "true"
annotation class SuperAnnotation(
        val l: <caret>Array<Long>,
        val str: Array<String>
)