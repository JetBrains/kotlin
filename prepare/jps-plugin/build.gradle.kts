
import org.gradle.jvm.tasks.Jar

apply { plugin("java") }

val projectsToShadow = listOf(
        ":build-common",
        ":compiler:cli-common",
        ":compiler:compiler-runner",
        ":compiler:daemon-client",
        ":compiler:daemon-common",
        ":core",
        ":idea:idea-jps-common",
        ":jps-plugin",
        ":kotlin-preloader",
        ":compiler:util",
        ":core:util.runtime",
        ":plugins:android-extensions-jps")

dependencies {}

val jar: Jar by tasks
jar.apply {
    setupRuntimeJar("Kotlin JPS plugin")
    manifest.attributes.put("Main-Class", "org.jetbrains.kotlin.runner.Main")
    manifest.attributes.put("Class-Path", "kotlin-runtime.jar")
    archiveName = "kotlin-jps-plugin.jar"
    projectsToShadow.forEach {
        dependsOn("$it:classes")
        project(it).let { p ->
            p.pluginManager.withPlugin("java") {
                from(p.the<JavaPluginConvention>().sourceSets.getByName("main").output)
            }
        }
    }
    from(fileTree("$rootDir/jps-plugin/src")) { include("META-INF/**") }
    from(files("$rootDir/resources/kotlinManifest.properties"))
    from(zipTree("$rootDir/dependencies/native-platform-uberjar.jar"))
}

configureKotlinProjectSources() // no sources
configureKotlinProjectNoTests()

ideaPlugin("lib/jps") {
    from(jar)
}

