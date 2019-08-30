import org.gradle.internal.os.OperatingSystem

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:util"))
    compile(project(":js:js.ast"))
    compile(project(":js:js.translator"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    val currentOs = OperatingSystem.current()

    when {
        currentOs.isWindows -> {
            val suffix = if (currentOs.toString().endsWith("64")) "_64" else ""
            compile("com.eclipsesource.j2v8:j2v8_win32_x86$suffix:4.6.0")
        }
        currentOs.isMacOsX -> compile("com.eclipsesource.j2v8:j2v8_macosx_x86_64:4.6.0")
        currentOs.run { isLinux || isUnix } -> compile("com.eclipsesource.j2v8:j2v8_linux_x86_64:4.8.0")
        else -> logger.error("unsupported platform $currentOs - can not compile com.eclipsesource.j2v8 dependency")
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
