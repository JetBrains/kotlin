/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package basic

import org.jetbrains.dokka.DokkaGenerator
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import kotlin.test.Test
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
