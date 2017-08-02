
import org.gradle.jvm.tasks.Jar

apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(ideaSdkCoreDeps("intellij-core"))
    compile(project(":compiler:plugin-api"))
    compile(project(":compiler:frontend"))
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectNoTests()

val jar: Jar by tasks
jar.apply {
    setupRuntimeJar("Kotlin AllOpen Compiler Plugin")
    from(fileTree("$projectDir/src")) { include("META-INF/**") }
    archiveName = "allopen-compiler-plugin.jar"
}

dist {
    from(jar)
}

ideaPlugin {
    from(jar)
}

