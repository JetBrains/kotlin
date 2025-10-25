plugins {
    id("native-interop-plugin")
    id("project-tests-convention")
}

dependencies {
    implementation(kotlinStdlib()) // `kotlinStdlib()` is not available in kotlin-native/build-tools project
    testRuntimeOnly(libs.junit.jupiter.engine)
}

nativeInteropPlugin {
    defFileName.set("env.konan.backend.kotlin.jetbrains.org.def")
    usePrebuiltSources.set(false)
    commonCompilerArgs.set(listOf("-Wall", "-O2"))
    cCompilerArgs.set(listOf("-std=c99"))
    cppCompilerArgs.set(listOf("-std=c++11"))
    selfHeaders.set(listOf("src/main/headers"))
    systemIncludeDirs.set(emptyList<String>())
    linkerArgs.set(emptyList<String>())
    additionalLinkedStaticLibraries.set(emptyList<String>())
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5)
}
