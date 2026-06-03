import org.gradle.api.internal.tasks.testing.junit.JUnitTestFramework
import org.jetbrains.kotlin.testFederation.TestClassScanningContext
import org.jetbrains.kotlin.testFederation.isTestClassExcluded
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
                    val scanningContext = TestClassScanningContext(classpath.toList())

                    val excludes = testClassesDirs.files.flatMap { dir ->
                        dir.walkTopDown().filter { it.extension == "class" }.filter { file ->
                            scanningContext.isTestClassExcluded(
                                file.relativeTo(dir), testBatchArguments.currentBatch.get(), testBatchArguments.totalBatches.get()
                            )
                        }.map { file -> "*.${file.nameWithoutExtension}" }
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
