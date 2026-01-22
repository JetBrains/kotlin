import org.gradle.kotlin.dsl.withType


// Resolves the JaCoCo agent for TestKit subprocess instrumentation.
val jacocoAgentRuntime = configurations.dependencyScope("jacocoAgentRuntime")
val jacocoAgentRuntimeResolver = configurations.resolvable(jacocoAgentRuntime.name + "Resolver") {
    extendsFrom(jacocoAgentRuntime)
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    }
}

val jacocoAgentDependency = extensions
    .getByType(VersionCatalogsExtension::class.java)
    .named("libs")
    .findLibrary("jacoco-agent").get()

dependencies {
    jacocoAgentRuntime(jacocoAgentDependency.get()) { artifact { classifier = "runtime" } }
}

val kgpTestCoverageEnabled: Boolean = providers.gradleProperty("kgp.jacoco.enabled").orNull?.toBoolean() ?: false

tasks.withType<Test>().configureEach {
    systemProperty("kgp.jacoco.enabled", kgpTestCoverageEnabled)
    if (kgpTestCoverageEnabled) {
        val jacocoRuntimeJar = jacocoAgentRuntimeResolver.map { it.incoming.files.singleFile }
        val jacocoOutputDir = layout.buildDirectory.dir("jacoco/testkit")

        inputs.files(jacocoAgentRuntimeResolver)
            .withNormalizer(ClasspathNormalizer::class)

        // Don't abort the build on test failures — the report still needs the partial `.exec`.
        ignoreFailures = true

        doFirst {
            jacocoOutputDir.get().asFile.mkdirs()
            systemProperty("jacocoRuntimeJar", jacocoRuntimeJar.get().absolutePath)
            systemProperty("jacocoOutputDir", jacocoOutputDir.get().asFile.absolutePath)
        }
    }
}
