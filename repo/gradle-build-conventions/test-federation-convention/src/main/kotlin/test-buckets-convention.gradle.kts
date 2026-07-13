import org.jetbrains.kotlin.testFederation.testBatchArguments

tasks.withType<Test>().configureEach {
    val testBatchArguments = project.testBatchArguments
    jvmArgumentProviders.add(testBatchArguments)

    doFirst {
        if (testBatchArguments.currentBatch.isPresent) {
            val testFramework = testFramework
            logger.quiet("Running tests in batch ${testBatchArguments.currentBatch.get()}/${testBatchArguments.totalBatches.get()}")
        }
    }
}
