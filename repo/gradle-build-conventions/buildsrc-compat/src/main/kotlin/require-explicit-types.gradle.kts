import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/**
 * Adds `-XXexplicit-return-types` flag to warn about not specified return types for public declarations
 */
tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-XXexplicit-return-types=warning")
    }
}
