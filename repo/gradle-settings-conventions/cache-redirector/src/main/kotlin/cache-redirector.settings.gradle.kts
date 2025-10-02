import java.net.URI

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Note: This script could not use the 'private' modifier as it is being used in Gradle integration tests with Gradle 7.6.3.
// This old Gradle version uses Kotlin runtime 1.7.10 with LV 1.4, and this runtime fails to compile this script in such a case.
// Relevant issue: https://youtrack.jetbrains.com/issue/KT-56936/Private-val-in-Gradle-precompiled-build-script-cannot-be-used-but-IDE-is-happy

internal val Settings.cacheRedirectorEnabled: Provider<Boolean>
    get() = providers
        .gradleProperty("cacheRedirectorEnabled")
        .map { it.toBoolean() }
        .orElse(false)

// Repository override section

/**
 *  The list of repositories supported by cache redirector should be synced with the "Table of redirects" at https://cache-redirector.jetbrains.com
 *  To add a repository to the list create an issue in ADM project (example issue https://youtrack.jetbrains.com/issue/IJI-149)
 *  Or send a merge request to https://jetbrains.team/p/iji/repositories/Cache-Redirector/files/64b69490c54a2a900bb3dd21471f942270289a12/images/config-gen/src/main/kotlin/Config.kt
 */
