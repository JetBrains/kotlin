import javax.inject.Inject

plugins {
    id("common-configuration")
    `java-library`
}

// The production `*-for-ide` artifacts that are directly consumed by the IntelliJ Kotlin plugin on its production classpath.
// Compiler plugin modules are deliberately excluded: they contain both K1 and K2 components.
// The scripting compiler plugin is an exception, as it's effectively a part of the compiler.
val forIdeModules = listOf(
    "analysis-api-for-ide",
    "analysis-api-impl-base-for-ide",
    "analysis-api-k2-for-ide",
    "analysis-api-platform-interface-for-ide",
    "analysis-api-standalone-for-ide",
    "kotlin-compiler-cli-for-ide",
    "kotlin-compiler-common-for-ide",
    "kotlin-compiler-fir-for-ide",
    "kotlin-compiler-ir-for-ide",
    "kotlin-gradle-statistics-for-ide",
    "kotlin-jps-common-for-ide",
    "kotlin-objcexport-header-generator-for-ide",
    "kotlin-swift-export-for-ide",
    "low-level-api-fir-for-ide",
    "scripting-compiler-plugin-for-ide",
    "symbol-light-classes-for-ide",
)

val proguardTool = configurations.create("proguardTool") {
    isCanBeConsumed = false
}

// All jars packed into the `*-for-ide` artifacts, joined into a single classpath.
// The configuration is non-transitive: the dependency metadata of the `*-for-ide` artifacts is broken anyway.
val forIdeArtifactJars = configurations.create("forIdeArtifactJars") {
    isCanBeConsumed = false
    isTransitive = false
}

// Dependencies that the IntelliJ platform provides to the Kotlin plugin at runtime.
val ideProvidedClasspath = configurations.create("ideProvidedClasspath") {
    isCanBeConsumed = false

    // IntelliJ dependencies not referenced by the compiler. Excluded to keep 'verification-metadata.xml' smaller.
    exclude(group = "dk.brics")
    exclude(group = "one.util")

    // References to 'com.intellij.util.diff' are suppressed in 'classpath-check.pro' instead, as in 'analysis-api.pro'.
    exclude(group = "com.jetbrains.intellij.platform", module = "util-diff")
}

forIdeArtifactJars.dependencies.addAllLater(
    provider {
        forIdeModules
            .map { project(":prepare:ide-plugin-dependencies:$it") }
            .flatMap { forIdeProject ->
                // 'for-ide' projects don't expose a consumable artifact without `-Ppublish.ide.plugin.dependencies=true`.
                val exportedDependencies = forIdeProject.configurations["api"].dependencies
                check(exportedDependencies.isNotEmpty()) { "No exported dependencies found in ${forIdeProject.path}" }
                // Copied to avoid sharing dependency instances with the `*-for-ide` projects
                exportedDependencies.map { it.copy() }
            }
    }
)

dependencies {
    // Kotlin scripting is shipped to the IDE as separate artifacts, but is effectively a part
    // of the compiler, so it belongs to the checked artifact set
    forIdeArtifactJars(project(":kotlin-script-runtime"))
    forIdeArtifactJars(project(":kotlin-scripting-common"))
    forIdeArtifactJars(project(":kotlin-scripting-jvm"))
    forIdeArtifactJars(project(":kotlin-scripting-compiler-impl"))

    // `java-psi-impl` transitively covers the whole IntelliJ core surface
    // (java-psi, core-impl, core, util, util-base, extensions, syntax, asm-all, ...)
    ideProvidedClasspath("com.jetbrains.intellij.java:java-psi-impl:$intellijVersion")
    ideProvidedClasspath(jpsModelImpl())
    ideProvidedClasspath(jpsModelSerialization())
    ideProvidedClasspath(project(":kotlin-stdlib"))
    ideProvidedClasspath(project(":kotlin-reflect"))
    ideProvidedClasspath(project(":kotlin-tooling-core"))
    ideProvidedClasspath(libs.kotlinx.coroutines.core.jvm)
    ideProvidedClasspath(libs.gson)
    ideProvidedClasspath(libs.caffeine)

    // Align the transitive tool dependencies with the versions already used in the repository,
    // so 'verification-metadata.xml' doesn't need to be extended with additional versions
    proguardTool(libs.proguard.base) {
        exclude(group = "org.jetbrains.kotlin")
    }
    proguardTool(project(":kotlin-stdlib"))
    constraints {
        proguardTool(libs.log4j2.api)
        proguardTool(libs.log4j2.core)
        proguardTool("org.json:json:20231013")
    }
}

