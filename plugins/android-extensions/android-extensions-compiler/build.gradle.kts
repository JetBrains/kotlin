
import org.gradle.jvm.tasks.Jar

apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(ideaSdkCoreDeps("intellij-core"))
    compile(project(":compiler:util"))
    compile(project(":compiler:plugin-api"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:backend"))
    compile(ideaPluginDeps("layoutlib", plugin = "android"))
}

configureKotlinProjectSources("android-extensions-compiler/src", "android-extensions-runtime/src", sourcesBaseDir = File(rootDir, "plugins", "android-extensions"))
configureKotlinProjectNoTests()

val jar: Jar by tasks
jar.apply {
    setupRuntimeJar("Kotlin Android Extensions Compiler")
}

dist {
    from(jar)
}

ideaPlugin {
    from(jar)
}

