// NO_XML_CONFIG
// REF: /src.fileReferenceInClasspathXmlContext.test.xml
import org.springframework.context.support.ClassPathXmlApplicationContext

class Bean {
    init {
        ClassPathXmlApplicationContext("<caret>fileReferenceInClasspathXmlContext.test.xml")
    }
}