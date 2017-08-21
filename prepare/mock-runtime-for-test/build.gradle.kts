
import org.gradle.jvm.tasks.Jar
import org.gradle.api.internal.HasConvention
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(project(":kotlin-stdlib"))
}

val jar: Jar by tasks
jar.apply {
    setupRuntimeJar("Kotlin Mock Runtime for Tests")
    from(fileTree("${rootProject.extra["distDir"]}/builtins")) { include("kotlin/**") }
    archiveName = "kotlin-mock-runtime-for-test.jar"
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
//    options.fork = true
    options.forkOptions.javaHome = file(rootProject.extra["JDK_16"] as String)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.6"
    kotlinOptions.jdkHome = rootProject.extra["JDK_16"] as String
}

task<Copy>("dist") {
    into(rootProject.extra["distDir"].toString())
    from(jar)
}

