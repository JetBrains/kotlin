// NO_XML_CONFIG
// REF: /src.fileReferenceInClasspathResource.test.xml
import org.springframework.core.io.ClassPathResource

class Bean {
    init {
        ClassPathResource("<caret>fileReferenceInClasspathResource.test.xml")
    }
}