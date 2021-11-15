/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.jvm

/*
* Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
*/


import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsImpl
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinCompilationToRunnableFiles
import org.jetbrains.kotlin.gradle.targets.external.KotlinExternalTarget

class KotlinJvmExternalCompilation
internal constructor(
    target: KotlinExternalTarget,
    name: String,
    private val defaultSourceSetNameOption: DefaultSourceSetNameOption = DefaultSourceSetNameOption.KotlinConvention
) : AbstractKotlinCompilationToRunnableFiles<KotlinJvmOptions>(target, name) {

    sealed class DefaultSourceSetNameOption {
        object KotlinConvention : DefaultSourceSetNameOption()
        data class Name(val name: String) : DefaultSourceSetNameOption()
    }

    override val defaultSourceSetName: String
        get() = when (defaultSourceSetNameOption) {
            is DefaultSourceSetNameOption.KotlinConvention -> super.defaultSourceSetName
            is DefaultSourceSetNameOption.Name -> defaultSourceSetNameOption.name
        }

    override val kotlinOptions: KotlinJvmOptions = KotlinJvmOptionsImpl()
}
