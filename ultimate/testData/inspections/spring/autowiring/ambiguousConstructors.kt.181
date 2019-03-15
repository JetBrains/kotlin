// WITH_RUNTIME

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan
open class AmbiguousConstructors

// Not reported if there is only one constructor

@Component
open class SingleConstructor1(n: Int)

@Component
open class SingleConstructor2 {
    constructor(n: Int)
}

// Not reported if no-arg constructor is present

@Component
open class NoArgConstructor1() {
    constructor(n: Int): this()
    constructor(s: String): this()
}

@Component
open class NoArgConstructor2 {
    constructor()
    constructor(n: Int)
    constructor(s: String)
}

// Not reported if @Autowired constructor is present
@Component
open class AutowiredConstructor1 {
    @Autowired constructor(n: Int)
    constructor(s: String)
}

@Component
open class AutowiredConstructor2 @Autowired constructor(n: Int) {
    constructor(s: String): this(0)
}

// Reported

@Component
open class AmbigiousConstructor1 {
    constructor(n: Int)
    constructor(s: String)
}

@Component
open class AmbigiousConstructor2(n: Int) {
    constructor(s: String): this(0)
}