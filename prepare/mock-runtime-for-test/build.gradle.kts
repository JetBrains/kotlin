
description = "Kotlin Mock Runtime for Tests"

apply { plugin("kotlin") }

jvmTarget = "1.6"
javaHome = rootProject.extra["JDK_16"] as String

dependencies {
    compile(projectDist(":kotlin-stdlib"))
}

sourceSets {
    "main" {
        java.apply {
            srcDir(File(rootDir, "core", "runtime.jvm", "src"))
                    .include("kotlin/TypeAliases.kt",
                             "kotlin/text/TypeAliases.kt")
            srcDir(File(rootDir, "libraries", "stdlib", "src"))
                    .include("kotlin/collections/TypeAliases.kt",
                             "kotlin/jvm/JvmVersion.kt",
                             "kotlin/util/Standard.kt",
                             "kotlin/internal/Annotations.kt")
        }
    }
    "test" {}
}


tasks.withType<JavaCompile> {
    sourceCompatibility = "1.6"
    targetCompatibility = "1.6"
}

val jar = runtimeJar {
    from(fileTree("${rootProject.extra["distDir"]}/builtins")) { include("kotlin/**") }
}

val distDir: String by rootProject.extra

dist(targetName = "kotlin-mock-runtime-for-test.jar", targetDir = File(distDir))
