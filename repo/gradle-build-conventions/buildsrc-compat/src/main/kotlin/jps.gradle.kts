@file:Suppress("UnstableApiUsage")

import org.jetbrains.gradle.ext.ActionDelegationConfig
import org.jetbrains.gradle.ext.JUnit
import org.jetbrains.kotlin.ideaExt.*


val ideaSdkPath: String
    get() = rootProject.ideaHomePathForTests().get().asFile.absolutePath
val distDir by extra("$rootDir/dist")
val distKotlinHomeDir by extra("$distDir/kotlinc")

fun updateCompilerXml() {
    val modulesExcludedFromJps = listOf(
        "buildSrc",
        "compiler/build-tools/kotlin-build-tools-api-tests",
        "jps/jps-plugin/jps-tests",
        "generators/sir-tests-generator",
        "libraries/examples",
        "libraries/scripting/dependencies-maven-all",
        "libraries/tools/atomicfu",
        "libraries/tools/binary-compatibility-validator",
        "libraries/tools/dukat",
        "libraries/tools/gradle",
        "libraries/tools/jdk-api-validator",
        "libraries/tools/kotlin-allopen",
        "libraries/tools/kotlin-annotation-processing",
        "libraries/tools/kotlin-assignment",
        "libraries/tools/kotlin-bom",
        "libraries/tools/kotlin-compose-compiler",
        "libraries/tools/kotlin-gradle-build-metrics",
        "libraries/tools/kotlin-gradle-plugin",
        "libraries/tools/kotlin-gradle-plugin-api",
        "libraries/tools/kotlin-gradle-plugin-dsl-codegen",
        "libraries/tools/kotlin-gradle-plugin-idea",
        "libraries/tools/kotlin-gradle-plugin-idea-for-compatibility-tests",
        "libraries/tools/kotlin-gradle-plugin-idea-proto",
        "libraries/tools/kotlin-gradle-plugin-integration-tests",
        "libraries/tools/kotlin-gradle-plugin-model",
        "libraries/tools/kotlin-gradle-plugin-npm-versions-codegen",
        "libraries/tools/kotlin-gradle-plugin-tcs-android",
        "libraries/tools/kotlin-gradle-plugin-test-utils-embeddable",
        "libraries/tools/kotlin-gradle-statistics",
        "libraries/tools/kotlin-lombok",
        "libraries/tools/kotlin-main-kts",
        "libraries/tools/kotlin-main-kts-test",
        "libraries/tools/kotlin-maven-allopen",
        "libraries/tools/kotlin-maven-lombok",
        "libraries/tools/kotlin-maven-noarg",
        "libraries/tools/kotlin-maven-plugin",
        "libraries/tools/kotlin-maven-plugin-test",
        "libraries/tools/kotlin-maven-sam-with-receiver",
        "libraries/tools/kotlin-maven-serialization",
        "libraries/tools/kotlin-noarg",
        "libraries/tools/kotlin-osgi-bundle",
        "libraries/tools/kotlin-privacy-manifests-plugin",
        "libraries/tools/kotlin-power-assert",
        "libraries/tools/kotlin-prepush-hook",
        "libraries/tools/kotlin-sam-with-receiver",
        "libraries/tools/kotlin-serialization",
        "libraries/tools/kotlin-serialization-unshaded",
        "libraries/tools/kotlin-stdlib-docs",
        "libraries/tools/kotlin-stdlib-gen",
        "libraries/tools/kotlin-test-js-runner",
        "libraries/tools/kotlin-tooling-metadata",
        "libraries/tools/maven-archetypes",
        "libraries/tools/mutability-annotations-compat",
        "libraries/tools/script-runtime",
        "native/commonizer",
        "native/commonizer-api",
        "native/objcexport-header-generator",
        "native/swift/swift-export-standalone/tests",
        "native/swift/swift-export-standalone/tests-gen",
        "plugins/atomicfu/atomicfu-compiler/test/org/jetbrains/kotlin/konan/test/blackbox",
        "plugins/atomicfu/atomicfu-runtime",
        "plugins/fir-plugin-prototype/plugin-annotations",
        "repo/gradle-settings-conventions",
        "repo/gradle-build-conventions",
    )

    val d = '$'
    val excludeEntries = modulesExcludedFromJps.joinToString("\n      ") {
        """      <directory url="file://${d}PROJECT_DIR${d}/$it" includeSubdirectories="true" />"""
    }

    val xmlContent = """
    <?xml version="1.0" encoding="UTF-8"?>
    <project version="4">
      <component name="CompilerConfiguration">
        <option name="BUILD_PROCESS_HEAP_SIZE" value="2000" />
        <excludeFromCompile>
          $excludeEntries
        </excludeFromCompile>
      </component>
    </project>
    """.trimIndent()
    rootDir.resolve(".idea/compiler.xml").writeText(xmlContent)
}