val cacheMap: Map<String, String> = mapOf(
    "https://cache-redirector.jetbrains.com/teamcity-rest-client" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/teamcity-rest-client",
    "https://cache-redirector.jetbrains.com/wormhole" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/wormhole",
    "https://dl35a2bc3xf3g.cloudfront.net/rplugin" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/rplugin",
    "https://dl35a2bc3xf3g.cloudfront.net/shellcheck/org/jetbrains/intellij/deps/shellcheck" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/intellij-third-party-dependencies/org/jetbrains/intellij/deps/shellcheck",
    "https://dl35a2bc3xf3g.cloudfront.net/shellcheck/org/jetbrains/intellij/deps/shellcheck" to "https://cache-redirector.jetbrains.com/intellij-third-party-dependencies/org/jetbrains/intellij/deps/shellcheck",
    "https://dl35a2bc3xf3g.cloudfront.net/recommenders-java/com/jetbrains/recommmenders/java" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/intellij-third-party-dependencies/com/jetbrains/recommmenders/java",
    "https://dl35a2bc3xf3g.cloudfront.net/recommenders-java/com/jetbrains/recommmenders/java" to "https://cache-redirector.jetbrains.com/intellij-third-party-dependencies/com/jetbrains/recommmenders/java",
    "https://dl35a2bc3xf3g.cloudfront.net/android-sdk" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/intellij-third-party-dependencies/android-sdk",
    "https://dl35a2bc3xf3g.cloudfront.net/android-sdk" to "https://cache-redirector.jetbrains.com/intellij-third-party-dependencies/android-sdk",
    "https://dl35a2bc3xf3g.cloudfront.net/android-sdk-offline-repo" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/intellij-third-party-dependencies/android-sdk-offline-repo",
    "https://dl35a2bc3xf3g.cloudfront.net/android-sdk-offline-repo" to "https://cache-redirector.jetbrains.com/intellij-third-party-dependencies/android-sdk-offline-repo",
    "https://dl35a2bc3xf3g.cloudfront.net/android-sdk-kotlin-plugin" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/intellij-third-party-dependencies/android-sdk-kotlin-plugin",
    "https://dl35a2bc3xf3g.cloudfront.net/android-sdk-kotlin-plugin" to "https://cache-redirector.jetbrains.com/intellij-third-party-dependencies/android-sdk-kotlin-plugin",
    "https://cache-redirector.jetbrains.com/intellij-dependencies" to "https://cache-redirector.jetbrains.com/intellij-third-party-dependencies",
    "https://cache-redirector.jetbrains.com/intellij-dependencies" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/intellij-third-party-dependencies",
    "https://cache-redirector.jetbrains.com/intellij-dependencies" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/dekaf",
    "https://cache-redirector.jetbrains.com/intellij-dependencies" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com.dekaf",
    "https://cache-redirector.jetbrains.com/intellij-dependencies" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/intellij-terraform",
    "https://cache-redirector.jetbrains.com/intellij-dependencies" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/jediterm",
    "https://cache-redirector.jetbrains.com/intellij-dependencies" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/markdown",
    "https://cache-redirector.jetbrains.com/intellij-dependencies" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com.markdown",
    "https://cache-redirector.jetbrains.com/intellij-dependencies" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/test-discovery",
    "https://cache-redirector.jetbrains.com/intellij-dependencies" to "https://cache-redirector.jetbrains.com/dl.bintray.com/jetbrains/golang",
    "https://packages.jetbrains.team/maven/p/intellij-plugin-verifier/intellij-plugin-structure" to "https://cache-redirector.jetbrains.com/packages.jetbrains.team/maven/p/intellij-plugin-verifier/intellij-plugin-structure",
    "https://packages.jetbrains.team/maven/p/intellij-plugin-verifier/intellij-plugin-verifier" to "https://cache-redirector.jetbrains.com/packages.jetbrains.team/maven/p/intellij-plugin-verifier/intellij-plugin-verifier",
    "https://packages.jetbrains.team/maven/p/teamcity-rest-client/teamcity-rest-client" to "https://cache-redirector.jetbrains.com/packages.jetbrains.team/maven/p/teamcity-rest-client/teamcity-rest-client",
    "https://packages.jetbrains.team/maven/p/teamcity-rest-client/teamcity-rest-client" to "https://cache-redirector.jetbrains.com/teamcity-rest-client",
    "https://ftp.agdsn.de/pub/mirrors/centos-altarch/7.9.2009/os/aarch64/Packages" to "https://cache-redirector.jetbrains.com/ftp.agdsn.de/pub/mirrors/centos-altarch/7.9.2009/os/aarch64/Packages",
    "https://ftp.agdsn.de/pub/mirrors/centos-altarch/7.9.2009/os/aarch64/Packages" to "https://cache-redirector.jetbrains.com/centos-7.9.2009-aarch64-packages",
    "https://archive.kernel.org/centos-vault/altarch/7.1.1503/os/aarch64/Packages" to "https://cache-redirector.jetbrains.com/centos-7.1.1503-aarch64-packages",
    "https://archive.kernel.org/centos-vault/altarch/7.1.1503/os/aarch64/Packages" to "https://cache-redirector.jetbrains.com/archive.kernel.org/centos-vault/altarch/7.1.1503/os/aarch64/Packages",
    "https://css4j.github.io/maven" to "https://cache-redirector.jetbrains.com/css4j.github.io/maven",
    "https://packages.jetbrains.team/maven/p/grazi/grazie-platform-public" to "https://cache-redirector.jetbrains.com/packages.jetbrains.team/maven/p/grazi/grazie-platform-public",
    "https://github.com/AdoptOpenJDK/openjdk14-binaries/releases/download" to "https://cache-redirector.jetbrains.com/github.com/AdoptOpenJDK/openjdk14-binaries/releases/download",
    "https://archive.kernel.org/centos-vault/7.7.1908/os/x86_64/Packages" to "https://cache-redirector.jetbrains.com/archive.kernel.org/centos-vault/7.7.1908/os/x86_64/Packages",
    "https://archive.kernel.org/centos-vault/7.7.1908/os/x86_64/Packages" to "https://cache-redirector.jetbrains.com/centos-7.7.1908-x86_64-packages",
    "https://archive.kernel.org/centos-vault/7.1.1503/os/x86_64/Packages" to "https://cache-redirector.jetbrains.com/archive.kernel.org/centos-vault/7.1.1503/os/x86_64/Packages",
    "https://archive.kernel.org/centos-vault/7.1.1503/os/x86_64/Packages" to "https://cache-redirector.jetbrains.com/centos-7.1.1503-x86_64-packages",
    "https://archive.kernel.org/centos-vault/7.0.1406/os/x86_64/Packages" to "https://cache-redirector.jetbrains.com/archive.kernel.org/centos-vault/7.0.1406/os/x86_64/Packages",
    "https://archive.kernel.org/centos-vault/7.0.1406/os/x86_64/Packages" to "https://cache-redirector.jetbrains.com/centos-7.0.1406-x86_64-packages",
    "https://packages.jetbrains.team/maven/p/ij/intellij-dependencies" to "https://cache-redirector.jetbrains.com/packages.jetbrains.team/maven/p/ij/intellij-dependencies",
    "https://packages.jetbrains.team/maven/p/ij/intellij-dependencies" to "https://cache-redirector.jetbrains.com/intellij-dependencies",
    "https://repository.jboss.org/nexus/content/repositories/public" to "https://cache-redirector.jetbrains.com/repository.jboss.org/nexus/content/repositories/public",
    "https://repo.eclipse.org/content/repositories/egit-releases" to "https://cache-redirector.jetbrains.com/repo.eclipse.org/content/repositories/egit-releases",
    "https://mirror.yandex.ru/centos/7.9.2009/os/x86_64/Packages" to "https://cache-redirector.jetbrains.com/mirror.yandex.ru/centos/7.9.2009/os/x86_64/Packages",
    "https://mirror.yandex.ru/centos/7.9.2009/os/x86_64/Packages" to "https://cache-redirector.jetbrains.com/centos-7.9.2009-x86_64-packages",
    "https://kotlin.bintray.com/kotlin-ide-plugin-dependencies" to "https://cache-redirector.jetbrains.com/kotlin.bintray.com/kotlin-ide-plugin-dependencies",
    "https://jetbrains.bintray.com/kotlin-native-dependencies" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/kotlin-native-dependencies",
    "https://github.com/git-for-windows/git/releases/download" to "https://cache-redirector.jetbrains.com/github.com/git-for-windows/git/releases/download",
    "https://github.com/webassembly/wabt/releases/download" to "https://cache-redirector.jetbrains.com/github.com/webassembly/wabt/releases/download",
    "https://github.com/webassembly/testsuite/zipball" to "https://cache-redirector.jetbrains.com/github.com/webassembly/testsuite/zipball",
    "https://archive.mozilla.org/pub/firefox/nightly" to "https://cache-redirector.jetbrains.com/archive.mozilla.org/pub/firefox/nightly",
    "https://archive.mozilla.org/pub/firefox/releases" to "https://cache-redirector.jetbrains.com/archive.mozilla.org/pub/firefox/releases",
    "https://github.com/WasmEdge/WasmEdge/releases/download" to "https://cache-redirector.jetbrains.com/github.com/WasmEdge/WasmEdge/releases/download",
    "https://github.com/bytecodealliance/wasmtime/releases/download" to "https://cache-redirector.jetbrains.com/github.com/bytecodealliance/wasmtime/releases/download",
    "https://storage.googleapis.com/chromium-v8/official/canary" to "https://cache-redirector.jetbrains.com/storage.googleapis.com/chromium-v8/official/canary",
    "https://packages.jetbrains.team/files/p/kt/kotlin-file-dependencies" to "https://cache-redirector.jetbrains.com/packages.jetbrains.team/files/p/kt/kotlin-file-dependencies",
    "https://download.visualstudio.microsoft.com/download/pr" to "https://cache-redirector.jetbrains.com/download.visualstudio.microsoft.com/download/pr",
    "https://jetbrains.bintray.com/intellij-plugin-service" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/intellij-plugin-service",
    "https://jetbrains.bintray.com/intellij-plugin-service" to "https://cache-redirector.jetbrains.com/intellij-plugin-service",
    "https://maven.exasol.com/artifactory/exasol-releases" to "https://cache-redirector.jetbrains.com/maven.exasol.com/artifactory/exasol-releases",
    "https://maven.exasol.com/artifactory/exasol-releases" to "https://cache-redirector.jetbrains.com/maven.exasol.com.releases",
    "https://www.myget.org/F/intellij-go-snapshots/maven" to "https://cache-redirector.jetbrains.com/www.myget.org/F/intellij-go-snapshots/maven",
    "https://www.myget.org/F/intellij-go-snapshots/maven" to "https://cache-redirector.jetbrains.com/myget.org.intellij-go-snapshots",
    "https://packages.jetbrains.team/maven/p/skija/maven" to "https://cache-redirector.jetbrains.com/packages.jetbrains.team/maven/p/skija/maven",
    "https://packages.jetbrains.team/maven/p/ij/wormhole" to "https://cache-redirector.jetbrains.com/packages.jetbrains.team/maven/p/ij/wormhole",
    "https://packages.jetbrains.team/maven/p/ij/wormhole" to "https://cache-redirector.jetbrains.com/wormhole",
    "https://packages.jetbrains.team/maven/p/dpgpv/maven" to "https://cache-redirector.jetbrains.com/packages.jetbrains.team/maven/p/dpgpv/maven",
    "https://packages.jetbrains.team/maven/p/dpgpv/maven" to "https://cache-redirector.jetbrains.com/download-pgp-verifier",
    "https://dl.bintray.com/jetbrains/scala-plugin-deps" to "https://cache-redirector.jetbrains.com/dl.bintray.com/jetbrains/scala-plugin-deps",
    "https://packages.jetbrains.team/maven/p/jcs/maven" to "https://cache-redirector.jetbrains.com/packages.jetbrains.team/maven/p/jcs/maven",
    "https://github.com/yarnpkg/yarn/releases/download" to "https://cache-redirector.jetbrains.com/github.com/yarnpkg/yarn/releases/download",
    "https://dl.bintray.com/kodein-framework/Kodein-DI" to "https://cache-redirector.jetbrains.com/dl.bintray.com/kodein-framework/Kodein-DI",
    "https://www.myget.org/F/rd-model-snapshots/maven" to "https://cache-redirector.jetbrains.com/www.myget.org/F/rd-model-snapshots/maven",
    "https://packages.jetbrains.team/maven/p/ki/maven" to "https://cache-redirector.jetbrains.com/packages.jetbrains.team/maven/p/ki/maven",
    "https://dl.google.com/dl/android/studio/ide-zips" to "https://cache-redirector.jetbrains.com/dl.google.com/dl/android/studio/ide-zips",
    "https://dl.bintray.com/kotlin/kotlin-js-wrappers" to "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlin-js-wrappers",
    "https://repo.typesafe.com/typesafe/ivy-releases" to "https://cache-redirector.jetbrains.com/repo.typesafe.com/typesafe/ivy-releases",
    "https://kotlin.bintray.com/kotlin-dependencies" to "https://cache-redirector.jetbrains.com/kotlin.bintray.com/kotlin-dependencies",
    "https://dl.bintray.com/kotlin/kotlin-bootstrap" to "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlin-bootstrap",
    "https://dl.bintray.com/jetbrains/maven-patched" to "https://cache-redirector.jetbrains.com/dl.bintray.com/jetbrains/maven-patched",
    "https://dl.bintray.com/scalacenter/releases" to "https://cache-redirector.jetbrains.com/dl.bintray.com/scalacenter/releases",
    "https://www.myget.org/F/rd-snapshots/maven" to "https://cache-redirector.jetbrains.com/www.myget.org/F/rd-snapshots/maven",
    "https://www.myget.org/F/rd-snapshots/maven" to "https://cache-redirector.jetbrains.com/myget.org.rd-snapshots.maven",
    "https://dl.bintray.com/kotlin/kotlinx.html" to "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlinx.html",
    "https://dl.bintray.com/konsoletyper/teavm" to "https://cache-redirector.jetbrains.com/dl.bintray.com/konsoletyper/teavm",
    "https://kotlin.bintray.com/kotlin-plugin" to "https://cache-redirector.jetbrains.com/kotlin.bintray.com/kotlin-plugin",
    "https://dl.google.com/android/repository" to "https://cache-redirector.jetbrains.com/dl.google.com/android/repository",
    "https://dl.bintray.com/scalamacros/maven" to "https://cache-redirector.jetbrains.com/dl.bintray.com/scalamacros/maven",
    "https://dl.bintray.com/kotlin/kotlin-eap" to "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlin-eap",
    "https://dl.bintray.com/kotlin/kotlin-eap" to "https://cache-redirector.jetbrains.com/kotlin-eap",
    "https://dl.bintray.com/kotlin/kotlin-dev" to "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlin-dev",
    "https://corretto.aws/downloads/resources" to "https://cache-redirector.jetbrains.com/corretto.aws/downloads/resources",
    "https://dl.google.com/dl/android/maven2" to "https://cache-redirector.jetbrains.com/dl.google.com/dl/android/maven2",
    "https://dl.google.com/dl/android/maven2" to "https://cache-redirector.jetbrains.com/dl.google.com.android.maven2",
    "https://dl.bintray.com/cy6ergn0m/maven" to "https://cache-redirector.jetbrains.com/dl.bintray.com/cy6ergn0m/maven",
    "https://cdn.azul.com/zulu-embedded/bin" to "https://cache-redirector.jetbrains.com/cdn.azul.com/zulu-embedded/bin",
    "https://dl.bintray.com/kotlin/kotlinx" to "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlinx",
    "https://dl.bintray.com/kotlin/exposed" to "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/exposed",
    "https://archive.apache.org/dist/maven" to "https://cache-redirector.jetbrains.com/archive.apache.org/dist/maven",
    "https://repo.maven.apache.org/maven2" to "https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2",
    "https://repo.jenkins-ci.org/releases" to "https://cache-redirector.jetbrains.com/repo.jenkins-ci.org/releases",
    "https://repo.jenkins-ci.org/releases" to "https://cache-redirector.jetbrains.com/repo.jenkins-ci.org.releases",
    "https://packages.confluent.io/maven/" to "https://cache-redirector.jetbrains.com/packages.confluent.io/maven/",
    "https://repo.grails.org/grails/core" to "https://cache-redirector.jetbrains.com/repo.grails.org/grails/core",
    "https://plugins.jetbrains.com/maven" to "https://cache-redirector.jetbrains.com/plugins.jetbrains.com/maven",
    "https://jetbrains.bintray.com/xodus" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/xodus",
    "https://jetbrains.bintray.com/space" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/space",
    "https://dl.bintray.com/groovy/maven" to "https://cache-redirector.jetbrains.com/dl.bintray.com/groovy/maven",
    "https://dl.bintray.com/groovy/maven" to "https://cache-redirector.jetbrains.com/dl.bintray.com.groovy",
    "https://teavm.org/maven/repository" to "https://cache-redirector.jetbrains.com/teavm.org/maven/repository",
    "https://dl.bintray.com/kotlin/ktor" to "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/ktor",
    "https://dl.bintray.com/d10xa/maven" to "https://cache-redirector.jetbrains.com/dl.bintray.com/d10xa/maven",
    "https://repo.spring.io/milestone" to "https://cache-redirector.jetbrains.com/repo.spring.io/milestone",
    "https://repo.spring.io/milestone" to "https://cache-redirector.jetbrains.com/repo.spring.io.milestone",
    "https://kotlin.bintray.com/dukat" to "https://cache-redirector.jetbrains.com/kotlin.bintray.com/dukat",
    "https://repo1.maven.org/maven2" to "https://cache-redirector.jetbrains.com/repo1.maven.org/maven2",
    "https://repo1.maven.org/maven2" to "https://cache-redirector.jetbrains.com/maven-central",
    "https://maven.fabric.io/public" to "https://cache-redirector.jetbrains.com/maven.fabric.io/public",
    "https://plugins.gradle.org/m2" to "https://cache-redirector.jetbrains.com/plugins.gradle.org/m2",
    "https://plugins.gradle.org/m2" to "https://cache-redirector.jetbrains.com/plugins.gradle.org",
    "https://cdn.azul.com/zulu/bin" to "https://cache-redirector.jetbrains.com/cdn.azul.com/zulu/bin",
    "https://downloads.gradle.org" to "https://cache-redirector.jetbrains.com/downloads.gradle.org",
    "https://downloads.apache.org" to "https://cache-redirector.jetbrains.com/downloads.apache.org",
    "https://download.eclipse.org" to "https://cache-redirector.jetbrains.com/download.eclipse.org",
    "https://services.gradle.org" to "https://cache-redirector.jetbrains.com/services.gradle.org",
    "https://jcenter.bintray.com" to "https://cache-redirector.jetbrains.com/jcenter.bintray.com",
    "https://jcenter.bintray.com" to "https://cache-redirector.jetbrains.com/jcenter",
    "https://www.python.org/ftp" to "https://cache-redirector.jetbrains.com/www.python.org/ftp",
    "https://registry.npmjs.org" to "https://cache-redirector.jetbrains.com/registry.npmjs.org",
    "https://maven.google.com" to "https://cache-redirector.jetbrains.com/maven.google.com",
    "https://dl.google.com/go" to "https://cache-redirector.jetbrains.com/dl.google.com/go",
    "https://dl.google.com/go" to "https://cache-redirector.jetbrains.com/dl.google.com.go",
    "https://clojars.org/repo" to "https://cache-redirector.jetbrains.com/clojars.org/repo",
    "https://nodejs.org/dist" to "https://cache-redirector.jetbrains.com/nodejs.org/dist",
    "https://jitpack.io" to "https://cache-redirector.jetbrains.com/jitpack.io",
    "https://repo.labs.intellij.net/intellij-nightly-sdk" to "https://cache-redirector.jetbrains.com/www.jetbrains.com/intellij-repository/nightly",
    "https://d1lc5k9lerg6km.cloudfront.net" to "https://cache-redirector.jetbrains.com/www.jetbrains.com/jps-cache/intellij",
    "https://www.jetbrains.com/intellij-repository" to "https://cache-redirector.jetbrains.com/www.jetbrains.com/intellij-repository",
    "https://www.jetbrains.com/intellij-repository" to "https://cache-redirector.jetbrains.com/intellij-repository",
    "https://download.jetbrains.com" to "https://cache-redirector.jetbrains.com/download.jetbrains.com",
    "https://secure.index-cdn.jetbrains.com" to "https://cache-redirector.jetbrains.com/secure.index-cdn.jetbrains.com",
    "https://d2xrhe97vsfxuc.cloudfront.net" to "https://cache-redirector.jetbrains.com/intellij-jbr",
    "https://d2xrhe97vsfxuc.cloudfront.net" to "https://cache-redirector.jetbrains.com/intellij-jdk",
    "https://d2xrhe97vsfxuc.cloudfront.net" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/intellij-jbr",
    "https://d2xrhe97vsfxuc.cloudfront.net" to "https://cache-redirector.jetbrains.com/jetbrains.bintray.com/intellij-jdk",
    "https://androidx.dev/snapshots/builds" to "https://cache-redirector.jetbrains.com/androidx.dev/snapshots/builds",
    "https://repo.gradle.org/gradle/libs-releases" to "https://cache-redirector.jetbrains.com/repo.gradle.org/gradle/libs-releases",
    "https://redirector.kotlinlang.org/maven/kotlin-dependencies" to "https://cache-redirector.jetbrains.com/redirector.kotlinlang.org/maven/kotlin-dependencies",
    "https://redirector.kotlinlang.org/maven/bootstrap" to "https://cache-redirector.jetbrains.com/redirector.kotlinlang.org/maven/bootstrap",
    "https://redirector.kotlinlang.org/maven/kotlin-ide-plugin-dependencies" to "https://cache-redirector.jetbrains.com/redirector.kotlinlang.org/maven/kotlin-ide-plugin-dependencies",
    "https://packages.jetbrains.team/maven/p/plan/litmuskt" to "https://cache-redirector.jetbrains.com/packages.jetbrains.team/maven/p/plan/litmuskt",
)

