import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.testFederation.testBatchArguments

tasks.withType<Test>().configureEach {
    val testBatchArguments = project.testBatchArguments
    jvmArgumentProviders.add(testBatchArguments)

    doFirst {
        if (testBatchArguments.currentBatch.isPresent) {
            if (testFramework !is JUnitPlatformTestFramework) {
                error("Test batching is only supported on 'Junit5'")
            }

            logger.quiet("Running tests in batch ${testBatchArguments.currentBatch.get()}/${testBatchArguments.totalBatches.get()}")
        }
    }
}
