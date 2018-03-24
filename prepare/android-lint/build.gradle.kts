
description = "Kotlin Android Lint"

plugins {
    `java-base`
}

val projectsToShadow = listOf(
        ":plugins:lint",
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
