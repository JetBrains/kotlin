plugins {
    `java-base`
}

val compilerClasspath = configurations.create("compilerClasspath") {
    isCanBeResolved = true
    isCanBeConsumed = false
}
val scriptsClasspath = configurations.create("scriptsClasspath") {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    compilerClasspath.name(project(":kotlin-compiler-embeddable"))
    compilerClasspath.name(project(":kotlin-scripting-compiler-embeddable"))

    scriptsClasspath.name(project(":gradle:regression-benchmark-templates"))
    scriptsClasspath.name(project(":kotlin-script-runtime"))
    scriptsClasspath.name(kotlinStdlib())
}

val service = project.extensions.getByType<JavaToolchainService>()

abstract class ScriptArgumentProvider @Inject constructor(
    layout: ProjectLayout,
    private val releaseKotlinVersion: String
) : CommandLineArgumentProvider {
    @get:Classpath
    abstract val scriptClasspath: ConfigurableFileCollection

    @get:Input
    abstract val script: Property<String>

    @get:OutputDirectory
    val scriptOutputDirectories: Provider<Directory> = layout.buildDirectory.dir("benchmark-script")

    override fun asArguments(): Iterable<String> {
        return listOf(
            "-no-reflect",
            "-no-stdlib",
            "-classpath", scriptClasspath.asFileTree.files.joinToString(separator = File.pathSeparator),
            "-script", "benchmarkScripts/${script.get()}",
            scriptOutputDirectories.get().asFile.absolutePath,
            releaseKotlinVersion
        )
    }
}

fun addBenchmarkTask(
    taskName: String,
    script: String,
    jdkVersion: JavaLanguageVersion = JavaLanguageVersion.of(8),
    additionalConfiguration: JavaExec.() -> Unit = {}
): TaskProvider<JavaExec> {
    return tasks.register<JavaExec>(taskName) {
        group = "Gradle Regression Benchmark"
        description = "Runs regression benchmark from $script"

        dependsOnKotlinGradlePluginInstall()

        outputs.upToDateWhen { false }
        javaLauncher.set(service.launcherFor {
            languageVersion.set(jdkVersion)
        })
        classpath = compilerClasspath
        mainClass.set("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")

        val scriptArgs = objects.newInstance<ScriptArgumentProvider>(version)
        scriptArgs.script.set(script)
        scriptArgs.scriptClasspath.from(scriptsClasspath)
        argumentProviders.add(scriptArgs)
        additionalConfiguration()
    }
}

val acceptAndroidSdkLicenses = tasks.register("acceptAndroidSdkLicenses") {
    useAndroidSdk()
    doLast { acceptAndroidSdkLicenses() }
}

fun JavaExec.usesAndroidSdk() {
    dependsOn(acceptAndroidSdkLicenses)

    doFirst {
        environment("ANDROID_HOME", configurations["androidSdk"].singleFile.canonicalPath)
    }
}

addBenchmarkTask(
    taskName = "benchmarkRegressionDuckduckgo",
    script = "duckduckgo.benchmark.kts",
    JavaLanguageVersion.of(17)
) {
    usesAndroidSdk()
}

addBenchmarkTask(
    taskName = "benchmarkRegressionGraphql",
    script = "graphql-kotlin.benchmark.kts",
    JavaLanguageVersion.of(11)
)

addBenchmarkTask(
    taskName = "benchmarkRegressionKvision",
    script = "kvision.benchmark.kts",
    JavaLanguageVersion.of(11)
)