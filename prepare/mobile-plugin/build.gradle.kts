import com.github.jk1.tcdeps.KotlinScriptDslAdapter.tc
import com.github.jk1.tcdeps.KotlinScriptDslAdapter.teamcityServer
import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("jvm")
    id("com.github.jk1.tcdeps") version "1.2"
}

val mobilePluginDir: File by rootProject.extra
val mobilePluginZipPath: File by rootProject.extra
val cidrVersion: String by rootProject.extra
val kotlinNativeBackendVersion: String by rootProject.extra

repositories {
    maven("https://maven.google.com")
    maven("https://repo.labs.intellij.net/intellij-proprietary-modules")
    teamcityServer {
        setUrl("https://buildserver.labs.intellij.net")
    }
}

dependencies {
    runtime(project(":kotlin-ultimate:ide:cidr-gradle-tooling")) { isTransitive = false }
    runtime(project(":kotlin-ultimate:ide:common-cidr-native")) { isTransitive = false }
    runtime(project(":kotlin-ultimate:ide:common-cidr-swift-native")) { isTransitive = false }
    embedded(project(":kotlin-ultimate:ide:mobile-native")) { isTransitive = false }
    runtime(project(":kotlin-android-extensions-runtime"))
    runtime(project(":plugins:android-extensions-compiler"))
    runtime("com.jetbrains.intellij.android:android-kotlin-extensions-common:$cidrVersion") { isTransitive = false }
    runtime("com.android.tools.ddms:ddmlib:26.0.0") {
        exclude("com.google.guava", "guava")
    }
    runtime(project(":kotlin-ultimate:libraries:tools:apple-gradle-plugin-api")) { isTransitive = false }
    runtime(tc("Kotlin_KotlinNative_Development_KotlinNativeLinuxBundle:${kotlinNativeBackendVersion}:backend.native.jar"))
    runtime(tc("Kotlin_KotlinNative_Development_KotlinNativeLinuxBundle:${kotlinNativeBackendVersion}:konan.serializer.jar")) // required for backend.native
}

val pluginJarTask: Task by tasks.named<Jar>("jar") {
    dependsOn(":kotlin-ultimate:ide:mobile-native:assemble")

    configurations.findByName("embedded")?.let { embedded ->
        dependsOn(embedded)
        from(provider { embedded.map(::zipTree) })
    }

    archiveBaseName.set(project.the<BasePluginConvention>().archivesBaseName)
    archiveFileName.set("mobile-plugin.jar")
    manifest.attributes.apply {
        put("Implementation-Vendor", "JetBrains")
        put("Implementation-Title", archiveBaseName.get())
    }
}

val copyRuntimeDeps: Task by tasks.creating(Copy::class) {
    from(configurations.runtime)
    into(File(mobilePluginDir, "lib"))
}

val mobilePlugin: Task by tasks.creating(Copy::class) {
    duplicatesStrategy = DuplicatesStrategy.FAIL

    into(mobilePluginDir)

    into("lib") {
        dependsOn(pluginJarTask)
        from(pluginJarTask)
    }

    dependsOn(
        copyRuntimeDeps
    )
}

val zipMobilePluginTask: Task by tasks.creating(Zip::class) {
    destinationDirectory.set(mobilePluginZipPath.parentFile)
    archiveFileName.set(mobilePluginZipPath.name)

    from(mobilePlugin)
    into("Mobile")

    doLast {
        logger.lifecycle("Plugin artifacts packed to $mobilePluginZipPath")
    }
}