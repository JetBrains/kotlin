// ACTION_CLASS: org.jetbrains.kotlin.idea.spring.generate.GenerateKotlinAutowiredDependencyAction
// CONFIG_FILE:
// CHOOSE_BEAN: barBean
package a

open class FooBean {
    <caret>
}

open class BarBean

open class BarBeanChild : BarBean()