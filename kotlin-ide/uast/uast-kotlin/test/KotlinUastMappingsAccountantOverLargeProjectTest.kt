package org.jetbrains.uast.test.kotlin

import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.uast.test.common.PsiClassToString
import org.jetbrains.uast.test.common.UastMappingsAccountantSingleTestBase
import org.jetbrains.uast.test.common.UastMappingsAccountantTest
import org.jetbrains.uast.test.common.sourcesFromLargeProject
import org.junit.Ignore
import org.junit.Test
import java.nio.file.Path


/**
 * Computes Kotlin PSI to Uast mappings over the custom large project.
 *
 * It is recommended to clone Kotlin IDE repo with Intellij under it
 * (with `git clone --depth 1`) and specify the [testProjectPath] to it.
 * Or you can run it on the very same repo you are working from,
 * but be careful -- some *.iml files may then be updated, revert them.
 */
@Ignore("Very laborious task, should be invoked manually only")
class KotlinUastMappingsAccountantOverLargeProjectTest :
    AbstractKotlinLargeProjectTest(), UastMappingsAccountantSingleTestBase {

    override val testProjectPath: Path = throw NotImplementedError("Must be specified manually")

    private val sourcesToProcessLimit = 50000 // will require up to 4 hours to finish

    private val delegate by lazy(LazyThreadSafetyMode.NONE) {
        UastMappingsAccountantTest(
            sources = sourcesFromLargeProject(KotlinFileType.INSTANCE, project, sourcesToProcessLimit, LOG).asIterable(),
            storeResultsTo = testProjectPath,
            resultsNamePrefix = "FULL-kotlin-mappings",
            psiClassPrinter = PsiClassToString.asIs,
            doInParallel = true,
            logger = LOG
        )
    }

    @Test
    override fun `test compute all mappings and print in all variations`() {
        delegate.`test compute all mappings and print in all variations`()
    }
}