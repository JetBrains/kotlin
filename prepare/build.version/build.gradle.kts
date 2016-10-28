
import java.io.File

val buildVersionFilePath = "${rootProject.extra["distDir"]}/build.txt"

val mainCfg = configurations.create("default")

artifacts.add(mainCfg.name, file(buildVersionFilePath))

val mainTask = task("prepare") {
    File(buildVersionFilePath).apply {
        parentFile.mkdirs()
        writeText(rootProject.extra["build.number"].toString())
    }
}

defaultTasks(mainTask.name)

