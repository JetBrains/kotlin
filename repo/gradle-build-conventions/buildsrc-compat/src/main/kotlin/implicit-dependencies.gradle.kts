/*
IDEA during the import creates detached configurations that contain a subset of dependencies
 that causes resolution of actually unused artifacts that fail the dependency verification.
*/
configurations.dependencyScope("implicitDependencies")

if (kotlinBuildProperties.isInIdeaSync.get()) {
    afterEvaluate {
        // IDEA manages to download dependencies from `implicitDependencies`, even if it is created with `isCanBeResolved = false`
        // Clear `implicitDependencies` to avoid downloading unnecessary dependencies during import
        configurations.named("implicitDependencies").get().dependencies.clear()
    }
}
