plugins {
    id("native-interop-plugin")
}

dependencies {
    implementation(kotlinStdlib()) // `kotlinStdlib()` is not available in kotlin-native/build-tools project
}

nativeInteropPlugin {
    defFileName.set("env.konan.backend.kotlin.jetbrains.org.def")
    usePrebuiltSources.set(true)
    commonCompilerArgs.set(listOf("-Wall", "-O2"))
    cCompilerArgs.set(listOf("-std=c99"))
    cppCompilerArgs.set(listOf("-std=c++11"))
    selfHeaders.set(listOf("src/main/headers"))
    systemIncludeDirs.set(emptyList<String>())
    linkerArgs.set(emptyList<String>())
    additionalLinkedStaticLibraries.set(emptyList<String>())
}

projectTest(jUnitMode = JUnitMode.JUnit5) // `projectTest()` is not available in kotlin-native/build-tools project