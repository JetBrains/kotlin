import org.gradle.kotlin.dsl.base
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.provideDelegate
import org.jetbrains.dokka.gradle.engine.parameters.KotlinPlatform

plugins {
    base
    `dokka-convention`
}

val kotlin_root = rootProject.file("../../../").absoluteFile.invariantSeparatorsPath
val kotlin_libs: String by project

val outputDir = file(findProperty("docsBuildDir") as String? ?: "${layout.buildDirectory}/doc")
val inputDirPrevious = file(findProperty("docsPreviousVersionsDir") as String? ?: "$outputDir/previous")
val outputDirPartial = outputDir.resolve("partial")
val kotlin_native_root = file("$kotlin_root/kotlin-native").absolutePath
val kotlinTemplatesDir = (findProperty("templatesDir") as String?)?.let { file(it) } ?: rootProject.file("templates")

val isLatest = (findProperty("isLatest") as String?)?.toBoolean() ?: true

dokka {
    val kotlinTestIncludeMd = file("$kotlin_root/libraries/kotlin.test/Module.md")

    val kotlinTestCommonClasspath = fileTree("$kotlin_libs/kotlin-stdlib-common")
    val kotlinTestJunitClasspath = fileTree("$kotlin_libs/kotlin-test-junit")
    val kotlinTestJunit5Classpath = fileTree("$kotlin_libs/kotlin-test-junit5")
    val kotlinTestTestngClasspath = fileTree("$kotlin_libs/kotlin-test-testng")
    val kotlinTestJsClasspath = fileTree("$kotlin_libs/kotlin-test-js")
    val kotlinTestJvmClasspath = fileTree("$kotlin_libs/kotlin-test")

    val kotlinLanguageVersion = version as String

    pluginsConfiguration {
        versioning {
            version.set(kotlinLanguageVersion)
            if (isLatest) {
                olderVersionsDir.set(projectDir.resolve("dokka-docs"))
            }
        }

        register<VersionFilterPluginParameters>("VersionFilterPlugin") {
            targetVersion = kotlinLanguageVersion
        }
    }

    dokkaPublications.html {
        val moduleDirName = "kotlin-test"
        if (isLatest) {
            outputDirectory.set(file("$outputDirPartial/latest").resolve(moduleDirName))
        } else {
            outputDirectory.set(
                file("$outputDirPartial/previous").resolve(moduleDirName).resolve(kotlinLanguageVersion)
            )
        }
    }

    dokkaSourceSets {
        val common = register("common") {
            jdkVersion.set(8)
            analysisPlatform.set(KotlinPlatform.Common)
            classpath.setFrom(kotlinTestCommonClasspath)
            enableJdkDocumentationLink.set(false)

            displayName.set("Common")
            sourceRoots.from("$kotlin_root/libraries/kotlin.test/common/src/main/kotlin")
            sourceRoots.from("$kotlin_root/libraries/kotlin.test/annotations-common/src/main/kotlin")
        }

        val jvm = register("jvm") {
            jdkVersion.set(8)
            analysisPlatform.set(KotlinPlatform.JVM)
            classpath.setFrom(kotlinTestJvmClasspath)

            displayName.set("JVM")
            dependentSourceSets.add(common.get().sourceSetId.get())
            sourceRoots.from("$kotlin_root/libraries/kotlin.test/jvm/src/main/kotlin")
        }

        register("jvm-JUnit") {
            jdkVersion.set(8)
            analysisPlatform.set(KotlinPlatform.JVM)
            classpath.setFrom(kotlinTestJunitClasspath)

            displayName.set("JUnit")
            dependentSourceSets.add(common.get().sourceSetId.get())
            dependentSourceSets.add(jvm.get().sourceSetId.get())
            sourceRoots.from("$kotlin_root/libraries/kotlin.test/junit/src/main/kotlin")

            externalDocumentationLinks.register("junit4") {
                url("http://junit.org/junit4/javadoc/latest/")
                packageListUrl("http://junit.org/junit4/javadoc/latest/package-list")
            }
        }

        register("jvm-JUnit5") {
            jdkVersion.set(8)
            analysisPlatform.set(KotlinPlatform.JVM)
            classpath.setFrom(kotlinTestJunit5Classpath)

            displayName.set("JUnit5")
            dependentSourceSets.add(common.get().sourceSetId.get())
            dependentSourceSets.add(jvm.get().sourceSetId.get())
            sourceRoots.from("$kotlin_root/libraries/kotlin.test/junit5/src/main/kotlin")

            externalDocumentationLinks.register("junit5") {
                url("https://junit.org/junit5/docs/current/api/")
                packageListUrl("https://junit.org/junit5/docs/current/api/element-list")
            }
        }

        register("jvm-TestNG") {
            jdkVersion.set(8)
            analysisPlatform.set(KotlinPlatform.JVM)
            classpath.setFrom(kotlinTestTestngClasspath)

            displayName.set("TestNG")
            dependentSourceSets.add(common.get().sourceSetId.get())
            dependentSourceSets.add(jvm.get().sourceSetId.get())
            sourceRoots.from("$kotlin_root/libraries/kotlin.test/testng/src/main/kotlin")

            // externalDocumentationLinks.register("jitpack") {
            //     url.set(new URL("https://jitpack.io/com/github/cbeust/testng/master/javadoc/"))
            //     packageListUrl.set(new URL("https://jitpack.io/com/github/cbeust/testng/master/javadoc/package-list"))
            // }
        }
        register("js") {
            analysisPlatform.set(KotlinPlatform.JS)
            classpath.setFrom(kotlinTestJsClasspath)
            enableJdkDocumentationLink.set(false)

            displayName.set("JS")
            dependentSourceSets.add(common.get().sourceSetId.get())
            sourceRoots.from("$kotlin_root/libraries/kotlin.test/js/src/main/kotlin")
        }
        register("native") {
            analysisPlatform.set(KotlinPlatform.Native)
            enableJdkDocumentationLink.set(false)

            displayName.set("Native")
            dependentSourceSets.add(common.get().sourceSetId.get())
            sourceRoots.from("$kotlin_native_root/runtime/src/main/kotlin/kotlin/test")
        }
        register("wasm-js") {
            analysisPlatform.set(KotlinPlatform.Wasm)
            enableJdkDocumentationLink.set(false)

            displayName.set("Wasm-JS")
            dependentSourceSets.add(common.get().sourceSetId.get())
            sourceRoots.from("$kotlin_root/libraries/kotlin.test/wasm/src/main")
            sourceRoots.from("$kotlin_root/libraries/kotlin.test/wasm/js/src/main")
        }
        register("wasm-wasi") {
            analysisPlatform.set(KotlinPlatform.Wasm)
            enableJdkDocumentationLink.set(false)

            displayName.set("Wasm-WASI")
            dependentSourceSets.add(common.get().sourceSetId.get())
            sourceRoots.from("$kotlin_root/libraries/kotlin.test/wasm/src/main")
            sourceRoots.from("$kotlin_root/libraries/kotlin.test/wasm/wasi/src/main")
        }
        configureEach {
            skipDeprecated.set(false)
            includes.from(kotlinTestIncludeMd)
            languageVersion.set(kotlinLanguageVersion)
            enableKotlinStdLibDocumentationLink.set(false)
            sourceLinksFromRoot(this)
        }
    }
    fixIntersectedSourceRootsAndSamples(dokkaSourceSets, "kotlin.test")
}

