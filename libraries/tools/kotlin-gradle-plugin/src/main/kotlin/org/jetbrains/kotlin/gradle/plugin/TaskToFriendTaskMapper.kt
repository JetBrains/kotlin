/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Task
import org.jetbrains.annotations.TestOnly

internal abstract class TaskToFriendTaskMapper {
    operator fun get(task: Task): String? =
        getFriendByName(task.name)

    @TestOnly
    operator fun get(name: String): String? =
        getFriendByName(name)

    protected abstract fun getFriendByName(name: String): String?
}

sealed internal class RegexTaskToFriendTaskMapper(
    private val prefix: String,
    suffix: String,
    private val targetName: String,
    private val postfixReplacement: String
) : TaskToFriendTaskMapper() {
    class Default(targetName: String) : RegexTaskToFriendTaskMapper("compile", "TestKotlin", targetName, "Kotlin")
    class Android(targetName: String) : RegexTaskToFriendTaskMapper("compile", "(Unit|Android)TestKotlin", targetName, "Kotlin")

    private val regex = "$prefix(.*)$suffix${targetName.capitalize()}".toRegex()

    override fun getFriendByName(name: String): String? {
        val match = regex.matchEntire(name) ?: return null
        val variant = match.groups[1]?.value ?: ""
        return prefix + variant + postfixReplacement + targetName.capitalize()
    }
}