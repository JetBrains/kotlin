// ACTION_CLASS: org.jetbrains.kotlin.idea.spring.generate.GenerateKotlinAutowiredDependencyAction
// CHOOSE_BEAN: barBean
package a

import org.springframework.stereotype.Component
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ComponentScan

@Component
open class FooBean {
    <caret>
}

@Component
open class BarBean

@Configuration
@ComponentScan
open class Application