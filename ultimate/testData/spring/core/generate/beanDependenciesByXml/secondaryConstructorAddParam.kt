// ACTION_CLASS: org.jetbrains.kotlin.idea.spring.generate.GenerateKotlinSpringBeanXmlDependencyAction$Constructor
// CONFIG_FILE: secondaryConstructorAddParam-config.xml
// CHOOSE_BEAN: bazBean
package a

open class FooBean(n: Int) {
    constructor(barBean: BarBean): this(1)<caret>
}

open class BarBean

open class BazBean