fun JUnit.configureForKotlin(xmx: String = "1600m") {
    vmParameters = listOf(
        "-ea",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-Xmx$xmx",
        "-XX:+UseCodeCacheFlushing",
        "-XX:ReservedCodeCacheSize=128m",
        "-XX:+UseParallelGC",
        "-Djna.nosys=true",
        "-Didea.platform.prefix=Idea",
        "-Didea.is.unit.test=true",
        "-Didea.ignore.disabled.plugins=true",
        "-Didea.home.path=$ideaSdkPath",
        "-Didea.use.native.fs.for.win=false",
        "-Djps.kotlin.home=${File(distKotlinHomeDir).absolutePath}",
        "-Duse.jps=true",
        "-Djava.awt.headless=true",
        "-Dkotlin.test.default.jvm.version=1.8",
    ).joinToString(" ")

    envs = mapOf(
        "NO_FS_ROOTS_ACCESS_CHECK" to "true",
        "PROJECT_CLASSES_DIRS" to "out/test/org.jetbrains.kotlin.compiler.test"
    )
    workingDirectory = rootDir.toString()
}

// Needed because of idea.ext plugin can't pass \n symbol
fun setupGenerateAllTestsRunConfiguration() {
    rootDir.resolve(".idea/runConfigurations/JPS__Generate_All_Tests.xml").writeText(
        """
        |<component name="ProjectRunConfigurationManager">
        | <configuration default="false" name="[JPS] Generate All Tests" type="Application" factoryName="Application">
        |    <option name="MAIN_CLASS_NAME" value="org.jetbrains.kotlin.pill.generateAllTests.Main" />
        |    <module name="kotlin.pill.generate-all-tests.test" />
        |    <option name="VM_PARAMETERS" value="&quot;-Dline.separator=&#xA;&quot;" />
        |    <shortenClasspath name="CLASSPATH_FILE" />
        |    <method v="2">
        |      <option name="Make" enabled="true" />
        |    </method>
        |  </configuration>
        |</component>
    """.trimMargin()
    )
}

