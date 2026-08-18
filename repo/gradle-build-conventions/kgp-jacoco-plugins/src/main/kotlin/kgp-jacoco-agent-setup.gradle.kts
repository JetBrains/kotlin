import org.gradle.kotlin.dsl.withType

val versionCatalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
val jacocoAgentDependency = versionCatalog.findLibrary("jacoco-agent").get()

// Resolves the JaCoCo agent for TestKit subprocess instrumentation.
val jacocoAgentRuntime = configurations.dependencyScope("jacocoAgentRuntime")
val jacocoAgentRuntimeResolver = configurations.resolvable(jacocoAgentRuntime.name + "Resolver") {
    extendsFrom(jacocoAgentRuntime)
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    }
}

dependencies {
    jacocoAgentRuntime(jacocoAgentDependency.get()) { artifact { classifier = "runtime" } }
}

val kgpTestCoverageEnabled: Boolean = providers.gradleProperty("kgp.jacoco.enabled").orNull?.toBoolean() ?: false

tasks.withType<Test>().configureEach {
    systemProperty("kgp.jacoco.enabled", kgpTestCoverageEnabled)
    if (kgpTestCoverageEnabled) {

        // Don't abort the build on test failures — the report still needs the partial `.exec`.
        ignoreFailures = true

        val jacocoRuntimeJar = jacocoAgentRuntimeResolver.map { it.incoming.files.singleFile }
        val jacocoDestFile = layout.buildDirectory.file("jacoco/coverage.exec")

        inputs.files(jacocoAgentRuntimeResolver).withNormalizer(ClasspathNormalizer::class)
        outputs.upToDateWhen { jacocoDestFile.get().asFile.exists() }

        // The test JVM itself also loads offline-instrumented KGP classes,
        // so it needs the JaCoCo offline runtime too.
        jvmArgumentProviders.add(CommandLineArgumentProvider {
            listOf(
                "-Xbootclasspath/a:${jacocoRuntimeJar.get().absolutePath}",
                "-Djacoco-agent.destfile=${jacocoDestFile.get().asFile.absolutePath}",
                "-Djacoco-agent.append=true",
                "-Djacoco-agent.output=file",
            )
        })

        doFirst {
            // pass values to set up the classpath for integration tests to use offline instrumentation
            systemProperty("jacocoRuntimeJar", jacocoRuntimeJar.get().absolutePath)
            systemProperty("jacocoDestFile", jacocoDestFile.get().asFile.absolutePath)
        }
    }
}
