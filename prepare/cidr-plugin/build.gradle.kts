import org.jetbrains.kotlin.ultimate.*

plugins {
    kotlin("jvm")
}

val originalPluginJar by configurations.creating

// Do not rename, used in pill importer
val projectsToShadow by extra(listOf(ultimatePath(":cidr-native")))

dependencies {
    originalPluginJar(ideaPluginJarDep())
}

// Extract plugin.xml from the original Kotlin plugin, patch this file to exclude non-CIDR stuff and version information,
// and then save under new name KotlinPlugin.xml.
val kotlinPluginXml by tasks.creating {
    inputs.files(originalPluginJar)
    outputs.files("$buildDir/$name/$kotlinPluginXmlPath")

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
            .also { pluginXmlText -> outputs.files.singleFile.writeText(pluginXmlText) }
    }
}

// Pack Jar file with patched KotlinPlugin.xml plus shadowed project classes.
val jar = pluginJar {
    dependsOn(originalPluginJar)

    lazyFrom { zipTree(originalPluginJar.singleFile).matching { exclude(pluginXmlPath) } }
    from(kotlinPluginXml) { into("META-INF") }

    for (p in projectsToShadow) {
        dependsOn("$p:classes")
        from(getMainSourceSetOutput(p))
    }
}
