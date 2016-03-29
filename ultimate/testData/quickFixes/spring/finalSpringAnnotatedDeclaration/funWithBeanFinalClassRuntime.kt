// "Make function foo open" "true"
// FIXTURE_CLASS: org.jetbrains.kotlin.idea.spring.tests.SpringTestFixtureExtension
// DISABLE-ERRORS
import org.springframework.context.annotation.Bean

class Foo {
    @Bean
    fun <caret>foo() = ""
}