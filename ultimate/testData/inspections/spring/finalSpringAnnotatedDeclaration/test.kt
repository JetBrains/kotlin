
// WITH_RUNTIME

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Configuration
annotation class MyConfiguration

@Bean
annotation class MyBean

// @Configuration

@Configuration
class Application1

@MyConfiguration
class Application2

@Configuration
open class Application3

@MyConfiguration
open class Application4

// @Component

@Component
class Component1

@Component
open class Component2

// @Bean

class Utils1 {
    @Bean
    fun foo1() = Component1()

    @MyBean
    fun foo2() = Component2()

    @Bean
    open fun foo3() = Component3()

    @MyBean
    open fun foo4() = Component4()
}

open class Utils2 {
    @Bean
    fun foo1() = Component1()

    @MyBean
    fun foo2() = Component2()

    @Bean
    open fun foo3() = Component3()

    @MyBean
    open fun foo4() = Component4()
}