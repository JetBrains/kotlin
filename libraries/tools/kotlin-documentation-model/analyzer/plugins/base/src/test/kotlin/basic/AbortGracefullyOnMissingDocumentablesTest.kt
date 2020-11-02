package basic

import org.jetbrains.dokka.DokkaGenerator
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class AbortGracefullyOnMissingDocumentablesTest: BaseAbstractTest() {
    @Test
    fun `Generation aborts Gracefully with no Documentables`() {
        DokkaGenerator(dokkaConfiguration {  }, logger).generate()

        assertTrue(
            logger.progressMessages.any { message -> "Exiting Generation: Nothing to document" == message },
            "Expected graceful exit message. Found: ${logger.progressMessages}"
        )
    }
}
