package test

import apt.Anno
import generated.Property

object Test {
    @field:Anno
    lateinit var property: Property

    @JvmStatic
    fun main(args: Array<String>) {
        print(javaClass.getDeclaredField("property").type.toGenericString())
    }
}