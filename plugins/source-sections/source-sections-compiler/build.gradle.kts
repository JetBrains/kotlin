
import org.gradle.jvm.tasks.Jar

apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    val compileOnly by configurations
    val testCompile by configurations
    val testCompileOnly by configurations
    val testRuntime by configurations
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.script"))
    compile(project(":compiler:plugin-api"))
    testCompile(project(":compiler.tests-common"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":compiler:util"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:cli-common"))
    testCompile(project(":compiler:frontend.java"))
    testCompile(project(":compiler:daemon-common"))
    testCompile(project(":compiler:daemon-client"))
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectResources("src") {
    include("META-INF/**")
}
configureKotlinProjectTestsDefault()

val jar: Jar by tasks
jar.apply {
    setupRuntimeJar("Kotlin SourceSections Compiler Plugin")
    archiveName = "kotlin-source-sections-compiler-plugin.jar"
}

dist {
    from(jar)
}

tasks.withType<Test> {
    dependsOnTaskIfExistsRec("dist", project = rootProject)
    workingDir = rootDir
    systemProperty("idea.is.unit.test", "true")
    environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
    ignoreFailures = true
}

