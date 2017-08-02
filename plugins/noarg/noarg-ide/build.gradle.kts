
import org.gradle.jvm.tasks.Jar

apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(project(":plugins:noarg-cli"))
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:cli-common"))
    compile(project(":idea"))
    compile(project(":idea:idea-jps-common"))
    compile(project(":plugins:annotation-based-compiler-plugins-ide-support"))
    compile(ideaSdkDeps("openapi", "idea"))
    compile(ideaPluginDeps("maven", plugin = "maven"))
    compile(ideaPluginDeps("gradle-tooling-api", "gradle", plugin = "gradle"))
}


configureKotlinProjectSourcesDefault()
configureKotlinProjectNoTests()


val jar: Jar by tasks

ideaPlugin {
    from(jar)
}

