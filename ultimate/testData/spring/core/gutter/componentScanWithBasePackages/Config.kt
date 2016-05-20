import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@ComponentScan(basePackages = arrayOf("foo", "bar"))
@Configuration
open class Config