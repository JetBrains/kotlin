import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id "base"
    id "org.jetbrains.dokka"
}

evaluationDependsOnChildren()

def pKotlinBig() { return project('kotlin_big').extensions }

ext.outputDir = "$buildDir/doc"
ext.outputDirLatest = "$outputDir/latest"
ext.outputDirPrevious = "$outputDir/previous"
ext.kotlin_root = pKotlinBig().kotlin_root
ext.kotlin_libs = pKotlinBig().kotlin_libs
ext.kotlin_native_root = file("$kotlin_root/kotlin-native").absolutePath
ext.github_revision = pKotlinBig().github_revision
ext.localRoot = kotlin_root
ext.baseUrl = new URL("https://github.com/JetBrains/kotlin/tree/$github_revision")
ext.templatesDir = "$projectDir/templates".replace('\\', '/')

task cleanDocs(type: Delete) {
    delete(outputDir)
}

task prepare() {
    dependsOn(':kotlin_big:extractLibs')
}

repositories {
    mavenCentral()
    maven {
        url 'https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev'
    }
}
final String dokka_version = findProperty("dokka_version")

dependencies {
    dokkaPlugin project(":plugins:dokka-samples-transformer-plugin")
    dokkaPlugin project(":plugins:dokka-stdlib-configuration-plugin")
    dokkaPlugin project(":plugins:dokka-version-filter-plugin")
    dokkaPlugin "org.jetbrains.dokka:versioning-plugin:$dokka_version"
}

