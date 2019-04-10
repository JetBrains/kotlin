/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

class PackageJson(
    var name: String,
    var version: String
) {
    val empty: Boolean
        get() = private == null && workspaces == null && dependencies.isEmpty()

    var private: Boolean? = null

    var workspaces: Collection<String>? = null

    val dependencies = mutableMapOf<String, String>()
}