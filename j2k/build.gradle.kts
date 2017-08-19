
apply { plugin("kotlin") }

//plugins {
//    kotlin("jvm")
//}

dependencies {
    val compile by configurations
    val testCompile by configurations
    val testRuntime by configurations
    compile(project(":kotlin-stdlib"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:light-classes"))
    compile(project(":compiler:util"))
//    compile(ideaSdkCoreDeps("intellij-core", "util"))
    testCompile(project(":idea"))
    testCompile(project(":idea:idea-test-framework"))
    testCompile(project(":compiler:light-classes"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(project(":compiler.tests-common"))
//    testRuntime(project(":idea:idea-android"))
//    testRuntime(project(":plugins:android-extensions-idea"))
//    testRuntime(project(":plugins:sam-with-receiver-ide"))
//    testRuntime(project(":plugins:allopen-ide"))
//    testRuntime(project(":plugins:noarg-ide"))
    testRuntime(ideaSdkDeps("*.jar"))
//    testRuntime(ideaPluginDeps("*.jar", plugin = "junit"))
//    testRuntime(ideaPluginDeps("*.jar", plugin = "testng"))
//    testRuntime(ideaPluginDeps("*.jar", plugin = "properties"))
//    testRuntime(ideaPluginDeps("*.jar", plugin = "gradle"))
//    testRuntime(ideaPluginDeps("*.jar", plugin = "Groovy"))
//    testRuntime(ideaPluginDeps("*.jar", plugin = "coverage"))
//    testRuntime(ideaPluginDeps("*.jar", plugin = "maven"))
//    testRuntime(ideaPluginDeps("*.jar", plugin = "android"))
//    testRuntime(ideaPluginDeps("*.jar", plugin = "junit"))
//    testRuntime(ideaPluginDeps("*.jar", plugin = "IntelliLang"))
//    testRuntime(ideaPluginDeps("*.jar", plugin = "testng"))
//    testRuntime(ideaPluginDeps("*.jar", plugin = "copyright"))
//    testRuntime(ideaPluginDeps("*.jar", plugin = "properties"))
//    testRuntime(ideaPluginDeps("*.jar", plugin = "java-i18n"))
//    testRuntime(ideaPluginDeps("*.jar", plugin = "coverage"))
//    testRuntime(ideaPluginDeps("*.jar", plugin = "java-decompiler"))
    buildVersion()
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectTestsDefault()

tasks.withType<Test> {
    dependsOnTaskIfExistsRec("dist", project = rootProject)
    jvmArgs("-ea", "-XX:+HeapDumpOnOutOfMemoryError", "-Xmx1200m", "-XX:+UseCodeCacheFlushing", "-XX:ReservedCodeCacheSize=128m", "-Djna.nosys=true")
    maxHeapSize = "1200m"
    workingDir = rootDir
    systemProperty("idea.is.unit.test", "true")
    environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
    ignoreFailures = true
}

testsJar()
