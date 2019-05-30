
// WITH_RUNTIME
// FULL_JDK

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import javax.annotation.Resource

@Configuration
@ComponentScan
open class MismatchedTypes

@Component
open class FooBean

@Component
open class BarBean

@Component
open class MismatchedType() {
    @Autowired
    @Resource(name = "fooBean")
    lateinit var bean1: BarBean
}

@Component
open class UnresolvedBeanRef() {
    @Autowired
    @Resource(name = "fooBeannn")
    lateinit var bean1: FooBean
}