val aliases = mapOf(
    "https://repo.maven.apache.org/maven2" to "https://repo1.maven.org/maven2" // Maven Central
)

fun URI.maybeRedirect(): URI {
    val url = toString().trimEnd('/')
    val deAliasedUrl = aliases.getOrDefault(url, url)

    val cacheUrlEntry = cacheMap.entries.find { (origin, _) -> deAliasedUrl.startsWith(origin) }
    return if (cacheUrlEntry != null) {
        val cacheUrl = cacheUrlEntry.value
        val originRestPath = deAliasedUrl.substringAfter(cacheUrlEntry.key, "")
        URI("$cacheUrl$originRestPath")
    } else {
        this
    }
}

fun RepositoryHandler.redirect() = configureEach {
    when (this) {
        is MavenArtifactRepository -> url = url.maybeRedirect()
        is IvyArtifactRepository -> @Suppress("SENSELESS_COMPARISON") if (url != null) {
            url = url.maybeRedirect()
        }
    }
}

// Native compiler download url override section

fun Project.overrideNativeCompilerDownloadUrl() {
    logger.info("Redirecting Kotlin/Native compiler download url")
    extensions.extraProperties["kotlin.native.distribution.baseDownloadUrl"] =
        "https://cache-redirector.jetbrains.com/download.jetbrains.com/kotlin/native/builds"
}

