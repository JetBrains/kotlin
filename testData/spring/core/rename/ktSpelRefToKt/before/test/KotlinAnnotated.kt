package test

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Bean

@Configuration
open class KotlinAnnotated {
    @Bean
    fun buildBeanJ() = JavaSpringBean(3)

    @Value("#{/*rename*/buildBeanJ.value + 1}") private var newValue: Int = 0
}