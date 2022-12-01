import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

val mainSourceSet: SourceSet = sourceSets["main"]
val functionalTestSourceSet: SourceSet = sourceSets.create("functionalTest") {
    compileClasspath += mainSourceSet.output
    runtimeClasspath += mainSourceSet.output

    configurations.getByName(implementationConfigurationName) {
        extendsFrom(configurations.getByName(mainSourceSet.implementationConfigurationName))
        extendsFrom(configurations.getByName("testImplementation"))
    }

    configurations.getByName(runtimeOnlyConfigurationName) {
        extendsFrom(configurations.getByName(mainSourceSet.runtimeOnlyConfigurationName))
        extendsFrom(configurations.getByName("testRuntimeOnly"))
    }
}

project.extensions.getByType<KotlinJvmProjectExtension>().target.compilations {
    named(functionalTestSourceSet.name) {
        associateWith(this@compilations.getByName("main"))
        associateWith(this@compilations.getByName("common"))
    }
}

val functionalTest by tasks.register<Test>("functionalTest") {
    group = JavaBasePlugin.VERIFICATION_GROUP
    description = "Runs functional tests"
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    workingDir = projectDir
    dependsOnKotlinGradlePluginInstall()

    testLogging {
        events("passed", "skipped", "failed")
    }
}

val forbidenApiCheck by tasks.register<org.jetbrains.gradle.plugins.tools.InspectForForbidenAPIUsage>("forbidenApiCheck") {
    forbiddenMethods.set(listOf("java.io.File.getCanonicalFile", "java.io.File.getCanonicalPath"))
    ignoreClasses.set(listOf(
        "org.jetbrains.kotlin.gradle.tasks.KotlinNativeTasksKt",
    "org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeCompilerArgBuilderKt",
        "org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest",
        "org.jetbrains.kotlin.gradle.tasks.CInteropProcess",
        "org.jetbrains.kotlin.gradle.targets.js.yarn.YarnEntryRegistry",
        "org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig",
        "org.jetbrains.kotlin.gradle.targets.js.ir.KotlinBrowserJsIr\$configureRun\$2\$runTask\$1\$3",
        "org.jetbrains.kotlin.gradle.targets.js.ir.KotlinBrowserJsIr\$configureRun\$2\$runTask\$1\$4",
        "org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenExec",
        "org.jetbrains.kotlin.gradle.targets.js.d8.D8Exec",
        "org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink",
        "org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec",
        "org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependencyKt",
        "org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectModules",
        "org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma\$useCoverage\$3",
        "org.jetbrains.kotlin.gradle.targets.js.subtargets.KotlinBrowserJs\$configureRun\$1\$runTask\$1\$3",
        "org.jetbrains.kotlin.gradle.targets.js.subtargets.KotlinBrowserJs\$configureRun\$1\$runTask\$1\$4",
        "org.jetbrains.kotlin.gradle.targets.js.subtargets.KotlinJsSubTarget\$configure\$1\$1"
        ))
    sourceSets.filter { !it.name.contains("test", false) }.forEach { sourceSet ->
        validationClasspath += sourceSet.output
    }
}

tasks.named("check") {
    dependsOn(functionalTest)
    dependsOn(forbidenApiCheck)
}
