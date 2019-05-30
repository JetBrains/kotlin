// NUMBER: 2
// EXIST: { lookupString:"fooBean" }
// EXIST: { lookupString:"fooBean2" }
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.xml.XmlBeanFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource

class FooBeanClass {
    init {
        val configFile = FileSystemResource("spring-config.xml")
        val factory = XmlBeanFactory(configFile)
        factory.getBean("fo<caret>o")
    }
}