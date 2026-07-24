import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.withType

val pluginBuildDir = "jfr"

tasks.withType<Test>().configureEach {
    val testTask = this

    val jfrExtension = extensions.create<JfrExtension>("javaFlightRecorder").apply {
        jfcFile.convention(defaultJfcFile())
        jfrFile.convention(defaultJfrFileFor(testTask.name))

        testTask.outputs.file(jfrFile) // inform testTask that it builds jfrFile
        jfrFile.builtBy(testTask) // inform other tasks that jfrFile is built by testTask
    }

    testTask.jvmArgumentProviders += objects.newInstance<JfrArgumentProvider>().apply {
        jfcFile.set(jfrExtension.jfcFile)
        jfrFile.from(jfrExtension.jfrFile)
        javaLauncher.set(testTask.javaLauncher)
    }

    testTask.doFirst {
        jfrExtension.jfrFile.singleFile.parentFile.mkdirs()
    }
}

fun defaultJfcFile(): RegularFile {
    val isTeamcityBuild = kotlinBuildProperties.isTeamcityBuild.get()
    return layout.settingsDirectory.file(if (isTeamcityBuild) "tests/jfr/teamcity.jfc" else "tests/jfr/local.jfc")
}

fun defaultJfrFileFor(testTaskName: String): Provider<RegularFile> =
    layout.buildDirectory.file("$pluginBuildDir/$testTaskName.jfr")
