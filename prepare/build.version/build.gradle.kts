
import java.io.File

val buildVersionFilePath = "${rootProject.extra["distDir"]}/build.txt"

configurations.create("default")

artifacts.add("default", file(buildVersionFilePath))

task("make-build-version") {
    File(buildVersionFilePath).apply {
        parentFile.mkdirs()
        writeText(rootProject.extra["build.number"].toString())
    }
}

defaultTasks("make-build-version")

