plugins {
    `java-library`
    `maven-publish`
}

publishing {
    repositories {
        maven("<localRepo>")
    }

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

fun Project.resolveDependencies(name: String) {
    val configuration = configurations.findByName(name) ?: return
    configuration.resolve() // ensure that resolution is green
    val allResolvedComponents = configuration.incoming.resolutionResult.allComponents
    val content = allResolvedComponents
        .map { component -> "${component.id} => ${component.variants.map { it.displayName }}" }
        .sorted()
        .joinToString("\n")
    val dir = file("resolvedDependenciesReports")
    dir.mkdirs()
    dir.resolve("${name}.txt").writeText(content)
}

tasks.register("resolveDependencies") {
    doFirst {
        project.resolveDependencies("compileClasspath")
    }
}