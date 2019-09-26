/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.utils.Printer

class CommonizedMemberScope : MemberScopeImpl() {
    private val members = ArrayList<DeclarationDescriptor>()

    operator fun plusAssign(member: DeclarationDescriptor) {
        this.members += member
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
        members.filteredBy<ClassifierDescriptor>(name).firstOrNull()

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> =
        members.filteredBy<PropertyDescriptor>(name).toList()

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> =
        members.filteredBy<SimpleFunctionDescriptor>(name).toList()

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> = members.filter { kindFilter.accepts(it) && nameFilter(it.name) }

    override fun printScopeStructure(p: Printer) {
        p.println(this::class.java.simpleName, " {")
        p.pushIndent()

        p.println("declarations = $members")

        p.popIndent()
        p.println("}")
    }

    companion object {
        fun createArray(size: Int) = Array(size) { CommonizedMemberScope() }

        operator fun Array<CommonizedMemberScope>.plusAssign(members: List<DeclarationDescriptor?>) {
            members.forEachIndexed { index, member ->
                if (member != null) {
                    this[index] += member
                }
            }
        }

        operator fun List<CommonizedMemberScope?>.plusAssign(members: List<DeclarationDescriptor?>) {
            members.forEachIndexed { index, member ->
                if (member != null) {
                    this[index]?.run { this += member }
                }
            }
        }
    }
}

private inline fun <reified T : DeclarationDescriptor> List<*>.filteredBy(name: Name): Sequence<T> =
    this.asSequence().filterIsInstance<T>().filter { it.name == name }
