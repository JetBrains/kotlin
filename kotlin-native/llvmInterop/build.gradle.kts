import org.jetbrains.kotlin.PlatformInfo
import org.jetbrains.kotlin.konan.target.TargetWithSanitizer
import org.jetbrains.kotlin.tools.lib

plugins {
    kotlin("jvm")
    id("native-interop-plugin")
}

kotlinNativeInterop {
    create("llvm") {
        dependsOn(":kotlin-native:llvmDebugInfoC:${lib("debugInfo")}")
        dependsOn(":kotlin-native:libllvmext:${lib("llvmext")}")
        defFile("llvm.def")
        compilerOpts("-I$llvmDir/include", "-I${rootProject.project(":kotlin-native:llvmDebugInfoC").projectDir}/src/main/include", "-I${rootProject.project(":kotlin-native:libllvmext").projectDir}/src/main/include")
        if (PlatformInfo.isMac()) {
            // $llvmDir/lib contains libc++.1.dylib too, and it seems to be preferred by the linker
            // over the sysroot-provided one.
            // As a result, libllvmstubs.dylib gets linked with $llvmDir/lib/libc++.1.dylib.
            // It has install_name = @rpath/libc++.1.dylib, which won't work for us, because
            // dynamic loader won't be able to find libc++ when loading libllvmstubs.
            // For some reason, this worked fine before macOS 12.3.
            //
            // To enforce linking with proper libc++, pass the default path explicitly:
            linkerOpts("-L${hostPlatform.absoluteTargetSysRoot}/usr/lib")
            // FIXME: Check if this actually needed
            linkerOpts("-Xlinker", "-lto_library", "-Xlinker", "KT-69382")
        }
        linkerOpts("-L$llvmDir/lib", "-L${rootProject.project(":kotlin-native:llvmDebugInfoC").layout.buildDirectory.get().asFile}", "-L${rootProject.project(":kotlin-native:libllvmext").layout.buildDirectory.get().asFile}")
    }
}

val nativeLibs by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE)
        attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, TargetWithSanitizer.host)
    }
}

artifacts {
    add(nativeLibs.name, layout.buildDirectory.dir("nativelibs/${TargetWithSanitizer.host}")) {
        builtBy(kotlinNativeInterop["llvm"].genTask)
    }
}