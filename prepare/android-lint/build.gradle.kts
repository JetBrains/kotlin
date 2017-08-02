
import org.gradle.jvm.tasks.Jar

apply { plugin("java") }

val projectsToShadow = listOf(
        ":plugins:lint",
        ":plugins:uast-kotlin",
        ":plugins:uast-kotlin-idea")

tasks.withType<Jar> {
    setupRuntimeJar("Kotlin Android Lint")
    archiveName = "android-lint.jar"
    projectsToShadow.forEach {
        dependsOn("$it:classes")
        project(it).let { p ->
            p.pluginManager.withPlugin("java") {
                from(p.the<JavaPluginConvention>().sourceSets.getByName("main").output)
            }
        }
    }
}

configureKotlinProjectSources() // no sources
configureKotlinProjectNoTests()

val jar: Jar by tasks

ideaPlugin {
    from(jar)
}
