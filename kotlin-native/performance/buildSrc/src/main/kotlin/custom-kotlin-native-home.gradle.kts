import org.gradle.api.tasks.PathSensitivity
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.kotlinNativeHome

// We are using bootstrap version of KGP, but we want to use a different compiler version.
// This instructs KGP to look for the Native compiler in a given folder.
extra["kotlin.native.home"] = kotlinNativeHome.toString()

private fun Task.dependsOnKotlinNativeHome() {
    // We can't assume reproducibility of the produced binaries => absolute path sensitivity
    inputs.dir(kotlinNativeHome).withPathSensitivity(PathSensitivity.ABSOLUTE)
}

tasks.withType(KotlinNativeCompile::class).configureEach {
    // Depending on the compiler jar is probably enough, but let's stay on the safe side
    dependsOnKotlinNativeHome()
}

tasks.withType(KotlinNativeLink::class).configureEach {
    // Some parts of the distribution can probably be omitted, but let's stay on the safe side
    dependsOnKotlinNativeHome()
}

tasks.withType(CInteropProcess::class).configureEach {
    // Depending on the compiler jar, konan.properties, libcallbacks.dylib and libclangstubs.dylib is probably enough,
    // but let's stay on the safe side
    dependsOnKotlinNativeHome()
}