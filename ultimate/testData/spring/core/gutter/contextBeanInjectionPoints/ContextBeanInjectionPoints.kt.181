import pkg.*
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Bean

@Configuration
class ContextBeanInjectionPoints {
    @Bean
    fun foo(): Foo {
        return Foo()
    }

    @Bean
    fun foo2(): Foo {
        return Foo()
    }

    @Bean
    fun bar(f: F<caret>oo): Bar {
        return Bar(f)
    }
}