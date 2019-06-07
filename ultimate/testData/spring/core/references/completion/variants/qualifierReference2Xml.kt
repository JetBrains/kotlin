// NUMBER: 2
// EXIST: { lookupString:"existingBean1" }
// EXIST: { lookupString:"existingBean2" }

package bean

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

class MultiHolder(var thing: String)

class AutoMultiUser() {
    @Qualifier("existing<caret>Bean2") @Autowired lateinit var multi2: MultiHolder
}