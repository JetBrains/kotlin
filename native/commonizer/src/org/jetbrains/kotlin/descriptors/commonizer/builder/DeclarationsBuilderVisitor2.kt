/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.builder.CommonizedMemberScope.Companion.plusAssign
import org.jetbrains.kotlin.descriptors.commonizer.builder.DeclarationsBuilderVisitor1.Companion.asListContaining
import org.jetbrains.kotlin.descriptors.commonizer.builder.DeclarationsBuilderVisitor1.Companion.noContainingDeclarations
import org.jetbrains.kotlin.descriptors.commonizer.builder.DeclarationsBuilderVisitor1.Companion.noReturningDeclarations
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.*
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirNode.Companion.dimension
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroup
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl

/** Builds and initializes the new tree of common descriptors */

/**
 * This visitor should be applied right after [DeclarationsBuilderVisitor1]. It does the following:
 * 1. Builds and initializes descriptors that depend on Kotlin types, such as [PropertyDescriptor], [SimpleFunctionDescriptor], etc.
 * 2. Initializes classifier descriptors.
 */
internal class DeclarationsBuilderVisitor2(
    private val components: GlobalDeclarationsBuilderComponents
) : CirNodeVisitor<List<DeclarationDescriptor?>, List<DeclarationDescriptor?>> {
    override fun visitRootNode(node: CirRootNode, data: List<DeclarationDescriptor?>): List<DeclarationDescriptor?> {
        check(data.isEmpty()) // root node may not have containing declarations
        check(components.targetComponents.size == node.dimension)

        // visit module descriptors:
        for (moduleNode in node.modules.values) {
            moduleNode.accept(this, noContainingDeclarations())
        }

        return noReturningDeclarations()
    }

    override fun visitModuleNode(node: CirModuleNode, data: List<DeclarationDescriptor?>): List<ModuleDescriptorImpl?> {
        // visit package fragment descriptors:
        for (packageNode in node.packages.values) {
            packageNode.accept(this, noContainingDeclarations())
        }

        return noReturningDeclarations()
    }

    override fun visitPackageNode(node: CirPackageNode, data: List<DeclarationDescriptor?>): List<PackageFragmentDescriptor?> {
        val packageFragments = components.cache.getCachedPackageFragments(node.moduleName, node.fqName)

        // build non-classifier package members:
        val packageMemberScopes = packageFragments.map { it?.getMemberScope() }
        for (propertyNode in node.properties.values) {
            packageMemberScopes += propertyNode.accept(this, packageFragments)
        }
        for (functionNode in node.functions.values) {
            packageMemberScopes += functionNode.accept(this, packageFragments)
        }

        for (classNode in node.classes.values) {
            classNode.accept(this, noContainingDeclarations())
        }

        return noReturningDeclarations()
    }

    override fun visitPropertyNode(node: CirPropertyNode, data: List<DeclarationDescriptor?>): List<PropertyDescriptor?> {
        val propertyDescriptors = CommonizedGroup<PropertyDescriptor>(node.dimension)
        node.buildDescriptors(components, propertyDescriptors, data)

        return propertyDescriptors
    }

    override fun visitFunctionNode(node: CirFunctionNode, data: List<DeclarationDescriptor?>): List<DeclarationDescriptor?> {
        val functionDescriptors = CommonizedGroup<SimpleFunctionDescriptor>(node.dimension)
        node.buildDescriptors(components, functionDescriptors, data)

        return functionDescriptors
    }

    override fun visitClassNode(node: CirClassNode, data: List<DeclarationDescriptor?>): List<DeclarationDescriptor?> {
        val classes = components.cache.getCachedClasses(node.fqName)

        // build class constructors:
        val allConstructorsByTargets = Array<MutableList<CommonizedClassConstructorDescriptor>>(node.dimension) { ArrayList() }
        for (constructorNode in node.constructors.values) {
            val constructorsByTargets = constructorNode.accept(this, classes).asListContaining<CommonizedClassConstructorDescriptor>()
            constructorsByTargets.forEachIndexed { index, constructor ->
                if (constructor != null) allConstructorsByTargets[index].add(constructor)
            }
        }

        // initialize classes
        classes.forEachIndexed { index, clazz ->
            clazz?.initialize(allConstructorsByTargets[index])
        }

        // build class members:
        val classMemberScopes = classes.map { it?.unsubstitutedMemberScope }
        for (propertyNode in node.properties.values) {
            classMemberScopes += propertyNode.accept(this, classes)
        }
        for (functionNode in node.functions.values) {
            classMemberScopes += functionNode.accept(this, classes)
        }

        for (classNode in node.classes.values) {
            classNode.accept(this, noContainingDeclarations())
        }

        return noReturningDeclarations()
    }

    override fun visitClassConstructorNode(node: CirClassConstructorNode, data: List<DeclarationDescriptor?>): List<DeclarationDescriptor?> {
        val containingDeclarations = data.asListContaining<CommonizedClassDescriptor>()

        val constructors = CommonizedGroup<ClassConstructorDescriptor>(node.dimension)
        node.buildDescriptors(components, constructors, containingDeclarations)

        return constructors
    }

    override fun visitTypeAliasNode(node: CirTypeAliasNode, data: List<DeclarationDescriptor?>) =
        error("This method should not be called in ${this::class.java}")
}
