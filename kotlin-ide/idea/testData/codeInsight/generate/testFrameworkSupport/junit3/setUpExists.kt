// ACTION_CLASS: org.jetbrains.kotlin.idea.actions.generate.KotlinGenerateTestSupportActionBase$SetUp
// NOT_APPLICABLE
// CONFIGURE_LIBRARY: JUnit
import junit.framework.TestCase

class A : TestCase() {<caret>
    override fun setUp() {
        super.setUp()
    }
}