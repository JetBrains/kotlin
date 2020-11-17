/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.execution.TestStateStorage
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.DummyLibraryProperties
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.roots.ui.configuration.libraries.CustomLibraryDescription
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.gradle.KotlinPlatform
import org.jetbrains.kotlin.idea.caches.project.isTestModule
import org.jetbrains.kotlin.idea.facet.externalSystemNativeMainRunTasks
import org.jetbrains.kotlin.idea.framework.KotlinLibraryKind
import org.jetbrains.kotlin.idea.highlighter.KotlinTestRunLineMarkerContributor.Companion.getTestStateIcon
import org.jetbrains.kotlin.idea.isMainFunction
import org.jetbrains.kotlin.idea.platform.IdePlatformKindTooling
import org.jetbrains.kotlin.idea.platform.isKotlinTestDeclaration
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.platform.impl.NativeIdePlatformKind
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import javax.swing.Icon

class NativeIdePlatformKindTooling : IdePlatformKindTooling() {

    override val kind = NativeIdePlatformKind

    override fun compilerArgumentsForProject(project: Project): CommonCompilerArguments? = null

    override val mavenLibraryIds: List<String> get() = emptyList()
    override val gradlePluginId: String get() = ""
    override val gradlePlatformIds: List<KotlinPlatform> get() = listOf(KotlinPlatform.NATIVE)

    override val libraryKind: PersistentLibraryKind<*> = NativeLibraryKind
    override fun getLibraryDescription(project: Project): CustomLibraryDescription? = null
    override fun getLibraryVersionProvider(project: Project): (Library) -> String? = { null }

    override fun getTestIcon(declaration: KtNamedDeclaration, descriptor: DeclarationDescriptor): Icon? {
        if (!descriptor.isKotlinTestDeclaration()) return null

        val moduleName = descriptor.module.stableName?.asString() ?: ""
        val targetName = moduleName.substringAfterLast(".").removeSuffix("Test>")

        val urls = when (declaration) {
            is KtClassOrObject -> {
                val lightClass = declaration.toLightClass() ?: return null
                listOf("java:suite://${lightClass.qualifiedName}")
            }

            is KtNamedFunction -> {
                val lightMethod = declaration.toLightMethods().firstOrNull() ?: return null
                val lightClass = lightMethod.containingClass as? KtLightClass ?: return null
                val baseName = "java:test://${lightClass.qualifiedName}.${lightMethod.name}"
                listOf("$baseName[${targetName}X64]", "$baseName[$targetName]", baseName)
            }

            else -> return null
        }

        return getTestStateIcon(urls, declaration.project, strict = false)
    }

    override fun acceptsAsEntryPoint(function: KtFunction): Boolean {
        if (!function.isMainFunction()) return false
        val functionName = function.fqName?.asString() ?: return false

        val module = function.module ?: return false
        if (module.isTestModule) return false

        val hasRunTask = module.externalSystemNativeMainRunTasks().any { it.entryPoint == functionName }
        if (!hasRunTask) return false

        val hasRunConfigurations = RunConfigurationProducer
            .getProducers(function.project)
            .asSequence()
            .filterIsInstance<KotlinNativeRunConfigurationProvider>()
            .any { !it.isForTests }
        if (!hasRunConfigurations) return false

        return true
    }
}

object NativeLibraryKind : PersistentLibraryKind<DummyLibraryProperties>("kotlin.native"), KotlinLibraryKind {
    override val compilerPlatform: TargetPlatform
        get() = NativePlatforms.unspecifiedNativePlatform

    override fun createDefaultProperties() = DummyLibraryProperties.INSTANCE!!
}
