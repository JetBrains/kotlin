// JAVAX_ANNOTATION_RESOURCE
// REF: <bean id="fooBean" class="test.Bean"/>
package test

import javax.annotation.Resource

class Bean

class Test {
    @Resource(name= "<caret>&fooBean") lateinit var name: String
}