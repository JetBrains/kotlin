import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    `jacoco-report-aggregation`
}

description = "Test Coverage report generation for KGP tests"

val KGP_TEST_TASKS_GROUP = "Kotlin Gradle Plugin Verification"

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

// `isTransitive = false` used to avoid having non-instrumented classes with zero coverage in reports
// e.g., build-tools-api, konan.*, commonizer
dependencies {
    jacocoAggregation(project(":kotlin-gradle-plugin")) { isTransitive = false }
    jacocoAggregation(project(":kotlin-gradle-plugin-api")) { isTransitive = false }
    jacocoAggregation(project(":kotlin-gradle-plugin-integration-tests")) { isTransitive = false }
}

reporting {
    reports {
        register<JacocoCoverageReport>("functionalCoverageReport") {
            testSuiteName = "functionalTest"
        }
        register<JacocoCoverageReport>("integrationCoverageReport") {
            testSuiteName = "integrationTest"
        }
    }
}

// `jacoco-report-aggregation` has no match-all-suites option, so compose the combined report
// manually from the per-suite tasks. Data still flows through the `jacocoAggregation` graph.
val functionalReport = tasks.named<JacocoReport>("functionalCoverageReport")
val integrationReport = tasks.named<JacocoReport>("integrationCoverageReport")

tasks.register<JacocoReport>("combinedCoverageReport") {
    group = KGP_TEST_TASKS_GROUP
    description = "Aggregated HTML/XML coverage report for KGP functional + integration tests"

    executionData.from(functionalReport.map { it.executionData })
    executionData.from(integrationReport.map { it.executionData })
    classDirectories.from(functionalReport.map { it.classDirectories })
    sourceDirectories.from(functionalReport.map { it.sourceDirectories })

    reports {
        html.required = true
        xml.required = true
        csv.required = false
    }

    onlyIf { executionData.files.any { it.exists() } }
}

// Default report set is HTML only; enable XML for the per-suite tasks too.
listOf(functionalReport, integrationReport).forEach { reportTask ->
    reportTask.configure {
        group = KGP_TEST_TASKS_GROUP
        reports {
            html.required = true
            xml.required = true
            csv.required = false
        }
        onlyIf { executionData.files.any { it.exists() } }
    }
}
