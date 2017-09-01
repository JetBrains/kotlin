
apply { plugin("kotlin") }

//plugins {
//    kotlin("jvm")
//}

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:light-classes"))
    compile(project(":compiler:util"))
//    compile(ideaSdkCoreDeps("intellij-core", "util"))
    testCompile(project(":idea"))
    testCompile(project(":idea:idea-test-framework"))
    testCompile(project(":compiler:light-classes"))
    testCompile(projectDist(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":compiler.tests-common"))
    testRuntime(project(":idea:idea-android"))
    testRuntime(project(":plugins:android-extensions-idea"))
    testRuntime(project(":sam-with-receiver-ide-plugin"))
    testRuntime(project(":allopen-ide-plugin"))
    testRuntime(project(":noarg-ide-plugin"))
    testRuntime(ideaSdkDeps("*.jar"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "properties"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "gradle"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "Groovy"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "coverage"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "maven"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "android"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "junit"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "testng"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "IntelliLang"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "testng"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "copyright"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "properties"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "java-i18n"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "coverage"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "java-decompiler"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    dependsOnTaskIfExistsRec("dist", project = rootProject)
    workingDir = rootDir
}

testsJar()


val testForWebDemo by task<Test> {
    include("**/*JavaToKotlinConverterForWebDemoTestGenerated*")
    classpath = the<JavaPluginConvention>().sourceSets["test"].runtimeClasspath
    workingDir = rootDir
}
val cleanTestForWebDemo by tasks

val test: Test by tasks
test.apply {
    exclude("**/*JavaToKotlinConverterForWebDemoTestGenerated*")
    dependsOn(testForWebDemo)
}
val cleanTest by tasks
cleanTest.dependsOn(cleanTestForWebDemo)

