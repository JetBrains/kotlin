
import java.io.File

val buildVersionFilePath = "${rootProject.extra["distDir"]}/build.txt"

val cfg = configurations.create("prepared-build-version")

artifacts.add(cfg.name, file(buildVersionFilePath))

task("make-build-version") {
    File(buildVersionFilePath).apply {
        parentFile.mkdirs()
        writeText(rootProject.extra["build.number"].toString())
    }
}

defaultTasks("make-build-version")