val mergedClasspathJar = tasks.register<Jar>("mergedClasspathJar") {
    description = "Merges all *-for-ide artifact JARs into a single JAR for the ProGuard classpath check"
    destinationDirectory.set(layout.buildDirectory.dir("proguard"))
    archiveFileName.set("merged-classpath.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    dependsOn(forIdeArtifactJars)
    from({ forIdeArtifactJars.map(::zipTree) })
}

val proguardClasspathWarnings = tasks.register<ProguardClasspathWarningsTask>("proguardClasspathWarnings") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs ProGuard over the merged classpath of the *-for-ide artifacts and captures its warnings"

    proguardClasspath.from(proguardTool)
    inputJars.from(mergedClasspathJar)
    libraryJars.from(ideProvidedClasspath)
    proguardConfigFile.set(layout.projectDirectory.file("classpath-check.pro"))
    log4jConfigFile.set(layout.projectDirectory.file("proguard-log4j2.xml"))
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_21_0))
    warningsFile.set(layout.buildDirectory.file("proguard/proguard-warnings.txt"))
}

tasks.register<ProguardWarningsGoldenFileTask>("checkProguardWarnings") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Verifies that ProGuard warnings for the *-for-ide artifact set match the golden file"

    actualWarningsFile.set(proguardClasspathWarnings.flatMap { it.warningsFile })
    goldenFile.set(layout.projectDirectory.file("proguard-warnings.txt"))
}

tasks.check {
    dependsOn(tasks.withType<ProguardWarningsGoldenFileTask>())
}

/**
 * Runs ProGuard as an external process, capturing its output.
 *
 * Unlike [proguard.gradle.ProGuardTask], this task doesn't fail on unresolved references
 * (the configuration file is expected to contain `-ignorewarnings`), and the printed warnings
 * are available for further checks in [warningsFile].
 */
@CacheableTask
abstract class ProguardClasspathWarningsTask @Inject constructor(
    private val execOperations: ExecOperations,
) : DefaultTask() {
    @get:Classpath
    abstract val proguardClasspath: ConfigurableFileCollection

    @get:Classpath
    abstract val inputJars: ConfigurableFileCollection

    @get:Classpath
    abstract val libraryJars: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val proguardConfigFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val log4jConfigFile: RegularFileProperty

    @get:Internal
    abstract val javaLauncher: Property<JavaLauncher>

    // Track the JDK by its version instead of the machine-specific installation path
    @get:Input
    val jdkVersion: Provider<String>
        get() = javaLauncher.map { it.metadata.languageVersion.toString() }

    @get:OutputFile
    abstract val warningsFile: RegularFileProperty

    @TaskAction
    fun run() {
        val launcher = javaLauncher.get()
        val jmodsDir = launcher.metadata.installationPath.asFile.resolve("jmods")
        val outputFile = warningsFile.get().asFile
        outputFile.parentFile.mkdirs()
        outputFile.outputStream().buffered().use { out ->
            execOperations.javaexec {
                executable(launcher.executablePath.asFile.absolutePath)
                classpath(proguardClasspath)
                mainClass.set("proguard.ProGuard")
                maxHeapSize = "4g"
                jvmArgs("-Dlog4j2.configurationFile=${log4jConfigFile.get().asFile.absolutePath}")
                args("@${proguardConfigFile.get().asFile.absolutePath}")
                inputJars.forEach { args("-injars", it.absolutePath) }
                args("-libraryjars", jmodsDir.absolutePath)
                libraryJars.forEach { args("-libraryjars", it.absolutePath) }
                standardOutput = out
                errorOutput = out
            }.assertNormalExitValue()
        }
    }
}

/**
 * Compares the actual ProGuard output against the golden file, following the convention of
 * `CheckForeignClassUsageTask`: on mismatch, the golden file is updated with the actual content,
 * and the task fails asking to review and commit the changes.
 */
abstract class ProguardWarningsGoldenFileTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val actualWarningsFile: RegularFileProperty

    @get:OutputFile
    abstract val goldenFile: RegularFileProperty

    @TaskAction
    fun check() {
        val actualLines = actualWarningsFile.get().asFile.readText().lines().dropLastWhile { it.isEmpty() }
        val actualText = actualLines.joinToString("\n", postfix = "\n")
        val expectedFile = goldenFile.get().asFile

        if (!expectedFile.exists()) {
            expectedFile.writeText(actualText)
            throw GradleException("${expectedFile.name} did not exist and has been created. Please review and commit the changes")
        }

        val expectedLines = expectedFile.readText().lines()
        if (expectedLines != actualText.lines()) {
            expectedFile.writeText(actualText)
            val diffPreview = buildString {
                fun appendLines(prefix: String, lines: List<String>) {
                    lines.take(MAX_REPORTED_LINES).forEach { appendLine("$prefix $it") }
                    if (lines.size > MAX_REPORTED_LINES) appendLine("$prefix ... and ${lines.size - MAX_REPORTED_LINES} more lines")
                }
                appendLines("+", actualLines - expectedLines.toSet())
                appendLines("-", expectedLines - actualLines.toSet())
            }
            throw GradleException(
                "The ProGuard classpath check output for the IDE plugin dependencies has changed, " +
                        "so ${expectedFile.name} has been updated. Please review and commit the changes\n" + diffPreview
            )
        }
    }

    companion object {
        private const val MAX_REPORTED_LINES = 30
    }
}
