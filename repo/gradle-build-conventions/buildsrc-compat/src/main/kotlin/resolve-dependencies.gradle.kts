// Aggregation-only tasks for dependency resolution.
// Per-project resolution tasks (resolveProjectDependencies, resolveProjectJsTools, resolveProjectJavaToolchains)
// are registered by common-configuration.gradle.kts.
// Cross-project dependsOn wiring is done in settings.gradle gradle.projectsEvaluated block.

val resolveDependenciesInAllProjects by tasks.registering {
    description = "Resolves dependencies in all projects (for dependency verification or populating caches)."
    // Per-project resolveProjectDependencies tasks are wired as dependencies via settings.gradle
}

val resolveJsTools by tasks.registering {
    description = "Resolves JavaScript tools (for dependency verification or populating caches)."
    // Per-project resolveProjectJsTools tasks are wired as dependencies via settings.gradle
}

/**
 * When called with `--write-verification-metadata` resolves all build dependencies including implicit dependencies for all platforms and
 * dependencies downloaded by plugins.
 *
 * Useful for populating Gradle dependency cache or updating `verification-metadata.xml` properly.
 *
 * `./gradlew resolveDependencies --write-verification-metadata md5,sha256 -Pkotlin.native.enabled=true`
 */
tasks.register("resolveDependencies") {
    description = "Resolves all dependencies, including implicit dependencies, in all projects for dependency verification."
    group = "build setup"

    dependsOn(
        resolveDependenciesInAllProjects,
        resolveJsTools,
    )
}

tasks.register("resolveJavaToolchainsInAllProjects") {
    // Currently unused.
    // It is supposed to run during agent image build to populate caches, along with resolveDependencies.
    description = "Resolves Java Toolchains in all Java projects."
    // Per-project resolveProjectJavaToolchains tasks are wired as dependencies via settings.gradle
}
