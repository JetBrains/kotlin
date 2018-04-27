// "Make function foo open" "true"
// FIXTURE_CLASS: org.jetbrains.kotlin.idea.spring.tests.SpringTestFixtureExtension
// DISABLE-ERRORS
import org.springframework.context.annotation.Bean

@Bean
annotation class MyBean

class Foo {
    @MyBean
    fun <caret>foo() = ""
}