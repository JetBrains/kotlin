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
    inputs.files(kotlinPlugin)
    outputs.files(fileFrom(buildDir, name, pluginXmlPath))

    doFirst {
        val placeholderRegex = Regex(
            """<!-- CIDR-PLUGIN-PLACEHOLDER-START -->(.*)<!-- CIDR-PLUGIN-PLACEHOLDER-END -->""",
            RegexOption.DOT_MATCHES_ALL)

        val excludeRegex = Regex(
            """<!-- CIDR-PLUGIN-EXCLUDE-START -->(.*?)<!-- CIDR-PLUGIN-EXCLUDE-END -->""",
            RegexOption.DOT_MATCHES_ALL)

        val ideaVersionRegex = Regex("""<idea-version[^/>]+/>""".trimMargin())

        val versionRegex = Regex("""<version>([^<]+)</version>""")

        zipTree(inputs.files.singleFile)
            .matching { include(pluginXmlPath) }
            .singleFile
            .readText()
            .replace(placeholderRegex, "<depends>com.intellij.modules.cidr.lang</depends>")
            .replace(excludeRegex, "")
            .replace(ideaVersionRegex, "") // IDEA version to be specified in CLion or AppCode plugin.xml file.
            .replace(versionRegex, "") // Version to be specified in CLion or AppCode plugin.xml file.
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
    from(jar) { into("lib") }
}
