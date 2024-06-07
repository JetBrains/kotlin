package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

@MppGradlePluginTests
class PreHmppDependenciesDeprecationIT : KGPBaseTest() {

    @GradleTest
    fun testSimpleReport(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        publishLibrary("preHmppLibrary", gradleVersion, tempDir)
        checkDiagnostics(gradleVersion, "simpleReport", tempDir, expectReportForDependency = "preHmppLibrary")
    }

    @GradleTest
    fun testReportFromIntermediateSourceSet(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        publishLibrary("preHmppLibrary", gradleVersion, tempDir)
        checkDiagnostics(gradleVersion, "reportFromIntermediateSourceSet", tempDir, expectReportForDependency = "preHmppLibrary")
    }

    @GradleTest
    fun testTransitiveDependencyUpgradesVersion(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        // 0.1
        publishLibrary("preHmppLibrary", gradleVersion, tempDir)

        // 0.2 -- still pre-HMPP
        publishLibrary("preHmppLibrary", gradleVersion, tempDir) {
            buildGradleKts.replaceText("0.1", "0.2")
        }
        publishLibrary("hmppLibraryWithPreHmppInDependencies", gradleVersion, tempDir) {
            buildGradleKts.replaceText("0.1", "0.2")
        }

        // Check that even though the version of requested dependency is different from resolved, the report warning is still emitted
        checkDiagnostics(gradleVersion, "transitiveDependencyUpgradesVersion", tempDir, expectReportForDependency = "preHmppLibrary")
    }

    @GradleTest
    fun noReportFromTransitiveDependencies(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        publishLibrary("preHmppLibrary", gradleVersion, tempDir)
        publishLibrary("hmppLibraryWithPreHmppInDependencies", gradleVersion, tempDir)
        checkDiagnostics(gradleVersion, "reportFromTransitiveDependencies", tempDir)
    }

    @GradleTest
    fun noReportWhenSuppressed(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        publishLibrary("preHmppLibrary", gradleVersion, tempDir)
        checkDiagnostics(gradleVersion, "simpleReport", tempDir) {
            gradleProperties.writeText("kotlin.mpp.allow.legacy.dependencies=true")
        }
    }

    @GradleTest
    fun testNoWarningsOnPopularDependencies(gradleVersion: GradleVersion) {
        checkDiagnostics(gradleVersion, "noWarningsOnPopularDependencies")
    }

    @GradleTest
    fun testNoWarningsOnKotlinTestIfAddedInCommonMain(gradleVersion: GradleVersion) {
        checkDiagnostics(gradleVersion, "noWarningsOnKotlinTestIfAddedInCommonMain")
    }

    @GradleTest
    fun testNoWarningsOnProjectDependencies(gradleVersion: GradleVersion) {
        checkDiagnostics(gradleVersion, "noWarningsOnProjectDependencies", projectPathToCheck = ":consumer")
    }

    @GradleTest
    fun testNoWarningsInPlatformSpecificSourceSetsOrTests(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        publishLibrary("preHmppLibrary", gradleVersion, tempDir)
        checkDiagnostics(gradleVersion, "noWarningsInPlatformSpecificSourceSetsOrTests", tempDir)
    }

    private fun checkDiagnostics(
        gradleVersion: GradleVersion,
        projectName: String,
        tempDir: Path? = null,
        projectPathToCheck: String = "", // empty means rootProject
        expectReportForDependency: String? = null,
        preBuildAction: TestProject.() -> Unit = {},
    ) {
        project(
            "preHmppDependenciesDeprecation/$projectName", gradleVersion,
            localRepoDir = tempDir?.resolve("repo"),
            buildOptions = defaultBuildOptions.copy(
                nativeOptions = defaultBuildOptions.nativeOptions.copy(
                    distributionDownloadFromMaven = false // TODO(Dmitrii Krasnov): this flag is off, because we try to find configuration which is not in maven yet. Could be set to true after KTI-1569 is done
                )
            )
        ) {
            preBuildAction()
            suppressDependencySourcesConfigurations()
            build("$projectPathToCheck:dependencies") {
                // all dependencies should be resolved, Gradle won't fail the 'dependencies' task on its own
                assertOutputDoesNotContain("FAILED")
                if (expectReportForDependency != null) {
                    output.assertHasDiagnostic(
                        KotlinToolingDiagnostics.PreHmppDependenciesUsedInBuild
                    )
                } else {
                    output.assertNoDiagnostic(KotlinToolingDiagnostics.PreHmppDependenciesUsedInBuild)
                }
            }
        }
    }

    private fun publishLibrary(name: String, gradleVersion: GradleVersion, tempDir: Path, prePublishAction: TestProject.() -> Unit = {}) {
        project("preHmppDependenciesDeprecation/$name", gradleVersion, localRepoDir = tempDir.resolve("repo")) {
            prePublishAction()
            build("publish")
        }
    }
}