// Check repositories are overridden section
abstract class CheckRepositoriesTask : DefaultTask() {
    @get:Input
    val teamcityBuild = project.providers
        .gradleProperty("teamcity").map { true }
        .orElse(
            project.providers.systemProperty("teamcity").map { true }
                .orElse(project.providers.environmentVariable("TEAMCITY_VERSION").map { true }.orElse(false))
        )

    @get:Input
    val ivyNonCachedRepositories = project.providers.provider {
        project.repositories
            .filterIsInstance<IvyArtifactRepository>()
            .filter {
                @Suppress("SENSELESS_COMPARISON")
                it.url == null
            }
            .map { it.name }
    }

    @get:Input
    val nonCachedRepositories = project.providers.provider {
        project.repositories.findNonCachedRepositories()
    }

    @get:Input
    val nonCachedBuildscriptsRepositories = project.providers.provider {
        project.buildscript.repositories.findNonCachedRepositories()
    }

    @get:Internal
    val projectDisplayName = project.displayName

    @TaskAction
    fun checkRepositories() {
        val testName = "$name in $projectDisplayName"
        val isTeamcityBuild = teamcityBuild.get()
        if (isTeamcityBuild) {
            testStarted(testName)
        }

        ivyNonCachedRepositories.get().forEach { ivyRepoName ->
            logInvalidIvyRepo(testName, projectDisplayName, isTeamcityBuild, ivyRepoName)
        }

        nonCachedRepositories.get().forEach { repoUrl ->
            logNonCachedRepo(testName, projectDisplayName, repoUrl, isTeamcityBuild)
        }

        nonCachedBuildscriptsRepositories.get().forEach { repoUrl ->
            logNonCachedRepo(testName, projectDisplayName, repoUrl, isTeamcityBuild)
        }

        if (isTeamcityBuild) {
            testFinished(testName)
        }
    }

