/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.cocoapods

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.CocoapodsDependency
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.SpecRepos

interface MissingInfoMessage<T> {
    val missingInfo: T
    val missingMessage: String
}

class MissingSpecReposMessage(override val missingInfo: SpecRepos) : MissingInfoMessage<SpecRepos> {
    override val missingMessage: String
        get() = missingInfo.getAll().joinToString(separator = "\n") { "source '$it'" }
}

class MissingCocoapodsMessage(override val missingInfo: CocoapodsDependency, private val project: Project) :
    MissingInfoMessage<CocoapodsDependency> {
    override val missingMessage: String
        get() = "pod '${missingInfo.name}'${missingInfo.source?.let { ", :path => '${it.getLocalPath(project, missingInfo.name)}'" } ?: ""}"
}

