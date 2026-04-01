plugins {
    id("gradle-plugin-common-configuration")
}

dependencies {
    api(platform(project(":kotlin-gradle-plugins-bom")))

    commonCompileOnly(project(":kotlin-gradle-plugin"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    val kgpProjectDir = project(":kotlin-gradle-plugin").projectDir
    val kotlinGradlePluginFriendPaths = libraries.filter {
        val path = it.absolutePath
        it.isDirectory && path.startsWith(kgpProjectDir.absolutePath)
    }
    compilerOptions.freeCompilerArgs.addAll(kotlinGradlePluginFriendPaths.elements.map { files ->
        files.map { "-Xfriend-paths=${it.asFile.absolutePath}" }
    })
}

gradlePlugin {
    plugins {
        create("kotlinWasmWasiRuntimePlugin") {
            id = "org.jetbrains.kotlin.plugin.wasm.wasi.runtime"
            displayName = "Kotlin Wasm WASI Runtime plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.gradle.targets.wasm.runtime.WasmWasiRuntimeGradlePlugin"
        }
    }
}