    private fun URI.isCachedOrLocal() = scheme == "file" ||
            host == "cache-redirector.jetbrains.com" ||
            host == "teamcity.jetbrains.com" ||
            host == "buildserver.labs.intellij.net"

    private fun RepositoryHandler.findNonCachedRepositories(): List<String> {
        val mavenNonCachedRepos = filterIsInstance<MavenArtifactRepository>()
            .filterNot { it.url.isCachedOrLocal() }
            .map { it.url.toString() }

        val ivyNonCachedRepos = filterIsInstance<IvyArtifactRepository>()
            .filterNot { it.url.isCachedOrLocal() }
            .map { it.url.toString() }

        return mavenNonCachedRepos + ivyNonCachedRepos
    }

    private fun escape(s: String): String {
        return s.replace("[|'\\[\\]]".toRegex(), "\\|$0").replace("\n".toRegex(), "|n").replace("\r".toRegex(), "|r")
    }

    private fun testStarted(testName: String) {
        println("##teamcity[testStarted name='%s']".format(escape(testName)))
    }

    private fun testFinished(testName: String) {
        println("##teamcity[testFinished name='%s']".format(escape(testName)))
    }

    private fun testFailed(name: String, message: String, details: String) {
        println("##teamcity[testFailed name='%s' message='%s' details='%s']".format(escape(name), escape(message), escape(details)))
    }

