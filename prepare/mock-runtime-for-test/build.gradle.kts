
import org.gradle.jvm.tasks.Jar
import org.gradle.api.internal.HasConvention
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

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

configure<JavaPluginConvention> {
   sourceSets["main"].apply {
       (this as HasConvention).convention.getPlugin<KotlinSourceSet>().kotlin.apply {
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
}

configureKotlinProjectNoTests()


task<Copy>("dist") {
    into(rootProject.extra["distDir"].toString())
    from(jar)
}

