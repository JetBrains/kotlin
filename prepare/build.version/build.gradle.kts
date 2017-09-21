
import java.io.File

val buildVersionFilePath = "${rootProject.extra["distDir"]}/build.txt"

val buildVersion by configurations.creating

val prepare = task("prepare") {
    val versionString = rootProject.extra["buildNumber"].toString()
    val versionFile = File(buildVersionFilePath)
    outputs.file(buildVersionFilePath)
    outputs.upToDateWhen {
        (versionFile.exists() && versionFile.readText().trim() == versionString).also {
            if (!it) {
                logger.info("$versionFile is not up-to-date: ${versionFile.takeIf { it.exists() }?.readText()?.trim()}")
            }
        }
    }
    doLast {
        versionFile.parentFile.mkdirs()
        versionFile.writeText(versionString)
    }
}

artifacts.add(buildVersion.name, file(buildVersionFilePath)) {
    builtBy(prepare)
}

val distKotlinHomeDir: String by rootProject.extra

val dist by task<Copy> {
    into(File(distKotlinHomeDir))
    from(buildVersionFilePath)
}