TaskProvider<DokkaTask> createStdLibVersionedDocTask(String version, Boolean isLatest) {
    return tasks.register("kotlin-stdlib_" + version + (isLatest ? "_latest" : ""), DokkaTask) {
        dependsOn(prepare)

        def kotlin_stdlib_dir = "$kotlin_root/libraries/stdlib"

        def stdlibIncludeMd = "$kotlin_root/libraries/stdlib/src/Module.md"
        def stdlibSamples = "$kotlin_root/libraries/stdlib/samples/test"

        def suppressedPackages = [
                "kotlin.internal",
                "kotlin.jvm.internal",
                "kotlin.js.internal",
                "kotlin.native.internal",
                "kotlin.jvm.functions",
                "kotlin.coroutines.jvm.internal",
                "kotlin.reflect.jvm.internal"
        ]

        def kotlinLanguageVersion = version
        if (version == "1.0")
            kotlinLanguageVersion =  "1.1"


        moduleName.set("kotlin-stdlib")
        def moduleDirName = "kotlin-stdlib"
        if (isLatest) {
            outputDirectory.set(new File(outputDirLatest, moduleDirName))
            pluginsMapConfiguration.set(["org.jetbrains.dokka.base.DokkaBase"                       : """{ "mergeImplicitExpectActualDeclarations": "true", "templatesDir": "$templatesDir" }""",
                                         "org.jetbrains.dokka.kotlinlang.StdLibConfigurationPlugin": """{ "ignoreCommonBuiltIns": "true" }""",
                                         "org.jetbrains.dokka.versioning.VersioningPlugin"         : """{ "version": "$version", "olderVersionsDir": "${outputDirPrevious.toString().replace('\\', '/')}/$moduleDirName" }"""])
        } else {
            outputDirectory.set(new File(new File(outputDirPrevious, moduleDirName), version))
            pluginsMapConfiguration.set(["org.jetbrains.dokka.base.DokkaBase"                      : """{ "mergeImplicitExpectActualDeclarations": "true", "templatesDir": "$templatesDir" }""",
                                         "org.jetbrains.dokka.kotlinlang.StdLibConfigurationPlugin": """{ "ignoreCommonBuiltIns": "true" }""",
                                         "org.jetbrains.dokka.kotlinlang.VersionFilterPlugin"      : """{ "targetVersion": "$version" }""",
                                         "org.jetbrains.dokka.versioning.VersioningPlugin"         : """{ "version": "$version" }"""])
        }
        dokkaSourceSets {
            if (version != "1.0" && version != "1.1") { // Common platform since Kotlin 1.2
                register("common") {
                    jdkVersion.set(8)
                    platform.set(Platform.common)
                    noJdkLink.set(true)

                    displayName.set("Common")
                    sourceRoots.from("$kotlin_root/core/builtins/native")
                    sourceRoots.from("$kotlin_root/core/builtins/src/")

                    sourceRoots.from("$kotlin_stdlib_dir/common/src")
                    sourceRoots.from("$kotlin_stdlib_dir/src")
                    sourceRoots.from("$kotlin_stdlib_dir/unsigned/src")
                }
            }

            register("jvm") {
                jdkVersion.set(8)
                platform.set(Platform.jvm)

                displayName.set("JVM")
                if (version != "1.0" && version != "1.1") {
                    dependsOn("common")
                }

                sourceRoots.from("$kotlin_stdlib_dir/jvm/src")

                sourceRoots.from("$kotlin_root/core/reflection.jvm/src")
                sourceRoots.from("$kotlin_stdlib_dir/jvm/runtime/kotlin/jvm/annotations")
                sourceRoots.from("$kotlin_stdlib_dir/jvm/runtime/kotlin/jvm/JvmClassMapping.kt")
                sourceRoots.from("$kotlin_stdlib_dir/jvm/runtime/kotlin/jvm/PurelyImplements.kt")
                sourceRoots.from("$kotlin_stdlib_dir/jvm/runtime/kotlin/Metadata.kt")
                sourceRoots.from("$kotlin_stdlib_dir/jvm/runtime/kotlin/Throws.kt")
                sourceRoots.from("$kotlin_stdlib_dir/jvm/runtime/kotlin/TypeAliases.kt")
                sourceRoots.from("$kotlin_stdlib_dir/jvm/runtime/kotlin/text/TypeAliases.kt")

                // for Kotlin 1.0 and 1.1 hack: Common platform becomes JVM
                if (version == "1.0" || version == "1.1") {
                    sourceRoots.from("$kotlin_root/core/builtins/native")
                    sourceRoots.from("$kotlin_root/core/builtins/src/")

                    sourceRoots.from("$kotlin_stdlib_dir/common/src")
                    sourceRoots.from("$kotlin_stdlib_dir/src")
                    sourceRoots.from("$kotlin_stdlib_dir/unsigned/src")
                }
            }
            if (version != "1.0" && version != "1.1") {
            register("jvm-jdk8") {
                jdkVersion.set(8)
                platform.set(Platform.jvm)

                displayName.set("JVM8")
                dependsOn("jvm")
                dependsOn("common")
                sourceRoots.from("$kotlin_stdlib_dir/jdk8/src")
            }
            register("jvm-jdk7") {
                jdkVersion.set(8)
                platform.set(Platform.jvm)

                displayName.set("JVM7")
                dependsOn("jvm")
                dependsOn("common")
                sourceRoots.from("$kotlin_stdlib_dir/jdk7/src")
            }
            }
            if (version != "1.0") { // JS platform since Kotlin 1.1
                register("js") {
                    jdkVersion.set(8)
                    platform.set(Platform.js)
                    noJdkLink.set(true)

                    displayName.set("JS")
                    if (version != "1.0" && version != "1.1") {
                        dependsOn("common")
                    }

                    sourceRoots.from("$kotlin_stdlib_dir/js/src")
                    sourceRoots.from("$kotlin_stdlib_dir/js-v1/src")

                    // for Kotlin 1.1 hack: Common platform becomes JVM
                    if (version == "1.1") {
                        sourceRoots.from("$kotlin_root/core/builtins/native")
                        sourceRoots.from("$kotlin_root/core/builtins/src/")

                        //sourceRoots.from("$kotlin_stdlib_dir/common/src") // is included  in /js-v1/src folder
                        sourceRoots.from("$kotlin_stdlib_dir/src")
                        sourceRoots.from("$kotlin_stdlib_dir/unsigned/src")
                    }
                    perPackageOption {
                        matchingRegex.set("org.w3c(\$|\\.).*")
                        reportUndocumented.set(false)
                    }
                    perPackageOption {
                        matchingRegex.set("org.khronos(\$|\\.).*")
                        reportUndocumented.set(false)
                    }
                }
            }
            if (version != "1.0" && version != "1.1" && version != "1.2") { // Native platform since Kotlin 1.3
                register("native") {
                    jdkVersion.set(8)
                    platform.set(Platform.native)
                    noJdkLink.set(true)

                    displayName.set("Native")
                    dependsOn("common")

                    sourceRoots.from("$kotlin_native_root/Interop/Runtime/src/main/kotlin")
                    sourceRoots.from("$kotlin_native_root/Interop/Runtime/src/native/kotlin")
                    sourceRoots.from("$kotlin_native_root/Interop/JsRuntime/src/main/kotlin")
                    sourceRoots.from("$kotlin_native_root/runtime/src/main/kotlin")
                    sourceRoots.from("$kotlin_stdlib_dir/native-wasm/src")
                    perPackageOption {
                        matchingRegex.set("kotlin.test(\$|\\.).*")
                        suppress.set(true)
                    }
                }
            }
            configureEach {
                documentedVisibilities.set([DokkaConfiguration.Visibility.PUBLIC, DokkaConfiguration.Visibility.PROTECTED])
                skipDeprecated.set(false)
                includes.from(stdlibIncludeMd.toString())
                noStdlibLink.set(true)
                languageVersion.set(kotlinLanguageVersion)
                samples.from(stdlibSamples.toString())
                suppressedPackages.forEach { packageName ->
                    perPackageOption {
                        matchingRegex.set("${packageName.replace(".", "\\.")}(\$|\\..*)")
                        suppress.set(true)
                    }
                }
                sourceLink {
                    localDirectory.set(file(localRoot))
                    remoteUrl.set(baseUrl)
                    remoteLineSuffix.set("#L")
                }
            }
        }
    }
}

