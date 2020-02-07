/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.validation.build

import org.gradle.api.*
import org.gradle.api.publish.*
import java.net.*

fun PublishingExtension.bintrayRepositoryPublishing(project: Project, user: String, repo: String, name: String) {
    repositories {
        maven {
            url = URI("https://api.bintray.com/maven/$user/$repo/$name/;publish=0")
            credentials {
                username = project.findProperty("bintrayUser") as? String ?: System.getenv("BINTRAY_USER")
                password = project.findProperty("bintrayApiKey") as? String ?: System.getenv("BINTRAY_API_KEY")
            }
        }
    }
}
