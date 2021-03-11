fun f(c: JavaWithGroovyInvoke_0) {
    c()
}

fun foo(o: JavaWithGroovyInvoke_0.OtherJavaClass) {
    o()
}

fun gr(o: GroovyClass) {
    o()

    o.fieldNoType()              // Red reference
    o.fieldWithType()

    o.methodNoType()()           // Red reference
    o.methodWithType()()
}