/*


apply { plugin("kotlin") }

dependencies {

    compile(ideaUltimateSdkDeps("*.jar"))
    compileOnly(project(":idea"))
    compileOnly(project(":idea:idea-maven"))
    compileOnly(project(":idea:idea-gradle"))
    compileOnly(project(":idea:idea-jvm"))

    runtimeOnly(files(toolsJar()))
}


val runUltimate by task<JavaExec> {
    dependsOn(":dist", ":prepare:idea-plugin:idea-plugin", ":dist-plugin", ":ultimate:idea-ultimate-plugin")

    classpath = the<JavaPluginConvention>().sourceSets["main"].runtimeClasspath

    main = "com.intellij.idea.Main"

    workingDir = File(projectDir.parentFile, "ideaSDK", "bin")

    val ideaUltimatePluginDir: File by rootProject.extra

    jvmArgs(
            "-Xmx1250m",
            "-XX:ReservedCodeCacheSize=240m",
            "-XX:+HeapDumpOnOutOfMemoryError",
            "-ea",
            "-Didea.is.internal=true",
            "-Didea.debug.mode=true",
            "-Didea.system.path=../system-idea",
            "-Didea.config.path=../config-idea",
            "-Dapple.laf.useScreenMenuBar=true",
            "-Dapple.awt.graphics.UseQuartz=true",
            "-Dsun.io.useCanonCaches=false",
            "-Dplugin.path=${ideaUltimatePluginDir.absolutePath}",
            "-Dkotlin.internal.mode.enabled=true",
            "-Didea.additional.classpath=../idea-kotlin-runtime/kotlin-runtime.jar,../idea-kotlin-runtime/kotlin-reflect.jar"
    )

    if (project.hasProperty("noPCE")) {
        jvmArgs("-Didea.ProcessCanceledException=disabled")
    }

    args()
}


*/