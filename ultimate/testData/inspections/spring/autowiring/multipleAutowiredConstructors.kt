
// WITH_RUNTIME

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan
open class MultipleAutowiredConstructors

// Not reported if required == false

@Component
open class NonRequiredAutowired1 {
    @Autowired(required = false) constructor(n: Int)
    @Autowired constructor(s: String)
}

@Component
open class NonRequiredAutowired2 @Autowired(required = false) constructor(n: Int) {
    @Autowired constructor(s: String): this(0)
}

// Reported

@Component
open class BothAutowired1 {
    @Autowired constructor(n: Int)
    @Autowired constructor(s: String)
}

@Component
open class BothAutowired2 @Autowired constructor(n: Int) {
    @Autowired constructor(s: String): this(0)
}