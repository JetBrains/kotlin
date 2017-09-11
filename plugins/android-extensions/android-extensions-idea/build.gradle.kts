
import org.gradle.jvm.tasks.Jar

apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    val compileOnly by configurations
    val testCompile by configurations
    val testCompileOnly by configurations
    val testRuntime by configurations

    compile(project(":compiler:util"))
    compile(project(":compiler:light-classes"))
    compile(project(":idea:idea-core"))
    compile(project(":idea"))
    compile(project(":android-extensions-compiler"))
    compile(ideaPluginDeps("android", "sdk-tools", "sdk-common", plugin = "android"))
    compile(ideaPluginDeps("Groovy", plugin = "Groovy"))
    testCompile(project(":compiler.tests-common"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:frontend.java"))
    testCompile(project(":idea:idea-test-framework")) { isTransitive = false }
    testCompile(projectTests(":idea"))
    testCompile(projectTests(":idea:idea-android"))
    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testRuntime(project(":plugins:android-extensions-jps"))
    testRuntime(project(":plugins:sam-with-receiver-ide"))
    testRuntime(project(":plugins:noarg-ide"))
    testRuntime(project(":plugins:allopen-ide"))
    testRuntime(ideaSdkDeps("*.jar"))
    testRuntime(ideaPluginDeps("idea-junit", "resources_en", plugin = "junit"))
    testRuntime(ideaPluginDeps("IntelliLang", plugin = "IntelliLang"))
    testRuntime(ideaPluginDeps("jcommander", "testng", "testng-plugin", "resources_en", plugin = "testng"))
    testRuntime(ideaPluginDeps("copyright", plugin = "copyright"))
    testRuntime(ideaPluginDeps("properties", "resources_en", plugin = "properties"))
    testRuntime(ideaPluginDeps("java-i18n", plugin = "java-i18n"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "gradle"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "Groovy"))
    testRuntime(ideaPluginDeps("coverage", "jacocoant", plugin = "coverage"))
    testRuntime(ideaPluginDeps("java-decompiler", plugin = "java-decompiler"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "maven"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "android"))
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectTestsDefault()

testsJar {}


tasks.withType<Test> {
    workingDir = rootDir
    systemProperty("idea.is.unit.test", "true")
    environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
    ignoreFailures = true
}

val jar: Jar by tasks

ideaPlugin {
    from(jar)
}
