import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

// Without `kotlin.build.coverage.enabled` this plugin is a no-op,
// keeping regular builds identical to builds without it: even a disabled JacocoTaskExtension
// would add a task action and change build cache keys, hence the conditional `apply`.
// The coverage engine (JaCoCo) is an implementation detail of this plugin and the
// :compiler:test-coverage aggregator — an engine change stays within these two places.

if (kotlinBuildProperties.booleanProperty("kotlin.build.coverage.enabled", false).get()) {
    apply(plugin = "jacoco")

    extensions.configure<JacocoPluginExtension> {
        toolVersion = extensions.getByType(VersionCatalogsExtension::class.java)
            .named("libs")
            .findVersion("jacoco").get().requiredVersion
    }

    tasks.withType<Test>().configureEach {
        extensions.configure<JacocoTaskExtension> {
            // Instrument only compiler classes. Without this the agent also instruments
            // JDK-internal and test-generated classes, whose reflection-sensitive consumers
            // break on JaCoCo's synthetic members: debugger stepping/local-variable tests
            // fail because com.sun.tools.jdi reflects over JDWP constant classes, and some
            // codegen tests reflect over the bytecode they generate and load.
            includes = listOf("org.jetbrains.kotlin.*")
        }

        // The coverage report is the point of such a run: don't abort on failing tests,
        // so the report is still produced from the collected execution data.
        ignoreFailures = true
    }
}
