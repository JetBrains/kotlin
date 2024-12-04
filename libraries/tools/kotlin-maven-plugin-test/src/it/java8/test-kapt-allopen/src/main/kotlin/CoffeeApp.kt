package coffee

import dagger.Component
import javax.inject.Singleton

object CoffeeApp {
    @Singleton
    @Component(modules = arrayOf(DripCoffeeModule::class))
    interface Coffee {
        fun maker(): CoffeeMaker
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val coffee = DaggerCoffeeApp_Coffee.builder().build()
        coffee.maker().brew()
    }
}
