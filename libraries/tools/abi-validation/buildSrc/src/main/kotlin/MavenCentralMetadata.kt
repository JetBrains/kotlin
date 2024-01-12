/*
 * Copyright 2016-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.validation.build

import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.withType

fun PublishingExtension.mavenCentralMetadata() {
    publications.withType(MavenPublication::class) {
        pom {
            if (!name.isPresent) {
                name.set(artifactId)
            }
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
}
