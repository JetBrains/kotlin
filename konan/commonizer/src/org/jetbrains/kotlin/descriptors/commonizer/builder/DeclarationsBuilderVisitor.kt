/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.CommonizedGroup
import org.jetbrains.kotlin.descriptors.commonizer.TargetId
import org.jetbrains.kotlin.descriptors.commonizer.builder.CommonizedMemberScope.Companion.plusAssign
import org.jetbrains.kotlin.descriptors.commonizer.builder.CommonizedPackageFragmentProvider.Companion.plusAssign
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.addIfNotNull

/** Builds and initializes the new tree of common descriptors */
internal class DeclarationsBuilderVisitor(
    private val storageManager: StorageManager,
    private val builtIns: KotlinBuiltIns,
    private val collector: (TargetId, Collection<ModuleDescriptor>) -> Unit
) : NodeVisitor<List<DeclarationDescriptor?>, List<DeclarationDescriptor?>> {
    override fun visitRootNode(node: RootNode, data: List<DeclarationDescriptor?>): List<DeclarationDescriptor?> {
        val allTargets = (node.target + node.common()!!).map { it.targetId }

        val modulesByTargets = HashMap<TargetId, MutableList<ModuleDescriptorImpl>>()

        // collect module descriptors:
        for (moduleNode in node.modules) {
            val modules = moduleNode.accept(this, noContainingDeclarations()).asListContaining<ModuleDescriptorImpl>()
            modules.forEachIndexed { index, module ->
                val target = allTargets[index]
                modulesByTargets.computeIfAbsent(target) { mutableListOf() }.addIfNotNull(module)
            }
        }

        // set cross-module dependencies:
        modulesByTargets.values.forEach { modulesSameTarget ->
            for (module in modulesSameTarget)
                module.setDependencies(modulesSameTarget)
        }

        // return result (preserve platforms order):
        for (target in allTargets) {
            collector(target, modulesByTargets[target]!!)
        }

        return noReturningDeclarations()
    }

    override fun visitModuleNode(node: ModuleNode, data: List<DeclarationDescriptor?>): List<ModuleDescriptorImpl?> {
        // build module descriptors:
        val moduleDescriptorsGroup = CommonizedGroup<ModuleDescriptorImpl>(node.dimension)
        node.buildDescriptors(moduleDescriptorsGroup, storageManager, builtIns)
        val moduleDescriptors = moduleDescriptorsGroup.toList()

        // build package fragments:
        val packageFragmentProviders = CommonizedPackageFragmentProvider.createArray(node.dimension)
        for (packageNode in node.packages) {
            val packageFragments = packageNode.accept(this, moduleDescriptors).asListContaining<PackageFragmentDescriptor>()
            packageFragmentProviders += packageFragments
        }

        // initialize module descriptors:
        moduleDescriptors.forEachIndexed { index, moduleDescriptor ->
            moduleDescriptor?.initialize(packageFragmentProviders[index])
        }

        return moduleDescriptors
    }

    override fun visitPackageNode(node: PackageNode, data: List<DeclarationDescriptor?>): List<PackageFragmentDescriptor?> {
        val containingDeclarations = data.asListContaining<ModuleDescriptorImpl>()

        // build package fragments:
        val packageFragmentsGroup = CommonizedGroup<CommonizedPackageFragmentDescriptor>(node.dimension)
        node.buildDescriptors(packageFragmentsGroup, containingDeclarations)
        val packageFragments = packageFragmentsGroup.toList()

        // build package members:
        val packageMemberScopes = CommonizedMemberScope.createArray(node.dimension)
        for (propertyNode in node.properties) {
            packageMemberScopes += propertyNode.accept(this, packageFragments)
        }
        for (functionNode in node.functions) {
            packageMemberScopes += functionNode.accept(this, packageFragments)
        }
        for (classNode in node.classes) {
            packageMemberScopes += classNode.accept(this, packageFragments)
        }
        for (typeAliasNode in node.typeAliases) {
            packageMemberScopes += typeAliasNode.accept(this, packageFragments)
        }

        // initialize package fragments:
        packageFragments.forEachIndexed { index, packageFragment ->
            packageFragment?.initialize(packageMemberScopes[index])
        }

        return packageFragments
    }

    override fun visitPropertyNode(node: PropertyNode, data: List<DeclarationDescriptor?>): List<PropertyDescriptor?> {
        val propertyDescriptorsGroup = CommonizedGroup<PropertyDescriptor>(node.dimension)
        node.buildDescriptors(propertyDescriptorsGroup, data, storageManager)

        return propertyDescriptorsGroup.toList()
    }

    override fun visitFunctionNode(node: FunctionNode, data: List<DeclarationDescriptor?>): List<DeclarationDescriptor?> {
        val functionDescriptorsGroup = CommonizedGroup<SimpleFunctionDescriptor>(node.dimension)
        node.buildDescriptors(functionDescriptorsGroup, data)

        return functionDescriptorsGroup.toList()
    }

    override fun visitClassNode(node: ClassNode, data: List<DeclarationDescriptor?>): List<DeclarationDescriptor?> {
        val classesGroup = CommonizedGroup<ClassifierDescriptorWithTypeParameters>(node.dimension)
        node.buildDescriptors(classesGroup, data, storageManager)
        val classes = classesGroup.toList().asListContaining<CommonizedClassDescriptor>()

        // build class constructors:
        val allConstructorsByTargets = Array<MutableList<CommonizedClassConstructorDescriptor>>(node.dimension) { ArrayList() }
        for (constructorNode in node.constructors) {
            val constructorsByTargets = constructorNode.accept(this, classes).asListContaining<CommonizedClassConstructorDescriptor>()
            constructorsByTargets.forEachIndexed { index, constructor ->
                if (constructor != null) allConstructorsByTargets[index].add(constructor)
            }
        }

        // build class members:
        val classMemberScopes = CommonizedMemberScope.createArray(node.dimension)
        for (propertyNode in node.properties) {
            classMemberScopes += propertyNode.accept(this, classes)
        }
        for (functionNode in node.functions) {
            classMemberScopes += functionNode.accept(this, classes)
        }
        for (classNode in node.classes) {
            classMemberScopes += classNode.accept(this, classes)
        }

        // initialize classes
        classes.forEachIndexed { index, clazz ->
            clazz?.initialize(classMemberScopes[index], allConstructorsByTargets[index])
        }

        return classes
    }

    override fun visitClassConstructorNode(node: ClassConstructorNode, data: List<DeclarationDescriptor?>): List<DeclarationDescriptor?> {
        val containingDeclarations = data.asListContaining<ClassDescriptor>()

        val constructorsGroup = CommonizedGroup<ClassConstructorDescriptor>(node.dimension)
        node.buildDescriptors(constructorsGroup, containingDeclarations)

        return constructorsGroup.toList()
    }

    override fun visitTypeAliasNode(node: TypeAliasNode, data: List<DeclarationDescriptor?>): List<DeclarationDescriptor?> {
        val typeAliasesGroup = CommonizedGroup<ClassifierDescriptorWithTypeParameters>(node.dimension)
        node.buildDescriptors(typeAliasesGroup, data, storageManager)
        val typeAliases = typeAliasesGroup.toList()

        val commonClass = typeAliases[node.indexOfCommon] as CommonizedClassDescriptor?
        commonClass?.initialize(MemberScope.Empty, emptyList())

        return typeAliases
    }

    companion object {
        inline fun <reified T : DeclarationDescriptor> noContainingDeclarations() = emptyList<T?>()
        inline fun <reified T : DeclarationDescriptor> noReturningDeclarations() = emptyList<T?>()
    }
}

@Suppress("UNCHECKED_CAST")
private inline fun <reified T : DeclarationDescriptor> List<DeclarationDescriptor?>.asListContaining(): List<T?> =
    this as List<T?>
