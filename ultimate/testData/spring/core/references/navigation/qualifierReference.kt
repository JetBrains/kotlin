// REF: <bean class="bean.MultiHolder" name="existingBean1">\n        <constructor-arg name="thing" value="1"/>\n    </bean>
package bean

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

class MultiHolder(var thing: String)

class AutoMultiUser() {
    @Qualifier("<caret>existingBean1") @Autowired lateinit var multi1: MultiHolder
}