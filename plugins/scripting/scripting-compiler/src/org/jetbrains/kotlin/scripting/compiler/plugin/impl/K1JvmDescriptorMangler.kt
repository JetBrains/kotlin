/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinExportChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleConstant
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorBasedKotlinManglerImpl
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorExportCheckerVisitor
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorMangleComputer
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.load.java.lazy.descriptors.isJavaField
import org.jetbrains.kotlin.load.java.typeEnhancement.hasEnhancedNullability
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext

@OptIn(K1Deprecation::class)
open class K1JvmDescriptorMangler(private val mainDetector: MainFunctionDetector?) : DescriptorBasedKotlinManglerImpl() {
    private object ExportChecker : DescriptorExportCheckerVisitor() {
        override fun DeclarationDescriptor.isPlatformSpecificExported() = true
    }

    open class JvmDescriptorManglerComputer(
        builder: StringBuilder,
        private val mainDetector: MainFunctionDetector?,
        mode: MangleMode,
        useEffectiveTypeVariances: Boolean = false
    ) : DescriptorMangleComputer(builder, mode, useEffectiveTypeVariances = useEffectiveTypeVariances) {
        override fun addReturnTypeSpecialCase(function: FunctionDescriptor): Boolean = true

        override fun copy(newMode: MangleMode): DescriptorMangleComputer = JvmDescriptorManglerComputer(builder, mainDetector, newMode)

        private fun isMainFunction(descriptor: FunctionDescriptor): Boolean =
            mainDetector != null && mainDetector.isMain(descriptor)

        override fun FunctionDescriptor.platformSpecificSuffix(): String? =
            if (isMainFunction(this)) source.containingFile.name else null

        override fun PropertyDescriptor.platformSpecificSuffix(): String? {
            // Since LV 1.4 there is a feature PreferJavaFieldOverload which allows to have java and kotlin
            // properties with the same signature on the same level.
            // For more details see JvmPlatformOverloadsSpecificityComparator.kt
            return if (isJavaField) MangleConstant.JAVA_FIELD_SUFFIX else null
        }

        override fun visitModuleDeclaration(descriptor: ModuleDescriptor) {
            // In general, having module descriptor as `containingDeclaration` for regular declaration is considered an error (in JS/Native)
            // because there should be `PackageFragmentDescriptor` in between
            // but on JVM there is `SyntheticJavaPropertyDescriptor` whose parent is a module. So let just skip it.
        }

        override fun mangleTypePlatformSpecific(type: KotlinType, tBuilder: StringBuilder) {
            // Disambiguate between 'double' and '@NotNull java.lang.Double' types in mixed Java/Kotlin class hierarchies
            if (SimpleClassicTypeSystemContext.hasEnhancedNullability(type)) {
                tBuilder.appendSignature(MangleConstant.ENHANCED_NULLABILITY_MARK)
            }
        }
    }

    override fun getExportChecker(compatibleMode: Boolean): KotlinExportChecker<DeclarationDescriptor> = ExportChecker

    override fun getMangleComputer(mode: MangleMode, compatibleMode: Boolean): KotlinMangleComputer<DeclarationDescriptor> =
        JvmDescriptorManglerComputer(StringBuilder(256), mainDetector, mode)
}
