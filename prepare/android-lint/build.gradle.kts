
description = "Kotlin Android Lint"

apply { plugin("java-base") }

val projectsToShadow = listOf(
        ":plugins:uast-kotlin",
        ":plugins:uast-kotlin-idea")

sourceSets {
    "main" {}
    "test" {}
}

containsEmbeddedComponents()

dependencies {
    projectsToShadow.forEach { p ->
        embeddedComponents(project(p)) { isTransitive = false }
    }
}

runtimeJar {
    /*
        TODO: `fromEmbeddedComponents()` should be used here.
        Couldn't use it because of the "must be locked before it can be used to compute a classpath" error.
     */
    projectsToShadow.forEach {
        dependsOn("$it:classes")
        project(it).let { p ->
            p.pluginManager.withPlugin("java") {
                from(p.the<JavaPluginConvention>().sourceSets.getByName("main").output)
            }
        }
    }
}

ideaPlugin()
