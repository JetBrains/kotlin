// ELEMENT: java
// CHAR: \n

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackages = arrayOf("ja<caret>va"))
open class App {

}