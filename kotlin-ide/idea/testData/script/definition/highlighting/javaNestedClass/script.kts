import lib.*

fun f(
    <warning>g</warning>: JavaInterface.G,
    <warning>f</warning>: JavaInterface.F,
    <warning>i</warning>: JavaInterface.F.I
) {

}

// DEPENDENCIES: classpath:lib-classes; imports:custom.library.*