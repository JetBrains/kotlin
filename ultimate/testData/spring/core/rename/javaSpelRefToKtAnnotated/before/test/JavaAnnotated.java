package test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JavaAnnotated {
    @Value("#{/*rename*/annJavaBean.value + 1}") private int newValue;
}