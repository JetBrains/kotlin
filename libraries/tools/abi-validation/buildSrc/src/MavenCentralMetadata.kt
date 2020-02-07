/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.validation.build

import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.publish.maven.*
import org.gradle.jvm.tasks.*
import org.gradle.kotlin.dsl.*

fun MavenPublication.mavenCentralMetadata() {
    pom {
        name.set("Binary API validator")
        description.set("Kotlin binary public API management tool")
        url.set("https://github.com/Kotlin/binary-compatibility-validator")
        licenses {
            license {
                name.set("The Apache Software License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("JetBrains")
                name.set("JetBrains Team")
                organization.set("JetBrains")
                organizationUrl.set("https://www.jetbrains.com")
            }
        }
        scm {
            url.set("https://github.com/Kotlin/binary-compatibility-validator")
        }
    }
}

fun MavenPublication.mavenCentralArtifacts(project: Project, sources: SourceDirectorySet) {
    val sourcesJar by project.tasks.creating(Jar::class) {
        archiveClassifier.set("sources")
        from(sources)
    }
    val javadocJar by project.tasks.creating(Jar::class) {
        archiveClassifier.set("javadoc")
        // contents are deliberately left empty
    }
    artifact(sourcesJar)
    artifact(javadocJar)
}
