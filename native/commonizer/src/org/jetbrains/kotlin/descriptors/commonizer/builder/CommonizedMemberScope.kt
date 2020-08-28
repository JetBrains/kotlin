/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import gnu.trove.THashMap
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.utils.Printer

class CommonizedMemberScope : MemberScope {
    private val functions = THashMap<Name, MutableList<SimpleFunctionDescriptor>>()
    private val variables = THashMap<Name, MutableList<PropertyDescriptor>>()
    private val classifiers = THashMap<Name, ClassifierDescriptorWithTypeParameters>()

    private fun addMember(member: DeclarationDescriptor) {
        when (member) {
            is SimpleFunctionDescriptor -> functions.getOrPut(member.name) { ArrayList(INITIAL_CAPACITY_FOR_CALLABLES) } += member
            is PropertyDescriptor -> variables.getOrPut(member.name) { ArrayList(INITIAL_CAPACITY_FOR_CALLABLES) } += member
            is ClassifierDescriptorWithTypeParameters -> classifiers[member.name] = member
            else -> error("Unsupported member type: ${member::class.java}, $member")
        }
    }

    override fun getFunctionNames(): Set<Name> = functions.keys
    override fun getVariableNames(): Set<Name> = variables.keys
    override fun getClassifierNames(): Set<Name> = classifiers.keys

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> =
        functions[name].orEmpty()

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> =
        variables[name].orEmpty()

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
        classifiers[name]

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        val result = ArrayList<DeclarationDescriptor>(INITIAL_CAPACITY_FOR_CONTRIBUTED_DESCRIPTORS)

        if (kindFilter.acceptsKinds(DescriptorKindFilter.CLASSIFIERS_MASK)) {
            classifiers.values.filterTo(result) { nameFilter(it.name) }
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
            functions.forEach { (name, callables) ->
                if (nameFilter(name))
                    result.addAll(callables)
            }
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)) {
            variables.forEach { (name, callables) ->
                if (nameFilter(name))
                    result.addAll(callables)
            }
        }

        return result
    }

    override fun printScopeStructure(p: Printer) {
        p.println(this::class.java.simpleName, " {")
        p.pushIndent()

        p.println("functions = $functions")
        p.println("variables = $variables")
        p.println("classifiers = $classifiers")

        p.popIndent()
        p.println("}")
    }

    companion object {
        fun createArray(size: Int) = Array(size) { CommonizedMemberScope() }

        operator fun Array<CommonizedMemberScope>.plusAssign(members: List<DeclarationDescriptor?>) {
            members.forEachIndexed { index, member ->
                if (member != null)
                    this[index].addMember(member)
            }
        }

        operator fun List<CommonizedMemberScope?>.plusAssign(members: List<DeclarationDescriptor?>) {
            members.forEachIndexed { index, member ->
                if (member != null)
                    this[index]?.addMember(member)
            }
        }

        // Heuristic memory usage optimization. During commonization of ios_x64 and ios_arm64 only about 3% of functions are overloaded
        // (and therefore have the same name in the same scope). For the remaining 97% the capacity of 1 is enough.
        private const val INITIAL_CAPACITY_FOR_CALLABLES = 1

        // Heuristic memory usage optimization. During commonization of ios_x64 and ios_arm64 the getContributedDescriptors() call
        // returns empty list in 27% times and list of size 1 in 63% times. The default capacity of 0 looks reasonable.
        private const val INITIAL_CAPACITY_FOR_CONTRIBUTED_DESCRIPTORS = 0
    }
}
