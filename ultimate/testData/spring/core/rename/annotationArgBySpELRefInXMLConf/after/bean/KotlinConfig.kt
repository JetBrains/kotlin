package bean

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration open class KotlinConfig {
    @Bean(name = arrayOf("nameK2")) open fun createBeanK() = BeanA()
}
