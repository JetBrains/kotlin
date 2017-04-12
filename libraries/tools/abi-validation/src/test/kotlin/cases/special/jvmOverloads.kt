@file:Suppress("UNUSED_PARAMETER")

package cases.special


@JvmOverloads
public fun publicFunWithOverloads(a: Int = 0, b: String? = null) {}

@JvmOverloads
internal fun internalFunWithOverloads(a: Int = 0, b: String? = null) {}

public class ClassWithOverloads
    @JvmOverloads
    internal constructor(val a: Int = 0, val b: String? = null) {

    @JvmOverloads
    internal fun internalFunWithOverloads(a: Int = 0, b: String? = null) {}

}