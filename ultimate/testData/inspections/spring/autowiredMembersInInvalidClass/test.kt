
// WITH_RUNTIME

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.support.SpringBeanAutowiringSupport

open class KtBean1 {
    @Autowired var foo: Int = 1
}

@Component
open class KtBean2 {
    @Autowired var foo: Int = 1
}

open class KtBean3 : SpringBeanAutowiringSupport() {
    @Autowired var foo: Int = 1
}