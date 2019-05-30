
// WITH_RUNTIME

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan
open class ParameterlessFunctions

@Component
open class ParameterlessFunction {
    @Autowired fun foo() = 1
}