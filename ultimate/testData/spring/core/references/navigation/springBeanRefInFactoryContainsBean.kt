// REF: <bean id="fooBean" class="java.lang.String"/>
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.xml.XmlBeanFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource

class FooBeanClass {
    init {
        val configFile = FileSystemResource("spring-config.xml")
        val factory = XmlBeanFactory(configFile)
        if (factory.containsBean("<caret>fooBean")) println("FooBean")
    }
}