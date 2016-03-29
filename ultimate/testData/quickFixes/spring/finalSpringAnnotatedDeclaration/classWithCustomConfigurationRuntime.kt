// "Make class Application open" "true"
// FIXTURE_CLASS: org.jetbrains.kotlin.idea.spring.tests.SpringTestFixtureExtension
// DISABLE-ERRORS
import org.springframework.context.annotation.Configuration

@Configuration
annotation class MyConfiguration

@MyConfiguration
class <caret>Application