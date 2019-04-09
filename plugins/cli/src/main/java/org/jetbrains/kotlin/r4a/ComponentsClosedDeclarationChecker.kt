package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.reportFromPlugin
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.r4a.analysis.ComponentMetadata
import org.jetbrains.kotlin.r4a.analysis.R4ADefaultErrorMessages
import org.jetbrains.kotlin.r4a.analysis.R4AErrors
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform

class ComponentsClosedDeclarationChecker :
    DeclarationChecker,
    StorageComponentContainerContributor {

    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor
    ) {
        if (platform != JvmPlatform) return
        container.useInstance(ComponentsClosedDeclarationChecker())
    }

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor !is ClassDescriptor || declaration !is KtClass) return
        if (descriptor.kind != ClassKind.CLASS) return
        if (!ComponentMetadata.isR4AComponent(descriptor)) return

        if (declaration.hasModifier(KtTokens.OPEN_KEYWORD) ||
            declaration.hasModifier(KtTokens.ABSTRACT_KEYWORD)) {
            val element = declaration.nameIdentifier ?: declaration
            context.trace.reportFromPlugin(
                R4AErrors.OPEN_COMPONENT.on(element),
                R4ADefaultErrorMessages
            )
        }
    }
}
