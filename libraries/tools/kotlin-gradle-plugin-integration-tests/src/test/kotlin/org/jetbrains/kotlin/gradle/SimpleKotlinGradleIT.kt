package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.configuration.WarningMode
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@SimpleGradlePluginTests
@DisplayName("KGP simple tests")
class SimpleKotlinGradleIT : KGPBaseTest() {

    @GradleTest
    @DisplayName("On second run common tasks should be up-to-date")
    fun testSimpleCompile(gradleVersion: GradleVersion) {
        project(
            projectName = "simpleProject",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG),
        ) {
            build("compileDeployKotlin", "build") {
                assertOutputContains("Finished executing kotlin compiler using ${KotlinCompilerExecutionStrategy.DAEMON} strategy")
                assertFileInProjectExists("build/reports/tests/test/classes/demo.TestSource.html")
                assertTasksExecuted(":compileKotlin", ":compileTestKotlin", ":compileDeployKotlin")
            }

            build("compileDeployKotlin", "build") {
                assertTasksUpToDate(
                    ":compileKotlin",
                    ":compileTestKotlin",
                    ":compileDeployKotlin",
                    ":compileJava"
                )
            }
        }
    }

    @GradleTest
    @DisplayName("Plugin allows to suppress all warnings")
    fun testSuppressWarnings(gradleVersion: GradleVersion) {
        project("suppressWarnings", gradleVersion) {
            build("build") {
                assertTasksExecuted(":compileKotlin")
                assertOutputDoesNotContain("""w: [^\r\n]*?\.kt""".toRegex())
            }
        }
    }

    @GradleTest
    @DisplayName("Plugin should allow to add custom Kotlin directory")
    fun testKotlinCustomDirectory(gradleVersion: GradleVersion) {
        project("customSrcDir", gradleVersion) {
            build("build")
        }
    }

    @GradleTest
    @DisplayName("Plugin should correctly handle additional java source directories")
    fun testKotlinExtraJavaSrc(gradleVersion: GradleVersion) {
        project("additionalJavaSrc", gradleVersion) {
            build("build")
        }
    }

    @GradleTest
    @DisplayName("Using newer language features with older api level should fail the build")
    fun testLanguageVersion(gradleVersion: GradleVersion) {
        project("languageVersion", gradleVersion) {
            buildAndFail("build") {
                assertOutputContains("Suspend function type is not allowed as supertypes")
            }
        }
    }

    @GradleTest
    @DisplayName("Compilation should fail on unknown JVM target")
    fun testJvmTarget(gradleVersion: GradleVersion) {
        project("jvmTarget", gradleVersion) {
            buildAndFail("build") {
                assertOutputContains("Unknown JVM target version: 1.7")
            }
        }
    }

    @GradleTest
    @DisplayName("Should produce '.kotlin_module' file with specified name")
    fun testModuleName(gradleVersion: GradleVersion) {
        project("moduleName", gradleVersion) {
            build("build") {
                assertFileInProjectExists("build/classes/kotlin/main/META-INF/FLAG.kotlin_module")
                assertFileInProjectNotExists("build/classes/kotlin/main/META-INF/moduleName.kotlin_module")
                assertOutputDoesNotContain("Argument -module-name is passed multiple times")
            }
        }
    }

    @GradleTest
    @DisplayName("Should use custom JDK to compile sources")
    fun testCustomJdk(gradleVersion: GradleVersion) {
        project("customJdk", gradleVersion) {
            buildAndFail("build") {
                assertOutputContains("Unresolved reference: stream")
                assertOutputDoesNotContain("Unresolved reference: AutoCloseable")
            }
        }
    }

    @GradleTest
    @DisplayName("Compile task destination dir should be configured on configuration phase")
    fun testDestinationDirReferencedDuringEvaluation(gradleVersion: GradleVersion) {
        project("destinationDirReferencedDuringEvaluation", gradleVersion) {
            build("build") {
                assertOutputContains("foo.GreeterTest > testHelloWorld PASSED")
            }
        }
    }

    @GradleTest
    @DisplayName("Plugin correctly handle redefined build dir location")
    fun testBuildDirLazyEvaluation(gradleVersion: GradleVersion) {
        project("kotlinProject", gradleVersion) {
            // Change the build directory in the end of the build script:
            val customBuildDirName = "customBuild"
            buildGradle.append(
                "buildDir = '$customBuildDirName'"
            )

            build("build") {
                assertDirectoryInProjectExists("$customBuildDirName/classes")
                assertFileInProjectNotExists("build")
            }
        }
    }

    @GradleTest
    @DisplayName("Should correctly work with Groovy lang modules")
    fun testGroovyInterop(gradleVersion: GradleVersion) {
        project("groovyInterop", gradleVersion) {
            build("build") {
                assertTasksExecuted(":test")
                assertOutputContains("GroovyInteropTest > parametersInInnerClassConstructor PASSED")
                assertOutputContains("GroovyInteropTest > classWithReferenceToInner PASSED")
                assertOutputContains("GroovyInteropTest > groovyTraitAccessor PASSED")
                assertOutputContains("GroovyInteropTest > parametersInEnumConstructor PASSED")
            }
        }
    }

    //Proguard corrupts RuntimeInvisibleParameterAnnotations/RuntimeVisibleParameterAnnotations tables:
    // https://sourceforge.net/p/proguard/bugs/735/
    // Gradle 7 compatibility issue: https://github.com/Guardsquare/proguard/issues/136
    @GradleTest
    @GradleTestVersions(maxVersion = TestVersions.Gradle.G_6_8)
    @DisplayName("Should correctly interop with ProGuard")
    fun testInteropWithProguarded(gradleVersion: GradleVersion) {
        project(
            "interopWithProguarded",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(warningMode = WarningMode.Summary)
        ) {
            build("build") {
                assertTasksExecuted(":test")
                assertOutputContains("InteropWithProguardedTest > parametersInInnerKotlinClassConstructor PASSED")
                assertOutputContains("InteropWithProguardedTest > parametersInInnerJavaClassConstructor PASSED")
                assertOutputContains("InteropWithProguardedTest > parametersInJavaEnumConstructor PASSED")
                assertOutputContains("InteropWithProguardedTest > parametersInKotlinEnumConstructor PASSED")
            }
        }
    }

    @GradleTest
    @DisplayName("Should correctly work with Scala lang modules")
    fun testScalaInterop(gradleVersion: GradleVersion) {
        project("scalaInterop", gradleVersion) {
            build("build") {
                assertTasksExecuted(":test")
                assertOutputContains("ScalaInteropTest > parametersInInnerClassConstructor PASSED")
            }
        }
    }

    @GradleTest
    @DisplayName("Should not produce kotlin-stdlib version conflict on Kotlin files compilation in 'buildSrc' module")
    internal fun testKotlinDslStdlibVersionConflict(gradleVersion: GradleVersion) {
        project(
            projectName = "buildSrcUsingKotlinCompilationAndKotlinPlugin",
            gradleVersion,
            forceOutput = true
        ) {
            listOf(
                "compileClasspath",
                "compileOnly",
                "runtimeClasspath"
            ).forEach { configuration ->
                build("-p", "buildSrc", "dependencies", "--configuration", configuration) {
                    listOf(
                        "org.jetbrains.kotlin:kotlin-stdlib:${buildOptions.kotlinVersion}",
                        "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${buildOptions.kotlinVersion}",
                        "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${buildOptions.kotlinVersion}",
                        "org.jetbrains.kotlin:kotlin-stdlib-common:${buildOptions.kotlinVersion}",
                        "org.jetbrains.kotlin:kotlin-reflect:${buildOptions.kotlinVersion}",
                        "org.jetbrains.kotlin:kotlin-script-runtime:${buildOptions.kotlinVersion}"
                    ).forEach { assertOutputDoesNotContain(it) }
                }
            }

            build("assemble")
        }
    }

    @GradleTest
    @DisplayName("useExperimentalAnnotation should produce deprecation warning")
    fun testUseExperimentalAnnotationShouldProduceWarning(gradleVersion: GradleVersion) {
        project("optInAnnotation", gradleVersion, buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
            build("assemble") {
                assertOutputContains("-opt-in=kotlin.RequiresOptIn")
                assertOutputContains("-opt-in=FooAnnotation")
                assertOutputContains("is deprecated and will be removed in next major releases")
            }
        }
    }

    @GradleTest
    @DisplayName("Should be compatible with project isolation")
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_1, maxVersion = TestVersions.Gradle.G_7_1)
    fun testProjectIsolation(gradleVersion: GradleVersion) {
        project(
            projectName = "instantExecution",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(configurationCache = true, projectIsolation = true),
        ) {
            build(":main-project:compileKotlin")
        }
    }
}
