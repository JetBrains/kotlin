
// WITH_RUNTIME

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.beans.factory.annotation.Qualifier

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
    @Qualifier("fooBean")
    lateinit var bean1: BarBean
}

@Component
open class UnresolvedBeanRef() {
    @Autowired
    @Qualifier("fooBeannn")
    lateinit var bean1: FooBean
}

@Component
open class NamelessQualifier() {
    @Autowired
    @Qualifier
    lateinit var bean1: FooBean
}