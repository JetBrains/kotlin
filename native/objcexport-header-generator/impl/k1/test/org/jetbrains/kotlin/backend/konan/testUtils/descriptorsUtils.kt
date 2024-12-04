package org.jetbrains.kotlin.backend.konan.testUtils

import org.jetbrains.kotlin.backend.konan.objcexport.getErasedTypeClass
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction

internal fun ModuleDescriptor.getTopLevelFunExtensionType(name: String): ClassDescriptor {
    return getTopLevelFun(name).extensionReceiverParameter?.type?.getErasedTypeClass()
        ?: error("Unable to find top level function $name")
}

internal fun ClassDescriptor.getMemberFun(name: String): FunctionDescriptor {
    return unsubstitutedMemberScope.findSingleFunction(Name.identifier(name))
}

internal fun ClassDescriptor.getMemberProperty(name: String): PropertyDescriptor {
    return unsubstitutedMemberScope.getContributedVariables(Name.identifier(name), NoLookupLocation.FROM_BACKEND).singleOrNull()
        ?: error("Unable to find property $name")
}

internal fun ModuleDescriptor.getTopLevelFun(name: String): FunctionDescriptor {
    return getPackage(FqName.ROOT).memberScope.findSingleFunction(Name.identifier(name))
}

internal fun ModuleDescriptor.getTopLevelProp(name: String): PropertyDescriptor {
    return getPackage(FqName.ROOT).memberScope.getContributedVariables(Name.identifier(name), NoLookupLocation.FROM_BACKEND).singleOrNull()
        ?: error("Unable to find property $name")
}

internal fun ModuleDescriptor.getClass(name: String): ClassDescriptor {
    return findClassAcrossModuleDependencies(ClassId.fromString(name)) ?: error("Failed to find `$name` class")
}