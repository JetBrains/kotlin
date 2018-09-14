/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.konan

// FIXME(ddol): KLIB-REFACTORING-CLEANUP: remove the whole file!

/**
 *  https://en.wikipedia.org/wiki/Software_versioning
 *  scheme major.minor[.build[.revision]].
 */

enum class MetaVersion(val metaString: String) {
    DEV("dev"),
    EAP("eap"),
    ALPHA("alpha"),
    BETA("beta"),
    RC1("rc1"),
    RC2("rc2"),
    RELEASE("release");

    companion object {

        fun findAppropriate(metaString: String): MetaVersion {
            return MetaVersion.values().find { it.metaString.equals(metaString, ignoreCase = true) }
                    ?: if (metaString.isBlank()) RELEASE else error("Unknown meta version: $metaString")
        }
    }
}