// Needed because of idea.ext plugin doesn't allow to set TEST_SEARCH_SCOPE = moduleWithDependencies
fun setupFirRunConfiguration() {
    val junit = JUnit("_stub").apply { configureForKotlin("4096m") }
    junit.moduleName = "kotlin.compiler.fir.fir2ir.test"
    junit.pattern = """^.*\.FirPsi\w+Test\w*Generated$"""
    junit.vmParameters = junit.vmParameters.replace(rootDir.absolutePath, "\$PROJECT_DIR\$")
    junit.workingDirectory = junit.workingDirectory.replace(rootDir.absolutePath, "\$PROJECT_DIR\$")

    rootDir.resolve(".idea/runConfigurations/JPS__Fast_FIR_PSI_tests.xml").writeText(
        """
            |<component name="ProjectRunConfigurationManager">
            |  <configuration default="false" name="[JPS] Fast FIR PSI tests" type="JUnit" factoryName="JUnit">
            |    <module name="${junit.moduleName}" />
            |    <option name="MAIN_CLASS_NAME" value="" />
            |    <option name="METHOD_NAME" value="" />
            |    <option name="TEST_OBJECT" value="pattern" />
            |    <option name="VM_PARAMETERS" value="${junit.vmParameters}" />
            |    <option name="PARAMETERS" value="" />
            |    <option name="WORKING_DIRECTORY" value="${junit.workingDirectory}" />
            |    <option name="TEST_SEARCH_SCOPE">
            |      <value defaultName="moduleWithDependencies" />
            |    </option>
            |    <envs>
                   ${junit.envs.entries.joinToString("\n") { (name, value) -> "|      <env name=\"$name\" value=\"$value\" />" }}
            |    </envs>
            |    <dir value="${'$'}PROJECT_DIR${'$'}/compiler/fir/analysis-tests/tests-gen" />
            |    <patterns>
            |      <pattern testClass="${junit.pattern}" />
            |    </patterns>
            |    <method v="2">
            |      <option name="Make" enabled="true" />
            |    </method>
            |  </configuration>
            |</component>
        """.trimMargin()
    )
}

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    allprojects {
        apply(mapOf("plugin" to "idea"))
        // Make Idea import embedded configuration as transitive dependency for some configurations
        afterEvaluate {
            val jpsBuildTestDependencies = configurations.maybeCreate("jpsBuildTestDependencies").apply {
                isCanBeConsumed = false
                isCanBeResolved = true
                attributes {
                    attribute(Usage.USAGE_ATTRIBUTE, objects.named("embedded-java-runtime"))
                }
            }

            listOf(
                "testCompile",
                "testCompileOnly",
                "testRuntime",
                "testRuntimeOnly"
            ).forEach { configurationName ->
                val configuration = configurations.findByName(configurationName)

                configuration?.apply {
                    extendsFrom(jpsBuildTestDependencies)
                }

                val dependencyProjects = configuration
                    ?.dependencies
                    ?.mapNotNull { (it as? ProjectDependency)?.dependencyProject }

                dependencies {
                    dependencyProjects?.forEach { dependencyProject ->
                        add(jpsBuildTestDependencies.name, project(dependencyProject.path))
                    }
                }
            }
        }
    }

    rootProject.afterEvaluate {
        writeIdeaBuildNumberForTests()

        setupFirRunConfiguration()
        setupGenerateAllTestsRunConfiguration()
        updateCompilerXml()

        rootProject.allprojects {
            idea {
                module {
                    inheritOutputDirs = true
                }
            }

            if (this != rootProject) {
                evaluationDependsOn(path)
            }
        }

        rootProject.idea {
            project {
                settings {
                    compiler {
                        processHeapSize = 2000
                        addNotNullAssertions = true
                        parallelCompilation = true
                    }

                    delegateActions {
                        delegateBuildRunToGradle = false
                        testRunner = ActionDelegationConfig.TestRunner.PLATFORM
                    }

                    runConfigurations {

                        defaults<JUnit> {
                            configureForKotlin()
                        }

                        junit("[JPS] Compiler Tests") {
                            moduleName = "kotlin.compiler.test"
                            pattern = "org.jetbrains.kotlin.*"
                            configureForKotlin()
                        }

                        junit("[JPS] JVM Backend Tests") {
                            moduleName = "kotlin.idea.test"
                            pattern = "org.jetbrains.kotlin.codegen.*"
                            configureForKotlin()
                        }

                        junit("[JPS] JS Backend Tests") {
                            moduleName = "kotlin.js.js.tests.test"
                            pattern = "org.jetbrains.kotlin.js.test.*"
                            configureForKotlin()
                        }

                        junit("[JPS] Java 8 Tests") {
                            moduleName = "kotlin.compiler.tests-java8.test"
                            pattern = "org.jetbrains.kotlin.*"
                            configureForKotlin()
                        }
                    }
                }
            }
        }
    }
}
