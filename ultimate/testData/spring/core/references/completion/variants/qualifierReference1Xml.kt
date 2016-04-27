// NUMBER: 3
// EXIST: { lookupString:"anotherBean" }
// EXIST: { lookupString:"existingBean1" }
// EXIST: { lookupString:"existingBean2" }

package bean

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

class MultiHolder(var thing: String)

class AutoMultiUser() {
    @Qualifier("<caret>existingBean1") @Autowired lateinit var multi1: MultiHolder
}