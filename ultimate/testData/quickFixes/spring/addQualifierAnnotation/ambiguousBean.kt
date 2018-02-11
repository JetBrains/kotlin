// "Add qualifier" "true"
// FIXTURE_CLASS: org.jetbrains.kotlin.idea.spring.tests.SpringTestFixtureExtension
// CONFIGURE_SPRING_FILE_SET
// WITH_RUNTIME
// DISABLE-ERRORS
// FORCE_PACKAGE_FOLDER

package pkg

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.beans.factory.annotation.Qualifier

@Configuration
@ComponentScan
open class InvalidAutowiredByTypeQualifiers

abstract class BaseBarBean
@Component
open class BarBean1 : BaseBarBean()
@Component
open class BarBean2 : BaseBarBean()

@Component
open class MultipleAutowiredCandidates() {
    @Autowired
    lateinit var <caret>bean: BaseBarBean
}