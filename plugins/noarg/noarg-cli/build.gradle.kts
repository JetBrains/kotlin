
import org.gradle.jvm.tasks.Jar

apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:util"))
    compile(project(":compiler:plugin-api"))
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectNoTests()

val jar: Jar by tasks
jar.apply {
    setupRuntimeJar("Kotlin NoArg Compiler Plugin")
    from(fileTree("$projectDir/src")) { include("META-INF/**") }
    archiveName = "noarg-compiler-plugin.jar"
}

dist {
    from(jar)
}

ideaPlugin {
    from(jar)
}

