
import java.io.File

val buildVersionFilePath = "${rootProject.extra["distDir"]}/build.txt"

val mainCfg = configurations.create("default")

artifacts.add(mainCfg.name, file(buildVersionFilePath))

val mainTask = task("prepare") {
    val versionString = rootProject.extra["build.number"].toString()
    val versionFile = File(buildVersionFilePath)
    outputs.file(buildVersionFilePath)
    outputs.upToDateWhen {
        (versionFile.exists() && versionFile.readText().trim() == versionString).apply {
            if (!this) {
                println("!!! not up-to-date $versionFile: ${versionFile.takeIf { it.exists() }?.readText()?.trim()}")
            }
        }
    }
    doLast {
        versionFile.parentFile.mkdirs()
        versionFile.writeText(versionString)
    }
}

defaultTasks(mainTask.name)

