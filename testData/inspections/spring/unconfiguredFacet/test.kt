
// WITH_RUNTIME

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan
annotation class MyConfiguration

@Configuration
class Application1

@MyConfiguration
class Application2