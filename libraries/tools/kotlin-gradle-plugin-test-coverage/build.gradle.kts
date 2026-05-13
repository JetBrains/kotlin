import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    `jacoco-report-aggregation`
}

description = "Test Coverage report generation for KGP tests"

val KGP_TEST_TASKS_GROUP = "Kotlin Gradle Plugin Verification"

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

// Producers expose `.exec`, classes, and sources via outgoing configurations matched here —
// Project-Isolation-compatible, preserves cross-project task ordering and up-to-date checks.
dependencies {
    jacocoAggregation(project(":kotlin-gradle-plugin"))
    jacocoAggregation(project(":kotlin-gradle-plugin-api"))
    jacocoAggregation(project(":kotlin-gradle-plugin-integration-tests"))
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

val combinedCoverageReport by tasks.registering(JacocoReport::class) {
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
