import pkg.*
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Bean
import org.springframework.beans.factory.annotation.Qualifier

@Configuration
class ContextBeanWithQualifierInjectionPoints {
    @Bean
    fun foo(): Foo {
        return Foo()
    }

    @Bean
    fun foo2(): Foo {
        return Foo()
    }

    @Bean
    fun bar(@Qualifier("foo2") f: F<caret>oo): Bar {
        return Bar(f)
    }
}