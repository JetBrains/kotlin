
// WITH_RUNTIME

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackages = arrayOf("foo"))
open class App1

@Configuration
@ComponentScan(basePackages = arrayOf("foo", "bar"))
open class App2

@Configuration
@ComponentScan(basePackages = arrayOf("bar"))
open class App3