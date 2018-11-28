/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */


package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

interface KotlinCompilationFactory<T : KotlinCompilation<*>> : NamedDomainObjectFactory<T> {
    val itemClass: Class<T>
}

class KotlinCommonCompilationFactory(
    val target: KotlinOnlyTarget<KotlinCommonCompilation>
) : KotlinCompilationFactory<KotlinCommonCompilation> {
    override val itemClass: Class<KotlinCommonCompilation>
        get() = KotlinCommonCompilation::class.java

    override fun create(name: String): KotlinCommonCompilation =
        KotlinCommonCompilation(target, name)
}

class KotlinJvmCompilationFactory(
    val target: KotlinOnlyTarget<KotlinJvmCompilation>
) : KotlinCompilationFactory<KotlinJvmCompilation> {
    override val itemClass: Class<KotlinJvmCompilation>
        get() = KotlinJvmCompilation::class.java

    override fun create(name: String): KotlinJvmCompilation =
        KotlinJvmCompilation(target, name)
}

class KotlinWithJavaCompilationFactory<KotlinOptionsType : KotlinCommonOptions>(
    val project: Project,
    val target: KotlinWithJavaTarget<KotlinOptionsType>
) : KotlinCompilationFactory<KotlinWithJavaCompilation<KotlinOptionsType>> {

    override val itemClass: Class<KotlinWithJavaCompilation<KotlinOptionsType>>
        @Suppress("UNCHECKED_CAST")
        get() = KotlinWithJavaCompilation::class.java as Class<KotlinWithJavaCompilation<KotlinOptionsType>>

    override fun create(name: String): KotlinWithJavaCompilation<KotlinOptionsType> {
        val result = KotlinWithJavaCompilation(target, name)
        return result
    }
}

class KotlinJvmAndroidCompilationFactory(
    val project: Project,
    val target: KotlinAndroidTarget
) : KotlinCompilationFactory<KotlinJvmAndroidCompilation> {

    override val itemClass: Class<KotlinJvmAndroidCompilation>
        get() = KotlinJvmAndroidCompilation::class.java

    override fun create(name: String): KotlinJvmAndroidCompilation =
        KotlinJvmAndroidCompilation(target, name)
}

class KotlinJsCompilationFactory(
    val project: Project,
    val target: KotlinOnlyTarget<KotlinJsCompilation>
) : KotlinCompilationFactory<KotlinJsCompilation> {
    override val itemClass: Class<KotlinJsCompilation>
        get() = KotlinJsCompilation::class.java

    override fun create(name: String): KotlinJsCompilation =
        KotlinJsCompilation(target, name)
}

class KotlinNativeCompilationFactory(
    val project: Project,
    val target: KotlinNativeTarget
) : KotlinCompilationFactory<KotlinNativeCompilation> {

    override val itemClass: Class<KotlinNativeCompilation>
        get() = KotlinNativeCompilation::class.java

    override fun create(name: String): KotlinNativeCompilation =
        KotlinNativeCompilation(target, name).apply {
            if (name == KotlinCompilation.TEST_COMPILATION_NAME) {
                friendCompilationName = KotlinCompilation.MAIN_COMPILATION_NAME
                outputKinds = mutableListOf(NativeOutputKind.EXECUTABLE)
                buildTypes = mutableListOf(NativeBuildType.DEBUG)
                isTestCompilation = true
            } else {
                buildTypes = mutableListOf(NativeBuildType.DEBUG, NativeBuildType.RELEASE)
            }
        }

}