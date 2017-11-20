
description = "Kotlin Android Lint"

apply { plugin("java-base") }

val projectsToShadow = listOf(
        ":plugins:lint",
        ":plugins:uast-kotlin",
        ":plugins:uast-kotlin-idea")

sourceSets {
    "main" {}
    "test" {}
}

runtimeJar {
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
