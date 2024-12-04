package usage

internal class Usage {
    val inlined = inline.test()

    val check = inline.same::class.java == inlined::class.java
}
