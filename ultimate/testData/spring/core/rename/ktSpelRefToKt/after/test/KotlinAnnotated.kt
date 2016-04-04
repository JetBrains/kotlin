package test

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Bean

@Configuration
open class KotlinAnnotated {
    @Bean
    fun buildBeanJNew() = JavaSpringBean(3)

    @Value("#{buildBeanJNew.value + 1}") private var newValue: Int = 0
}