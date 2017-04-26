package hello.tests

import coffee.*
import dagger.Component
import dagger.Module
import dagger.Provides
import junit.framework.TestCase
import javax.inject.Singleton

private var executed = false

class ExampleTest : TestCase() {
    @Singleton
    @Component(modules = arrayOf(TestCoffeeModule::class))
    interface Coffee {
        fun maker(): CoffeeMaker
    }

    @Module(includes = arrayOf(PumpModule::class))
    class TestCoffeeModule {
        @Provides @Singleton
        fun provideHeater(): Heater {
            return object: ElectricHeater() {
                override fun on() {
                    println("~ psh ~ psh ~ psh ~")
                    println("(from tests)")
                    executed = true
                    super.on()
                }
            }
        }
    }

    fun testAssert() {
        val coffee = DaggerExampleTest_Coffee.builder().build()
        coffee.maker().brew()
        assert(executed)
    }
}
