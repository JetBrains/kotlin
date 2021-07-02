/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.scopes

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class ExplicitImportsScope(private val descriptors: Collection<DeclarationDescriptor>) : BaseImportingScope(null) {
    override fun getContributedClassifier(name: Name, location: LookupLocation) =
        descriptors.filter { it.name == name }.firstIsInstanceOrNull<ClassifierDescriptor>()

    override fun getContributedPackage(name: Name) = descriptors.filter { it.name == name }.firstIsInstanceOrNull<PackageViewDescriptor>()

    override fun getContributedVariables(name: Name, location: LookupLocation) =
        descriptors.filter { it.name == name }.filterIsInstance<VariableDescriptor>()

    override fun getContributedFunctions(name: Name, location: LookupLocation) =
        descriptors.filter { it.name == name }.filterIsInstance<FunctionDescriptor>()

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        changeNamesForAliased: Boolean
    ) = descriptors

    override fun computeImportedNames() = descriptors.mapTo(hashSetOf()) { it.name }

    override fun printStructure(p: Printer) {
        p.println(this::class.java.name)
    }
}
