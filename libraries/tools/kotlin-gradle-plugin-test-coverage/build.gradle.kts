import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import java.io.File


description = "Test Coverage report generation for KGP tests"
val KGP_TEST_TASKS_GROUP = "Kotlin Gradle Plugin Verification"


// Configuration to resolve JaCoCo CLI for generating reports
val jacocoCli: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    jacocoCli(libs.jacoco.cli) { artifact { classifier = "nodeps" } }
}

val kgpProject = project(":kotlin-gradle-plugin")
val kgpApiProject = project(":kotlin-gradle-plugin-api")
val kgpIntegrationTestsProject = project(":kotlin-gradle-plugin-integration-tests")

class JacocoReportConfig(
    val reportType: String,
    val reportDir: Provider<RegularFile>,
    val coverageFiles: Provider<Set<File>>,
    val extraArgs: List<String> = emptyList(),
    val reportMessage: String,
)

val integrationJaCoCoReportFiles = providers.provider<Set<File>> {
    kgpIntegrationTestsProject.layout.buildDirectory.dir("jacoco/testkit")
        .get().asFileTree
        .matching { include("*.exec") }.files
}

val functionalJacocoReportFiles = providers.provider<Set<File>> {
    kgpProject.layout.buildDirectory.dir("jacoco")
        .get().asFileTree
        .matching { include("functionalTest.exec") }.files
}

val combinedJacocoReportFiles = providers.provider {
    buildSet {
        addAll(functionalJacocoReportFiles.get())
        addAll(integrationJaCoCoReportFiles.get())
    }
}

fun TaskContainer.registerJacocoReportTask(
    taskName: String,
    description: String,
    config: JacocoReportConfig,
): TaskProvider<JavaExec> {
    return register<JavaExec>(taskName) {
        group = KGP_TEST_TASKS_GROUP
        this.description = description

        val classesDirs = listOf(
            kgpProject.layout.buildDirectory.dir("classes/kotlin/common"),
            kgpApiProject.layout.buildDirectory.dir("classes/kotlin/common"),
        )

        val sourceDirs = listOf(
            kgpProject.projectDir.resolve("src/common/kotlin"),
            kgpProject.projectDir.resolve("src/main/kotlin"),
            kgpApiProject.projectDir.resolve("src/common/kotlin"),
        )

        onlyIf {
            if (config.coverageFiles.get().isEmpty()) {
                logger.lifecycle(
                    "Skipping $taskName: no JaCoCo .exec reports found in " +
                            config.coverageFiles.get().joinToString(" or ") { it.absolutePath }
                )
                false
            } else {
                true
            }
        }

        classpath(jacocoCli)
        mainClass = "org.jacoco.cli.internal.Main"

        inputs.files(config.coverageFiles)
        classesDirs.forEach { inputs.dir(it) }
        sourceDirs.forEach { inputs.dir(it) }

        argumentProviders.add(CommandLineArgumentProvider {
            buildList {
                add("report")
                config.coverageFiles.get().forEach { add(it.absolutePath) }
                classesDirs.forEach { dir ->
                    add("--classfiles")
                    add(dir.get().asFile.absolutePath)
                }
                sourceDirs.forEach { src ->
                    add("--sourcefiles")
                    add(src.absolutePath)
                }
                add("--${config.reportType}")
                add(config.reportDir.get().asFile.absolutePath)
                addAll(config.extraArgs)
            }
        })

        outputs.dir(config.reportDir.get().asFile.parentFile)

        doLast {
            logger.lifecycle(config.reportMessage)
        }
    }
}

fun reportsDir(subdir: String) = layout.buildDirectory.file("reports/jacoco/$subdir")

val jacocoFunctionalHtmlReport = tasks.registerJacocoReportTask(
    taskName = "jacocoFunctionalHtmlReport",
    description = "Generate HTML coverage report for functional tests using jacoco-cli",
    config = JacocoReportConfig(
        reportType = "html",
        reportDir = reportsDir("html/functional"),
        coverageFiles = functionalJacocoReportFiles,
        extraArgs = listOf("--name", "Kotlin Gradle Plugin Functional Tests Coverage"),
        reportMessage = "JaCoCo functional HTML report generated at: ${
            reportsDir("html/functional").get().asFile.absolutePath
        }/index.html",
    ),
)

val jacocoIntegrationHtmlReport = tasks.registerJacocoReportTask(
    taskName = "jacocoIntegrationHtmlReport",
    description = "Generate HTML coverage report for integration tests using jacoco-cli",
    config = JacocoReportConfig(
        reportType = "html",
        reportDir = reportsDir("html/integration"),
        coverageFiles = integrationJaCoCoReportFiles,
        extraArgs = listOf("--name", "Kotlin Gradle Plugin Integration Tests Coverage"),
        reportMessage = "JaCoCo integration HTML report generated at: ${
            reportsDir("html/integration").get().asFile.absolutePath
        }/index.html",
    ),
)

val jacocoCombinedHtmlReport = tasks.registerJacocoReportTask(
    taskName = "jacocoCombinedHtmlReport",
    description = "Generate HTML coverage report for functional and integration tests using jacoco-cli",
    config = JacocoReportConfig(
        reportType = "html",
        reportDir = reportsDir("html/combined"),
        coverageFiles = combinedJacocoReportFiles,
        extraArgs = listOf("--name", "Kotlin Gradle Plugin Functional and Integration Tests Coverage"),
        reportMessage = "JaCoCo combined HTML report generated at: ${
            reportsDir("html/combined").get().asFile.absolutePath
        }/index.html",
    ),
)

val jacocoHtmlReport = tasks.register("jacocoHtmlReport") {
    group = KGP_TEST_TASKS_GROUP
    description = "Generate functional, integration, and combined HTML coverage reports using jacoco-cli"

    dependsOn(
        jacocoFunctionalHtmlReport,
        jacocoIntegrationHtmlReport,
        jacocoCombinedHtmlReport,
    )
}

val jacocoXmlReport = tasks.registerJacocoReportTask(
    taskName = "jacocoXmlReport",
    description = "Generate XML coverage report using jacoco-cli",
    config = JacocoReportConfig(
        reportType = "xml",
        reportDir = reportsDir("report.xml"),
        coverageFiles = combinedJacocoReportFiles,
        reportMessage = "JaCoCo XML report generated at: ${reportsDir("report.xml").get().asFile.absolutePath}",
    ),
)

tasks.register("jacocoReport") {
    group = KGP_TEST_TASKS_GROUP
    description = "Generate functional, integration, and combined HTML and XML coverage reports using jacoco-cli"

    dependsOn(
        jacocoXmlReport,
        jacocoHtmlReport
    )
}
