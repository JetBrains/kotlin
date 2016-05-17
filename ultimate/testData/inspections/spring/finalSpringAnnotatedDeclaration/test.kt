
// WITH_RUNTIME

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Configuration
annotation class MyConfiguration

@Bean
annotation class MyBean

@Transactional
annotation class MyTransactional

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

// @Transactional

@Transactional
class Trans1 {
    @Transactional
    fun foo1() = Component1()

    @MyTransactional
    fun foo2() = Component2()

    @Transactional
    open fun foo3() = Component3()

    @MyTransactional
    open fun foo4() = Component4()
}

@Transactional
open class Trans2 {
    @Transactional
    fun foo1() = Component1()

    @MyTransactional
    fun foo2() = Component2()

    @Transactional
    open fun foo3() = Component3()

    @MyTransactional
    open fun foo4() = Component4()
}

// Object declarations

@Configuration
object Application5

@MyConfiguration
object Application6

@Component
object Component3

@Transactional
object Trans3