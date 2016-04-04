// NUMBER: 2
// EXIST: { lookupString:"annFooBean" }
// EXIST: { lookupString:"kotlinAnnotated" }

package a

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.AnnotationConfigApplicationContext

class FooBean
class BarBean

@Configuration
open class KotlinAnnotated {
    @Bean(name=arrayOf("annFooBean")) open fun buildFoo(): FooBean = FooBean()
    @Bean open fun buildBar(): BarBean = BarBean()
}

class Test {
    fun test() {
        val context = AnnotationConfigApplicationContext("a");
        context.getBean("ann<caret>")
    }
}