TaskProvider<DokkaTask> createKotlinTestVersionedDocTask(String version, Boolean isLatest, TaskProvider<DokkaTask> stdlibDocTask) {
    return tasks.register("kotlin-test_" + version + (isLatest ? "_latest" : ""), DokkaTask) {
        dependsOn(prepare, stdlibDocTask)

        def kotlinTestIncludeMd = "$kotlin_root/libraries/kotlin.test/Module.md"

        def kotlinTestCommonClasspath = fileTree("$kotlin_libs/kotlin-test-common")
        def kotlinTestJunitClasspath = fileTree("$kotlin_libs/kotlin-test-junit")
        def kotlinTestJunit5Classpath = fileTree("$kotlin_libs/kotlin-test-junit5")
        def kotlinTestTestngClasspath = fileTree("$kotlin_libs/kotlin-test-testng")
        def kotlinTestJsClasspath = fileTree("$kotlin_libs/kotlin-test-js")
        def kotlinTestJvmClasspath = fileTree("$kotlin_libs/kotlin-test")

        def stdlibPackageList = new URL("file:///${stdlibDocTask.get().outputDirectory.get()}/kotlin-stdlib/package-list".toString())
        def junit5PackageList = new URL("https://junit.org/junit5/docs/current/api/element-list".toString())
        def kotlinLanguageVersion = version

        moduleName.set("kotlin-test")

        def moduleDirName = "kotlin-test"
        if (isLatest) {
            outputDirectory.set(new File(outputDirLatest, moduleDirName))
            pluginsMapConfiguration.set(["org.jetbrains.dokka.base.DokkaBase"             : """{ "mergeImplicitExpectActualDeclarations": "true", "templatesDir": "$templatesDir" }""",
                                         "org.jetbrains.dokka.versioning.VersioningPlugin": """{ "version": "$version", "olderVersionsDir": "${outputDirPrevious.toString().replace('\\', '/')}/$moduleDirName" }"""])
        } else {
            outputDirectory.set(new File(new File(outputDirPrevious, moduleDirName), version))
            pluginsMapConfiguration.set(["org.jetbrains.dokka.base.DokkaBase"                : """{ "mergeImplicitExpectActualDeclarations": "true", "templatesDir": "$templatesDir" }""",
                                         "org.jetbrains.dokka.kotlinlang.VersionFilterPlugin": """{ "targetVersion": "$version" }""",
                                         "org.jetbrains.dokka.versioning.VersioningPlugin"   : """{ "version": "$version" }"""])
        }

        dokkaSourceSets {
            if (version != "1.0" && version != "1.1") { // Common platform since Kotlin 1.2
                register("common") {
                    jdkVersion.set(8)
                    platform.set(Platform.common)
                    classpath.setFrom(kotlinTestCommonClasspath)
                    noJdkLink.set(true)

                    displayName.set("Common")
                    sourceRoots.from("$kotlin_root/libraries/kotlin.test/common/src/main/kotlin")
                    sourceRoots.from("$kotlin_root/libraries/kotlin.test/annotations-common/src/main/kotlin")
                }
            }

            register("jvm") {
                jdkVersion.set(8)
                platform.set(Platform.jvm)
                classpath.setFrom(kotlinTestJvmClasspath)

                displayName.set("JVM")
                if (version != "1.0" && version != "1.1")
                    dependsOn("common")
                sourceRoots.from("$kotlin_root/libraries/kotlin.test/jvm/src/main/kotlin")
                if (version == "1.0" || version == "1.1") {
                    sourceRoots.from("$kotlin_root/libraries/kotlin.test/common/src/main/kotlin")
                    sourceRoots.from("$kotlin_root/libraries/kotlin.test/annotations-common/src/main/kotlin")
                }
            }

            register("jvm-JUnit") {
                jdkVersion.set(8)
                platform.set(Platform.jvm)
                classpath.setFrom(kotlinTestJunitClasspath)

                displayName.set("JUnit")
                if (version != "1.0" && version != "1.1")
                    dependsOn("common")
                dependsOn("jvm")
                sourceRoots.from("$kotlin_root/libraries/kotlin.test/junit/src/main/kotlin")

                externalDocumentationLink {
                    url.set(new URL("http://junit.org/junit4/javadoc/latest/"))
                    packageListUrl.set(new URL("http://junit.org/junit4/javadoc/latest/package-list"))
                }
            }

            if (version != "1.0" && version != "1.1")
            register("jvm-JUnit5") {
                jdkVersion.set(8)
                platform.set(Platform.jvm)
                classpath.setFrom(kotlinTestJunit5Classpath)

                displayName.set("JUnit5")
                dependsOn("common")
                dependsOn("jvm")
                sourceRoots.from("$kotlin_root/libraries/kotlin.test/junit5/src/main/kotlin")

                externalDocumentationLink {
                    url.set(new URL("https://junit.org/junit5/docs/current/api/"))
                    packageListUrl.set(junit5PackageList)
                }
            }

            if (version != "1.0" && version != "1.1")
            register("jvm-TestNG") {
                jdkVersion.set(8)
                platform.set(Platform.jvm)
                classpath.setFrom(kotlinTestTestngClasspath)

                displayName.set("TestNG")
                dependsOn("common")
                dependsOn("jvm")
                sourceRoots.from("$kotlin_root/libraries/kotlin.test/testng/src/main/kotlin")

                // externalDocumentationLink {
                //     url.set(new URL("https://jitpack.io/com/github/cbeust/testng/master/javadoc/"))
                //     packageListUrl.set(new URL("https://jitpack.io/com/github/cbeust/testng/master/javadoc/package-list"))
                // }
            }
            if (version != "1.0") { // JS platform since Kotlin 1.1
                register("js") {
                    platform.set(Platform.js)
                    classpath.setFrom(kotlinTestJsClasspath)
                    noJdkLink.set(true)

                    displayName.set("JS")
                    if (version != "1.1")
                        dependsOn("common")
                    sourceRoots.from("$kotlin_root/libraries/kotlin.test/js/src/main/kotlin")
                    if (version == "1.0" || version == "1.1") {
                        sourceRoots.from("$kotlin_root/libraries/kotlin.test/common/src/main/kotlin")
                        sourceRoots.from("$kotlin_root/libraries/kotlin.test/annotations-common/src/main/kotlin")
                    }
                }
            }
            if (version != "1.0" && version != "1.1" && version != "1.2") { // Native platform since Kotlin 1.3
                register("native") {
                    platform.set(Platform.native)
                    noJdkLink.set(true)

                    displayName.set("Native")
                    dependsOn("common")
                    sourceRoots.from("$kotlin_native_root/runtime/src/main/kotlin/kotlin/test")
                }
            }
            configureEach {
                skipDeprecated.set(false)
                includes.from(kotlinTestIncludeMd.toString())
                languageVersion.set(kotlinLanguageVersion)
                noStdlibLink.set(true)
                sourceLink {
                    localDirectory.set(file(localRoot))
                    remoteUrl.set(baseUrl)
                    remoteLineSuffix.set("#L")
                }
                externalDocumentationLink {
                    url.set(new URL("https://kotlinlang.org/api/latest/jvm/stdlib/"))
                    packageListUrl.set(stdlibPackageList)
                }
            }
        }
    }
}

gradle.projectsEvaluated {
    def versions = ["1.0", "1.1", "1.2", "1.3", "1.4", "1.5", "1.6", "1.7", "1.8"]
    String latestVersion = versions.last()

    // builds this version/all versions as historical for the next versions builds
    tasks.register('buildAllVersions')
    // builds the latest version incorporating all previous historical versions docs
    tasks.register('buildLatestVersion')

    def latestStdlib = createStdLibVersionedDocTask(latestVersion, true)
    def latestTest = createKotlinTestVersionedDocTask(latestVersion, true, latestStdlib)

    buildLatestVersion.configure { dependsOn(latestStdlib, latestTest) }

    versions.forEach { version ->
        def versionStdlib = createStdLibVersionedDocTask(version, false)
        def versionTest = createKotlinTestVersionedDocTask(version, false, versionStdlib)
        if (version != latestVersion) {
            latestStdlib.configure { dependsOn versionStdlib }
            latestTest.configure { dependsOn versionTest }
        }
        buildAllVersions.configure { dependsOn(versionStdlib, versionTest) }
    }
}
