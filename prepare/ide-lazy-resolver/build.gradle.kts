import java.util.Properties

description = "Kotlin IDE Lazy Resolver"

plugins {
    java
}

val versions by configurations.creating
val versionFilePath = "${rootProject.buildDir}/dependencies.properties"
val ideaVersion = findProperty("versions.intellijSdk").toString()
val markdownVersion = findProperty("versions.markdown").toString()

val writeVersions by tasks.registering {
    val versionFile = File(versionFilePath)
    val ideaVersion = ideaVersion
    val markdownVersion = markdownVersion

    inputs.property("ideaVersion", ideaVersion)
    inputs.property("markdownVersion", markdownVersion)

    outputs.file(versionFile)
    doLast {
        versionFile.parentFile.mkdirs()
        val properties = Properties()
        properties.setProperty("idea.build.id", ideaVersion)
        properties.setProperty("markdown.build.id", markdownVersion)
        properties.store(versionFile.outputStream(), "")
    }
}

runtimeJar {
    dependsOn(writeVersions)
    archiveName = "kotlin-ide-common.jar"
    dependsOn(":idea:ide-common:classes")
    project(":idea:ide-common").let { p ->
        p.pluginManager.withPlugin("java") {
            from(p.mainSourceSet.output)
        }
    }
    from(fileTree("$rootDir/idea/ide-common")) { include("src/**") } // Eclipse formatter sources navigation depends on this
}

sourceSets {
    "main" {}
    "test" {}
}

