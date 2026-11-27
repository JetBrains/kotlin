plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    `java-library`
    id("analysis-api-artifact")
}

dependencies {
    // Tests have access to the Analysis API internals, so the implementation artifacts are exposed
    api(project(":prepare:analysis-api:kotlin-analysis-api-implementation"))
    api(project(":prepare:analysis-api:kotlin-analysis-api-standalone-implementation"))

    // The test framework is built on top of the compiler test infrastructure
    api(project(":prepare:kotlin-compiler-internal-test-framework"))

    api(kotlinTest())
    api(libs.junit.jupiter.api)

    // The test data manager writes its reports with kotlinx.serialization
    api(libs.kotlinx.serialization.json)

    // Used by the test data manager
    implementation(libs.junit.platform.launcher)
}

analysisApiArtifact {
    content {
        testFixtures(
            listOf(
                ":analysis:analysis-api-fir",
                ":analysis:analysis-api-standalone",
                ":analysis:analysis-test-framework",
                ":analysis:low-level-api-fir",

                // Supertypes of 'AbstractAnalysisApiBasedTest' come from these modules
                ":analysis:test-data-manager",
                ":compiler:psi:psi-api",
            )
        )

        // The test fixtures of the test data manager are built on top of its production code
        project(":analysis:test-data-manager")
    }
}
