@file:Suppress("UnstableApiUsage")

import org.gradle.jvm.tasks.Jar
import org.jetbrains.gradle.ext.ActionDelegationConfig
import org.jetbrains.gradle.ext.JUnit
import org.jetbrains.gradle.ext.RecursiveArtifact
import org.jetbrains.gradle.ext.TopLevelArtifact
import org.jetbrains.kotlin.ideaExt.*


val ideaPluginDir: File by extra
val ideaSandboxDir: File by extra
val ideaSdkPath: String
    get() = rootProject.ideaHomePathForTests().absolutePath

fun updateCompilerXml() {
    val modulesExcludedFromJps = listOf(
        "buildSrc",
        "native/commonizer",
        "plugins/atomicfu/atomicfu-runtime",
        "libraries/tools/atomicfu",
        "libraries/tools/binary-compatibility-validator",
        "libraries/tools/dukat",
        "libraries/tools/gradle",
        "libraries/tools/jdk-api-validator",
        "libraries/tools/kotlin-allopen",
        "libraries/tools/kotlin-annotation-processing",
        "libraries/tools/kotlin-assignment",
        "libraries/tools/kotlin-bom",
        "libraries/tools/kotlin-gradle-build-metrics",
        "libraries/tools/kotlin-gradle-plugin",
        "libraries/tools/kotlin-gradle-plugin-api",
        "libraries/tools/kotlin-gradle-plugin-dsl-codegen",
        "libraries/tools/kotlin-gradle-plugin-idea",
        "libraries/tools/kotlin-gradle-plugin-idea-for-compatibility-tests",
        "libraries/tools/kotlin-gradle-plugin-integration-tests",
        "libraries/tools/kotlin-gradle-plugin-kpm-android",
        "libraries/tools/kotlin-gradle-plugin-tcs-android",
        "libraries/tools/kotlin-gradle-plugin-model",
        "libraries/tools/kotlin-gradle-plugin-npm-versions-codegen",
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
        "libraries/tools/kotlin-prepush-hook",
        "libraries/tools/kotlin-project-model",
        "libraries/tools/kotlin-sam-with-receiver",
        "libraries/tools/kotlin-serialization",
        "libraries/tools/kotlin-serialization-unshaded",
        "libraries/tools/kotlin-stdlib-docs",
        "libraries/tools/kotlin-stdlib-gen",
        "libraries/tools/kotlin-test-js-runner",
        "libraries/tools/kotlin-tooling-core",
        "libraries/tools/kotlin-tooling-metadata",
        "libraries/tools/kotlinp",
        "libraries/tools/maven-archetypes",
        "libraries/tools/mutability-annotations-compat",
        "libraries/tools/script-runtime",
        "libraries/scripting/dependencies-maven-all",
        "native/commonizer-api",
        "libraries/examples",
        "libraries/tools/kotlin-gradle-plugin-idea-proto",
        "libraries/tools/kotlin-project-model-tests-generator",
        "repo/gradle-settings-conventions",
        "plugins/fir-plugin-prototype/plugin-annotations",
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
          ${excludeEntries}
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
        "-Djna.nosys=true",
        "-Didea.platform.prefix=Idea",
        "-Didea.is.unit.test=true",
        "-Didea.ignore.disabled.plugins=true",
        "-Didea.home.path=$ideaSdkPath",
        "-Didea.use.native.fs.for.win=false",
        "-Djps.kotlin.home=${ideaPluginDir.absolutePath}",
        "-Duse.jps=true",
        "-Djava.awt.headless=true"
    ).filterNotNull().joinToString(" ")

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
    """.trimMargin())
}

// Needed because of idea.ext plugin doesn't allow to set TEST_SEARCH_SCOPE = moduleWithDependencies
fun setupFirRunConfiguration() {

    val junit = JUnit("_stub").apply { configureForKotlin("2048m") }
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
                    dependencyProjects?.forEach {dependencyProject ->
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
                    ideArtifacts {
                        kotlinCompilerJar()

                        kotlinReflectJar()

                        kotlinCompilerClientEmbeddableJar()

                        kotlinMainKtsJar()

                        kotlinImportsDumperCompilerPluginJar()

                        kotlinDaemonClientJar()

                        kotlinc()

                        dist()
                    }

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

                        // todo: replace `pattern` with `package`, when `com.intellij.execution.junit.JUnitRunConfigurationImporter#process` will be fixed
                        junit("[JPS] All IDEA Plugin Tests") {
                            moduleName = "kotlin.idea.test"
                            pattern = "org.jetbrains.kotlin.*"
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

val jarArtifactProjects = listOf(
    "kotlin-compiler-client-embeddable",
    "kotlin-compiler",
    "kotlin-daemon-client",
    "kotlin-imports-dumper-compiler-plugin",
    "kotlin-jps-plugin",
    "kotlin-main-kts",
    "kotlin-reflect"
)

fun NamedDomainObjectContainer<TopLevelArtifact>.kotlinCompilerJar() =
    jarFromProject(project(":kotlin-compiler"), "kotlin-compiler.jar")

fun NamedDomainObjectContainer<TopLevelArtifact>.kotlinReflectJar() =
    jarFromProject(project(":kotlin-reflect"))

fun NamedDomainObjectContainer<TopLevelArtifact>.kotlinCompilerClientEmbeddableJar() =
    jarFromProject(project(":kotlin-compiler-client-embeddable"))

fun NamedDomainObjectContainer<TopLevelArtifact>.kotlinMainKtsJar() {
    val mainKtsProject = project(":kotlin-main-kts")
    jarFromProject(mainKtsProject) {
        directoryContent("${mainKtsProject.rootDir}/jar-resources")
    }
}

fun NamedDomainObjectContainer<TopLevelArtifact>.kotlinPluginJar() =
    jarFromProject(project(":prepare:idea-plugin"), "kotlin-plugin.jar")

fun NamedDomainObjectContainer<TopLevelArtifact>.kotlinImportsDumperCompilerPluginJar() =
    jarFromProject(project(":kotlin-imports-dumper-compiler-plugin"))

fun NamedDomainObjectContainer<TopLevelArtifact>.kotlinDaemonClientJar() =
    jarFromProject(project(":kotlin-daemon-client"))

fun NamedDomainObjectContainer<TopLevelArtifact>.kotlinJpsPluginJar() {
    val jpsPluginProject = project(":kotlin-jps-plugin")
    jarFromProject(jpsPluginProject) {
        file("${jpsPluginProject.rootDir}/resources/kotlinManifest.properties")
    }
}

fun NamedDomainObjectContainer<TopLevelArtifact>.dist() {
    val distLibrariesProject = project(":kotlin-stdlib:jps-build")
    val stdlibMinimal by distLibrariesProject.configurations
    val commonStdlib by distLibrariesProject.configurations
    val commonStdlibSources by distLibrariesProject.configurations
    val stdlibSources by distLibrariesProject.configurations

    create("dist") {
        file("$rootDir/build/build.txt")

        // Use output-file-name when fixed https://github.com/JetBrains/gradle-idea-ext-plugin/issues/63
        archive("kotlin-stdlib-jvm-minimal-for-test.jar") {
            extractedDirectory(stdlibMinimal.singleFile)
        }

        directory("artifacts") {
            directory("ideaPlugin") {
                artifact("ideaPlugin")
            }
        }

        directory("common") {
            // Use output-file-name when fixed https://github.com/JetBrains/gradle-idea-ext-plugin/issues/63
            archive("kotlin-stdlib-common.jar") {
                extractedDirectory(commonStdlib.filter {
                    it.name.contains("stdlib")
                }.singleFile)
            }

            // Use output-file-name when fixed https://github.com/JetBrains/gradle-idea-ext-plugin/issues/63
            archive("kotlin-stdlib-common-sources.jar") {
                extractedDirectory(commonStdlibSources.filter {
                    it.name.contains("stdlib")
                }.singleFile)
            }
        }

        directory("kotlinc") {
            artifact("kotlinc")
        }

        directory("maven") {
            // Use output-file-name when fixed https://github.com/JetBrains/gradle-idea-ext-plugin/issues/63
            archive("kotlin-stdlib-sources.jar") {
                extractedDirectory(stdlibSources.singleFile)
            }
        }
    }
}

fun NamedDomainObjectContainer<TopLevelArtifact>.kotlinc() {
    val kotlinCompilerProject = project(":kotlin-compiler")
    val libraries by kotlinCompilerProject.configurations
    val compilerPlugins by kotlinCompilerProject.configurations
    val sources by kotlinCompilerProject.configurations

    create("kotlinc") {
        directory("bin") {
            directoryContent("$rootDir/compiler/cli/bin")
        }

        directory("lib") {
            artifact("kotlin-compiler.jar")
            jarsFromConfiguration(libraries) { it.replace("-$bootstrapKotlinVersion", "") }
            jarsFromConfiguration(compilerPlugins) { it.removePrefix("kotlin-") }
            sourceJarsFromConfiguration(sources) { it.replace("-$bootstrapKotlinVersion", "") }
        }

        directory("license") {
            directoryContent("$rootDir/license")
        }

        file("$rootDir/bootstrap/build.txt")
    }
}

fun NamedDomainObjectContainer<TopLevelArtifact>.ideaPlugin() {
    val ideaPluginProject = project(":prepare:idea-plugin")
    val libraries by ideaPluginProject.configurations
    val jpsPlugin by ideaPluginProject.configurations

    create("ideaPlugin") {
        directory("Kotlin") {
            directory("kotlinc") {
                artifact("kotlinc")
            }

            directory("lib") {
                artifact("kotlin-plugin.jar")

                jarsFromConfiguration(libraries) { it.replace("-$bootstrapKotlinVersion", "") }

                directory("jps") {
                    jarsFromConfiguration(jpsPlugin)
                }
            }
        }
    }
}

fun NamedDomainObjectContainer<TopLevelArtifact>.jarFromProject(project: Project, name: String? = null, configureAction: RecursiveArtifact.() -> Unit = {}) {
    val jarName = name ?: project.name + ".jar"
    create(jarName) {
        archiveFromProject(project, jarName, configureAction)
    }
}

fun RecursiveArtifact.archiveFromProject(project: Project, name: String? = null, configureAction: RecursiveArtifact.() -> Unit = {}) {
    val jarName = name ?: project.name + ".jar"
    archive(jarName) {

        var foundManifest = false
        fun extractManifest(jar: Jar) {
            if (jar.enabled && !foundManifest) {
                val manifestPath = jar.temporaryDir.resolve("MANIFEST.MF")
                jar.manifest.writeTo(manifestPath)
                directory("META-INF") {
                    file(manifestPath)
                }
                foundManifest = true
            }
        }


        (project.tasks.findByName("modularJar") as? Jar)?.let(::extractManifest)
        (project.tasks.findByName("resultJar") as? Jar)?.let(::extractManifest)
        (project.tasks.findByName("result") as? Jar)?.let(::extractManifest)
        (project.tasks.findByName("shadowJar") as? Jar)?.let(::extractManifest)
        (project.tasks["jar"] as? Jar)?.let(::extractManifest)

        if (!foundManifest) error("No manifest found for jar: $jarName in ${project.name}")

        if (project.sourceSets.names.contains("main")) {
            moduleOutput(moduleName(project.path))
        }

        jarContentsFromEmbeddedConfiguration(project)

        configureAction()
    }
}

fun moduleName(projectPath: String) = rootProject.name + projectPath.replace(':', '.') + ".main"

fun RecursiveArtifact.jarContentsFromEmbeddedConfiguration(project: Project) {
    val embedded = project.configurations.findByName("embedded") ?: return
    jarContentsFromConfiguration(embedded)
}

fun RecursiveArtifact.jarContentsFromConfiguration(configuration: Configuration) {
    val resolvedArtifacts = configuration
        .resolvedConfiguration
        .resolvedArtifacts

    resolvedArtifacts.filter { it.id.componentIdentifier is ModuleComponentIdentifier }
        .map { it.file }
        .forEach { extractedDirectory(it) }

    resolvedArtifacts
        .map { it.id.componentIdentifier }
        .filterIsInstance<ProjectComponentIdentifier>()
        .forEach {
            moduleOutput(moduleName(it.projectPath))
            jarContentsFromEmbeddedConfiguration(project(it.projectPath))
        }
}

fun RecursiveArtifact.sourceJarsFromConfiguration(configuration: Configuration, renamer: (String) -> String = { it }) {
    val resolvedArtifacts = configuration
        .resolvedConfiguration
        .resolvedArtifacts

    jarsFromExternalModules(resolvedArtifacts, renamer)

    resolvedArtifacts
        .map { it.id.componentIdentifier }
        .filterIsInstance<ProjectComponentIdentifier>()
        .forEach {
            val jarBaseName = project(it.projectPath).the<BasePluginExtension>().archivesName.get()
            val renamed = renamer("$jarBaseName-sources") + ".jar"
            archive(renamed) {
                project(it.projectPath)
                    .mainSourceSet
                    .allSource
                    .sourceDirectories
                    .forEach {sourceDirectory ->
                    directoryContent(sourceDirectory)
                }
            }
        }
}

fun RecursiveArtifact.jarsFromConfiguration(configuration: Configuration, renamer: (String) -> String = { it }) {
    val resolvedArtifacts = configuration
        .resolvedConfiguration
        .resolvedArtifacts

    jarsFromExternalModules(resolvedArtifacts, renamer)

    resolvedArtifacts
        .map { it.id.componentIdentifier }
        .filterIsInstance<ProjectComponentIdentifier>()
        .forEach {
            val jarBaseName = project(it.projectPath).the<BasePluginExtension>().archivesName.get()
            val artifactName = renamer(jarBaseName) + ".jar"
            if (it.projectName in jarArtifactProjects) {
                artifact(artifactName)
            } else {
                archiveFromProject(project(it.projectPath), artifactName)
            }
        }
}

fun RecursiveArtifact.jarsFromExternalModules(resolvedArtifacts: Iterable<ResolvedArtifact>, renamer: (String) -> String = { it }) {
    // Use output-file-name property when fixed https://github.com/JetBrains/gradle-idea-ext-plugin/issues/63
    resolvedArtifacts.filter { it.id.componentIdentifier is ModuleComponentIdentifier }
        .forEach {
            val baseName = it.file.nameWithoutExtension
            val renamed = renamer(baseName)
            if (it.file.extension == "jar" && renamed != baseName) {
                archive("$renamed.jar") {
                    extractedDirectory(it.file)
                }
            } else {
                file(it.file)
            }
        }
}
