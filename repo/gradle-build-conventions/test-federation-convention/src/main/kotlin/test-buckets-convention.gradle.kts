import org.gradle.api.internal.tasks.testing.junit.JUnitTestFramework
import org.jetbrains.kotlin.testFederation.testBatchArguments
import kotlin.math.absoluteValue

tasks.withType<Test>().configureEach {
    val testBatchArguments = project.testBatchArguments
    jvmArgumentProviders.add(testBatchArguments)

    doFirst {
        if (testBatchArguments.currentBatch.isPresent) {
            val testFramework = testFramework
            if (testFramework is JUnitTestFramework) { // todo: support vintage engine
                filter {
                    val excludes = testClassesDirs.files.flatMap { dir ->
                        dir.walkTopDown().filter { it.extension == "class" }.mapNotNull { file ->
                            val name = file.nameWithoutExtension
                            val thisTestBatch = (name.hashCode().absoluteValue % testBatchArguments.totalBatches.get()) + 1
                            val isExcluded = thisTestBatch != testBatchArguments.currentBatch.get()
                            if (isExcluded) "*.${file.nameWithoutExtension}" else null // todo support nested classes
                        }
                    }

                    excludes.forEach { rule ->
                        excludeTestsMatching(rule)
                    }
                }
            }
            logger.quiet("Running tests in batch ${testBatchArguments.currentBatch.get()}/${testBatchArguments.totalBatches.get()}")
        }
    }
}
