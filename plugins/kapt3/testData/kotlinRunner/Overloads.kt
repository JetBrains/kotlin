package test

internal annotation class MyAnnotation

@MyAnnotation
internal class State @JvmOverloads constructor(
    val someInt: Int,
    val someLong: Long,
    val someString: String = ""
) {

    @JvmOverloads
    fun overloadedMethod(
        someInt: Int,
        someLong: Long,
        someString: String = ""
    ) = 0

    @JvmStatic
    @JvmOverloads
    fun staticMethod(
        someInt: Int,
        someLong: Long,
        someString: String = ""
    ) = 0

}
