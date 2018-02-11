// NUMBER: 3
// EXIST: { lookupString:"lang" }
// EXIST: { lookupString:"util" }
// EXIST: { lookupString:"sql" }

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackages = arrayOf("java.l<caret>ang"))
open class App {

}