package org.jetbrains.kotlin.r4a.frames.analysis

import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.reportFromPlugin
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.r4a.R4aUtils
import org.jetbrains.kotlin.r4a.analysis.R4ADefaultErrorMessages
import org.jetbrains.kotlin.r4a.analysis.R4AErrors
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform

class FrameModelChecker : DeclarationChecker, StorageComponentContainerContributor {

    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor
    ) {
        if (platform != JvmPlatform) return
        container.useInstance(FrameModelChecker())
    }

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor is ClassDescriptor) {
            if (!descriptor.isModelClass) return

            if (declaration.hasModifier(KtTokens.OPEN_KEYWORD) ||
                declaration.hasModifier(KtTokens.ABSTRACT_KEYWORD)) {
                val element = (declaration as? KtClass)?.nameIdentifier ?: declaration
                context.trace.reportFromPlugin(
                    R4AErrors.OPEN_MODEL.on(element),
                    R4ADefaultErrorMessages
                )
            }
        }
    }
}

private val MODEL_FQNAME = R4aUtils.r4aFqName("Model")
val DeclarationDescriptor.isModelClass: Boolean get() = annotations.hasAnnotation(MODEL_FQNAME)
