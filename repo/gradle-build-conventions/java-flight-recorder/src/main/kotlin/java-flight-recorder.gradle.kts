import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.withType

val pluginBuildDir = "jfr"

tasks.withType<Test>().configureEach {
    val testTask = this
    val jfcFile = defaultJfcFile()
    val jfrFile = defaultJfrFileFor(testTask)

    val jfrExtension = extensions.create<JfrExtension>("javaFlightRecorder")
    jfrExtension.jfcFile.convention(jfcFile)
    jfrExtension.jfrFile.convention(jfrFile)

    testTask.outputs.file(jfrExtension.jfrFile) // inform testTask that it builds jfrFile
    jfrExtension.jfrFile.builtBy(testTask) // inform other tasks that jfrFile is built by testTask

    val jfrArgumentProvider = objects.newInstance<JfrArgumentProvider>().apply {
        this.jfcFile.set(jfrExtension.jfcFile)
        this.jfrFile.from(jfrExtension.jfrFile)
        this.javaLauncher.set(testTask.javaLauncher)
    }
    testTask.jvmArgumentProviders.add(jfrArgumentProvider)
}

fun defaultJfcFile(): RegularFile {
    val isTeamcityBuild = kotlinBuildProperties.isTeamcityBuild.get()
    return layout.settingsDirectory.file(if (isTeamcityBuild) "tests/jfr/teamcity.jfc" else "tests/jfr/local.jfc")
}

fun defaultJfrFileFor(testTask: Test): Provider<RegularFile> {
    val jfrFile = layout.buildDirectory.file("$pluginBuildDir/${testTask.name}.jfr")
    testTask.doFirst { jfrFile.get().asFile.parentFile.mkdirs() }
    return jfrFile
}
