// ELEMENT: lang
// CHAR: \t

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackages = arrayOf("java.la<caret>ng"))
open class App {

}