    private fun logNonCachedRepo(
        testName: String,
        projectDisplayName: String,
        repoUrl: String,
        isTeamcityBuild: Boolean
    ) {
        val msg = "Repository $repoUrl in $projectDisplayName should be cached with cache-redirector"
        val details = "Using non cached repository may lead to download failures in CI builds." +
                " Check https://github.com/JetBrains/kotlin/blob/master/repo/gradle-settings-conventions/cache-redirector/src/main/kotlin/cache-redirector.settings.gradle.kts for details."

        if (isTeamcityBuild) {
            testFailed(testName, msg, details)
        }

        logger.warn("WARNING - $msg\n$details")
    }

    private fun logInvalidIvyRepo(
        testName: String,
        projectDisplayName: String,
        isTeamcityBuild: Boolean,
        ivyRepoName: String,
    ) {
        val msg = "Invalid ivy repo found in $projectDisplayName"
        val details = "Url must be not null for $ivyRepoName repository"

        if (isTeamcityBuild) {
            testFailed(testName, msg, details)
        }

        logger.warn("WARNING - $msg: $details")
    }
}

fun Project.addCheckRepositoriesTask() {
    val checkRepoTask = tasks.register("checkRepositories", CheckRepositoriesTask::class.java)

    tasks.configureEach {
        if (name == "checkBuild") {
            dependsOn(checkRepoTask)
        }
    }
}

// Main configuration

if (cacheRedirectorEnabled.get()) {
    logger.info("Redirecting repositories for settings in ${settingsDir.absolutePath}")

    pluginManagement.repositories.redirect()
    dependencyResolutionManagement.repositories.redirect()
    buildscript.repositories.redirect()

    gradle.beforeProject {
        buildscript.repositories.redirect()
        repositories.redirect()
        overrideNativeCompilerDownloadUrl()
        addCheckRepositoriesTask()
    }
}
