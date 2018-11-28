@file:Suppress("UNUSED_PARAMETER")
package cases.default

internal fun allDefaults(par1: Any = 1, par2: String? = null) {}

internal fun someDefaults(par1: Any, par2: String? = null) {}


open class ClassFunctions {

    internal open fun allDefaults(par1: Any = 1, par2: String? = null) {}

    internal open fun someDefaults(par1: Any, par2: String? = null) {}

}


interface InterfaceFunctions {

    fun withAllDefaults(par1: Int = 1, par2: String? = null)

    fun withSomeDefaults(par1: Int, par2: String? = null)

}