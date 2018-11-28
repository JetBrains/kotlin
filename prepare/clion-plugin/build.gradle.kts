import com.github.jk1.tcdeps.KotlinScriptDslAdapter.teamcityServer
import com.github.jk1.tcdeps.KotlinScriptDslAdapter.tc
import java.util.regex.Pattern

apply {
    plugin("kotlin")
}

plugins {
    id("com.github.jk1.tcdeps") version "0.17"
}

repositories {
    teamcityServer {
        setUrl("http://buildserver.labs.intellij.net")
        credentials {
            username = "guest"
            password = "guest"
        }
    }
}

val kotlinVersion = rootProject.extra["kotlinVersion"] as String

val cidrPluginDir: File by rootProject.extra
val clionPluginDir: File by rootProject.extra
val clionVersion = rootProject.extra["versions.clion"] as String
val clionVersionRepo = rootProject.extra["versions.clion.repo"] as String

val cidrPlugin by configurations.creating
val platformDepsZip by configurations.creating

val pluginXmlPath = "META-INF/plugin.xml"
val javaPsiXmlPath = "META-INF/JavaPsiPlugin.xml"
val javaPluginXmlPath = "META-INF/JavaPlugin.xml"

val platformDepsJarName = "kotlinNative-platformDeps.jar"
val pluginXmlLocation = File(buildDir, "pluginXml")

// Do not rename, used in JPS importer
val projectsToShadow by extra(listOf(
    ":kotlin-ultimate:cidr-native",
    ":kotlin-ultimate:clion-native"))


dependencies {
    cidrPlugin(project(":prepare:cidr-plugin"))
    platformDepsZip(tc("$clionVersionRepo:$clionVersion:CL-plugins/kotlinNative-platformDeps-$clionVersion.zip"))
}

val kotlinPluginXml by tasks.creating {
    inputs.files(cidrPlugin)
    outputs.files(fileFrom(buildDir, name, "META-INF/KotlinPlugin.xml"))

    doFirst {
        val pluginXmlText = zipTree(inputs.files.singleFile)
            .matching { include(pluginXmlPath) }
            .singleFile
            .readText()
        outputs.files.singleFile.writeText(pluginXmlText)
    }
}

val preparePluginXml by task<Copy> {
    dependsOn(":kotlin-ultimate:clion-native:assemble")

    val cidrPluginVersion = project.findProperty("cidrPluginVersion") as String? ?: "beta-1"
    val clionPluginVersion = "$kotlinVersion-CLion-$cidrPluginVersion-$clionVersion"

    inputs.property("clionPluginVersion", clionPluginVersion)

    from(project(":kotlin-ultimate:clion-native").mainSourceSet.output.resourcesDir) { include(pluginXmlPath) }
    into(pluginXmlLocation)

    val sinceBuild =
        if (clionVersion.matches(Regex("\\d+\\.\\d+"))) clionVersion else clionVersion.substring(0, clionVersion.lastIndexOf('.'))
    val untilBuild = clionVersion.substring(0, clionVersion.lastIndexOf('.')) + ".*"

    filter {
        it
            .replace("<!--idea_version_placeholder-->",
                     "<idea-version since-build=\"$sinceBuild\" until-build=\"$untilBuild\"/>")
            .replace("<!--version_placeholder-->",
                     "<version>$clionPluginVersion</version>")
    }
}

val jar = runtimeJar {
    archiveName = "kotlin-plugin.jar"
    dependsOn(cidrPlugin)
    dependsOn(preparePluginXml)
    from(kotlinPluginXml) { into("META-INF") }

    from {
        zipTree(cidrPlugin.singleFile).matching {
            exclude(pluginXmlPath)
        }
    }

    for (p in projectsToShadow) {
        dependsOn("$p:classes")
        from(getSourceSetsFrom(p)["main"].output) {
            exclude(pluginXmlPath)
        }
    }
    from(pluginXmlLocation) { include(pluginXmlPath) }
}

fun Zip.includePatched(fileToMarkers: Map<String, List<String>>) {

    val notDone = mutableSetOf<Pair<String, String>>()
    fileToMarkers.forEach { (path, markers) ->
        for (marker in markers) {
            notDone += path to marker
        }
    }

    eachFile {
        val markers = fileToMarkers[this.sourcePath] ?: return@eachFile
        this.filter {
            var data = it
            for (marker in markers) {
                val newData = data.replace(("^(.*" + Pattern.quote(marker) + ".*)$").toRegex(), "<!-- $1 -->")
                data = newData
                notDone -= path to marker
            }
            data
        }
    }
    doLast {
        check(notDone.size == 0) {
            "Filtering failed for: " +
                    notDone.joinToString(separator = "\n") { (file, marker) -> "file=$file, marker=`$marker`" }
        }
    }
}

val platformDepsJar by task<Zip> {
    archiveName = platformDepsJarName
    val platformDepsJar = zipTree(platformDepsZip.singleFile).matching { include("**/$platformDepsJarName") }.singleFile
    from(zipTree(platformDepsJar)) {
        exclude(pluginXmlPath)
        includePatched(
            mapOf(
                javaPsiXmlPath to listOf("implementation=\"org.jetbrains.uast.java.JavaUastLanguagePlugin\""),
                javaPluginXmlPath to listOf("implementation=\"com.intellij.spi.SPIFileTypeFactory\"")
            )
        )
    }
}

task<Copy>("clionPlugin") {
    into(clionPluginDir)
    from(cidrPluginDir) { exclude("lib/kotlin-plugin.jar") }
    from(jar) { into("lib") }
    from(platformDepsJar) { into("lib") }
    from(zipTree(platformDepsZip.singleFile).files) {
        exclude("**/$platformDepsJarName")
        into("lib")
    }
    from(File(project(":kotlin-ultimate:clion-native").projectDir, "templates")) { into("templates") }
}
