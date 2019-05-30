package test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JavaAnnotated {
    @Bean public JavaSpringBean buildBeanJ() { return new JavaSpringBean(3); }
    @Value("#{buildBeanJ.value + 1}") private int newValue;
}