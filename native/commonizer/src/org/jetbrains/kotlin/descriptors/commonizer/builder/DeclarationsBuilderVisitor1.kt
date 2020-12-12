/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.CommonizerTarget
import org.jetbrains.kotlin.descriptors.commonizer.builder.CommonizedMemberScope.Companion.plusAssign
import org.jetbrains.kotlin.descriptors.commonizer.builder.CommonizedPackageFragmentProvider.Companion.plusAssign
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.*
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirNode.Companion.dimension
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirNode.Companion.indexOfCommon
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroup
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl

/**
 * Serves two goals:
 * 1. Builds and initializes descriptors that do not depend on Kotlin types, such as [ModuleDescriptor], [PackageFragmentDescriptor], etc.
 * 2. Builds BUT not initializes classifier descriptors, so that they can be used during the next phase for construction of Kotlin types.
 */
internal class DeclarationsBuilderVisitor1(
    private val components: GlobalDeclarationsBuilderComponents
) : CirNodeVisitor<List<DeclarationDescriptor?>, List<DeclarationDescriptor?>> {
    override fun visitRootNode(node: CirRootNode, data: List<DeclarationDescriptor?>): List<DeclarationDescriptor?> {
        check(data.isEmpty()) // root node may not have containing declarations
        check(components.targetComponents.size == node.dimension)

        val allTargets: List<CommonizerTarget> = (node.targetDeclarations + node.commonDeclaration()).map { it!!.target }
        val modulesByTargets: Map<CommonizerTarget, MutableList<ModuleDescriptorImpl>> =
            allTargets.associateWithTo(HashMap()) { mutableListOf() }

        // collect module descriptors:
        for (moduleNode in node.modules.values) {
            val modules = moduleNode.accept(this, noContainingDeclarations()).asListContaining<ModuleDescriptorImpl>()
            modules.forEachIndexed { index, module ->
                if (module != null) {
                    val target = allTargets[index]
                    modulesByTargets.getValue(target) += module
                }
            }
        }

        // return result (preserving order of targets):
        allTargets.forEachIndexed { index, target ->
            components.cache.cache(index, modulesByTargets.getValue(target))
        }

        return noReturningDeclarations()
    }

    override fun visitModuleNode(node: CirModuleNode, data: List<DeclarationDescriptor?>): List<ModuleDescriptorImpl?> {
        // build module descriptors:
        val moduleDescriptorsGroup = CommonizedGroup<ModuleDescriptorImpl>(node.dimension)
        node.buildDescriptors(components, moduleDescriptorsGroup)

        // build package fragments:
        val packageFragmentProviders = CommonizedPackageFragmentProvider.createArray(node.dimension)
        for (packageNode in node.packages.values) {
            val packageFragments = packageNode.accept(this, moduleDescriptorsGroup).asListContaining<PackageFragmentDescriptor>()
            packageFragmentProviders += packageFragments
        }

        // initialize module descriptors:
        moduleDescriptorsGroup.forEachIndexed { index, moduleDescriptor ->
            moduleDescriptor?.initialize(packageFragmentProviders[index])
        }

        return moduleDescriptorsGroup
    }

    override fun visitPackageNode(node: CirPackageNode, data: List<DeclarationDescriptor?>): List<PackageFragmentDescriptor?> {
        val containingDeclarations = data.asListContaining<ModuleDescriptorImpl>()

        // build package fragments:
        val packageFragments = CommonizedGroup<CommonizedPackageFragmentDescriptor>(node.dimension)
        node.buildDescriptors(components, packageFragments, containingDeclarations)

        // build package members:
        val packageMemberScopes = CommonizedMemberScope.createArray(node.dimension)
        for (classNode in node.classes.values) {
            packageMemberScopes += classNode.accept(this, packageFragments)
        }
        for (typeAliasNode in node.typeAliases.values) {
            packageMemberScopes += typeAliasNode.accept(this, packageFragments)
        }

        // initialize package fragments:
        packageFragments.forEachIndexed { index, packageFragment ->
            packageFragment?.initialize(packageMemberScopes[index])
        }

        return packageFragments
    }

    override fun visitPropertyNode(node: CirPropertyNode, data: List<DeclarationDescriptor?>) =
        error("This method should not be called in ${this::class.java}")

    override fun visitFunctionNode(node: CirFunctionNode, data: List<DeclarationDescriptor?>) =
        error("This method should not be called in ${this::class.java}")

    override fun visitClassNode(node: CirClassNode, data: List<DeclarationDescriptor?>): List<DeclarationDescriptor?> {
        val classifiers = CommonizedGroup<ClassifierDescriptorWithTypeParameters>(node.dimension)
        node.buildDescriptors(components, classifiers, data)
        val classes = classifiers.asListContaining<CommonizedClassDescriptor>()

        // build class members:
        val classMemberScopes = CommonizedMemberScope.createArray(node.dimension)
        for (classNode in node.classes.values) {
            classMemberScopes += classNode.accept(this, classes)
        }

        // save member scope
        classes.forEachIndexed { index, clazz ->
            clazz?.unsubstitutedMemberScope = classMemberScopes[index]
        }

        return classes
    }

    override fun visitClassConstructorNode(node: CirClassConstructorNode, data: List<DeclarationDescriptor?>) =
        error("This method should not be called in ${this::class.java}")

    override fun visitTypeAliasNode(node: CirTypeAliasNode, data: List<DeclarationDescriptor?>): List<DeclarationDescriptor?> {
        val classifiers = CommonizedGroup<ClassifierDescriptorWithTypeParameters>(node.dimension)
        node.buildDescriptors(components, classifiers, data)

        val commonClass = classifiers[node.indexOfCommon] as? CommonizedClassDescriptor?
        commonClass?.unsubstitutedMemberScope = CommonizedMemberScope() // empty member scope
        commonClass?.initialize(emptyList()) // no constructors

        return classifiers
    }

    companion object {
        internal inline fun <reified T : DeclarationDescriptor> noContainingDeclarations() = emptyList<T?>()
        internal inline fun <reified T : DeclarationDescriptor> noReturningDeclarations() = emptyList<T?>()

        @Suppress("UNCHECKED_CAST")
        internal inline fun <reified T : DeclarationDescriptor> List<DeclarationDescriptor?>.asListContaining(): List<T?> =
            this as List<T?>
    }
}
