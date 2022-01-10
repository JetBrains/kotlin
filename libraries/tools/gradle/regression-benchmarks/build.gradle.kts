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

        dependsOn(":kotlin-gradle-plugin:install")

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
    doLast {
        val androidSdk = configurations["androidSdk"].singleFile
        val sdkLicensesDir = androidSdk.resolve("licenses").also {
            if (!it.exists()) it.mkdirs()
        }

        val sdkLicenses = listOf(
            "8933bad161af4178b1185d1a37fbf41ea5269c55",
            "d56f5187479451eabf01fb78af6dfcb131a6481e",
            "24333f8a63b6825ea9c5514f83c2829b004d1fee",
        )
        val sdkPreviewLicense = "84831b9409646a918e30573bab4c9c91346d8abd"

        val sdkLicenseFile = sdkLicensesDir.resolve("android-sdk-license")
        if (!sdkLicenseFile.exists()) {
            sdkLicenseFile.createNewFile()
            sdkLicenseFile.writeText(
                sdkLicenses.joinToString(separator = "\n")
            )
        } else {
            sdkLicenses
                .subtract(
                    sdkLicenseFile.readText().lines()
                )
                .forEach {
                    sdkLicenseFile.appendText("$it\n")
                }
        }

        val sdkPreviewLicenseFile = sdkLicensesDir.resolve("android-sdk-preview-license")
        if (!sdkPreviewLicenseFile.exists()) {
            sdkPreviewLicenseFile.writeText(sdkPreviewLicense)
        } else {
            if (sdkPreviewLicense != sdkPreviewLicenseFile.readText().trim()) {
                sdkPreviewLicenseFile.writeText(sdkPreviewLicense)
            }
        }
    }
}

fun JavaExec.usesAndroidSdk() {
    dependsOn(acceptAndroidSdkLicenses)

    doFirst {
        environment("ANDROID_SDK_ROOT", configurations["androidSdk"].singleFile.canonicalPath)
    }
}

addBenchmarkTask(
    taskName = "benchmarkRegressionDuckduckgo",
    script = "duckduckgo.benchmark.kts",
    JavaLanguageVersion.of(11)
) {
    usesAndroidSdk()
}

addBenchmarkTask(
    taskName = "benchmarkRegressionGraphql",
    script = "graphql-kotlin.benchmark.kts"
)
