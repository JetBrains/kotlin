/*
* Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
*/

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.file.SourceDirectorySet
import org.gradle.util.WrapUtil
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

class KotlinJsIrCompilation(
    target: KotlinTarget,
    name: String
) : KotlinJsCompilation(target, name) {

    internal val binaries: KotlinJsBinaryContainer =
        target.project.objects.newInstance(
            KotlinJsBinaryContainer::class.java,
            target,
            WrapUtil.toDomainObjectSet(JsBinary::class.java)
        )

    internal val allSources: MutableSet<SourceDirectorySet> = mutableSetOf()

    override fun addSourcesToCompileTask(sourceSet: KotlinSourceSet, addAsCommonSources: Lazy<Boolean>) {
        super.addSourcesToCompileTask(sourceSet, addAsCommonSources)
        allSources.add(sourceSet.kotlin)
    }

    override val disambiguationClassifierInPlatform: String?
        get() = (target as KotlinJsIrTarget).disambiguationClassifierInPlatform

    override val defaultSourceSetName: String
        get() {
            return lowerCamelCaseName(
                if ((target as KotlinJsIrTarget).mixedMode)
                    target.disambiguationClassifierInPlatform
                else
                    target.disambiguationClassifier,
                compilationName
            )
        }
}