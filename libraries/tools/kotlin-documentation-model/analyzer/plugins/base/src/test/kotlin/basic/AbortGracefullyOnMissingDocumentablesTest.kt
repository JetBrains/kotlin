package basic

import org.jetbrains.dokka.DokkaGenerator
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class AbortGracefullyOnMissingDocumentablesTest: AbstractCoreTest() {
    @Test
    fun `Generation aborts Gracefully with no Documentables`() {
        DokkaGenerator(dokkaConfiguration {  }, logger).generate()

        assertTrue(
            logger.progressMessages.any { message -> "Exiting Generation: Nothing to document" == message },
            "Expected graceful exit message. Found: ${logger.progressMessages}"
        )
    }
}
