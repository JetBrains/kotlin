import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import groovy.lang.Closure
import java.io.FilterReader

description = "Kotlin AppCode & CLion plugin"

apply {
    plugin("kotlin")
}

val ideaPluginDir: File by rootProject.extra
val cidrPluginDir: File by rootProject.extra

val kotlinPlugin by configurations.creating

val pluginXmlPath = "META-INF/plugin.xml"

dependencies {
    kotlinPlugin(project(":prepare:idea-plugin", configuration = "runtimeJar"))
}

val pluginXml by tasks.creating {
    val kotlinVersion: String by rootProject.extra
    val pluginFullVersionNumber = findProperty("pluginVersion") as? String
            ?: "$kotlinVersion-CIDR"

    inputs.property("pluginFullVersionNumber", pluginFullVersionNumber)
    inputs.files(kotlinPlugin)
    outputs.files(File(buildDir, name, pluginXmlPath))

    doFirst {
        val placeholderRegex = Regex(
                """<!-- CLION-PLUGIN-PLACEHOLDER-START -->(.*)<!-- CLION-PLUGIN-PLACEHOLDER-END -->""",
                RegexOption.DOT_MATCHES_ALL)
        val versionRegex = Regex("""<version>([^<]+)</version>""")

        zipTree(inputs.files.singleFile)
            .matching { include(pluginXmlPath) }
            .singleFile
            .readText()
            .replace(placeholderRegex, "")
            .replace(versionRegex, "<version>$pluginFullVersionNumber</version>")
            .also { pluginXmlText ->
                outputs.files.singleFile.writeText(pluginXmlText)
            }
    }
}

val jar = runtimeJar {
    archiveName = "kotlin-plugin.jar"
    dependsOn(kotlinPlugin)
    from {
        zipTree(kotlinPlugin.singleFile).matching {
            exclude(pluginXmlPath)
        }
    }
    from(pluginXml) { into("META-INF") }
}

task<Copy>("cidrPlugin") {
    into(cidrPluginDir)
    from(ideaPluginDir) { exclude("lib/kotlin-plugin.jar") }
    from(jar) { into("lib") }
}