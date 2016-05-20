// ELEMENT: existingBean1
// CHAR: \n
package bean

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

class MultiHolder(var thing: String)

class AutoMultiUser() {
    @Qualifier("exist<caret>ing") @Autowired lateinit var multi1: MultiHolder
}