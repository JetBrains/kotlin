
// WITH_RUNTIME

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.beans.factory.annotation.Qualifier

@Configuration
@ComponentScan
open class InvalidAutowiredByTypeQualifiers

abstract class AbstractBarBean
@Component
open class BarBean1 : AbstractBarBean()
@Component
open class BarBean2 : AbstractBarBean()

@Component
open class IncompleteClassAutowiring() {
    @Autowired
    lateinit var : String

}