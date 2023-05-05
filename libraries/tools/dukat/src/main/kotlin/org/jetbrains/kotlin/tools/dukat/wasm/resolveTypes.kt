package org.jetbrains.kotlin.tools.dukat.wasm

import org.jetbrains.dukat.idlDeclarations.*
import org.jetbrains.dukat.idlLowerings.IDLLowering
import org.jetbrains.dukat.idlLowerings.containsType
import org.jetbrains.dukat.logger.Logging
import org.jetbrains.dukat.panic.raiseConcern

private class TypeResolver : IDLLowering {

    private val logger: Logging = Logging("resolveTypes")

    private val resolvedUnionTypes: MutableSet<String> = mutableSetOf()
    private val failedToResolveUnionTypes: MutableSet<String> = mutableSetOf()

    private val namedDeclarationsToAdd: MutableMap<String, MutableSet<IDLUnionDeclaration>> = mutableMapOf()
    private val anonymousDeclarationsToAdd: MutableSet<IDLUnionDeclaration> = mutableSetOf()

    private val dependenciesToAdd: MutableMap<String, MutableSet<IDLSingleTypeDeclaration>> = mutableMapOf()

    private var sourceSet: IDLSourceSetDeclaration = IDLSourceSetDeclaration(listOf())
    private var currentFile: IDLFileDeclaration = IDLFileDeclaration("", listOf(), listOf(), null)

    fun getDependencies(declaration: IDLClassLikeDeclaration): List<IDLSingleTypeDeclaration> {
        return dependenciesToAdd[declaration.name]?.toList() ?: listOf()
    }

    fun getNamedDeclarationsToAdd(fileName: String): List<IDLUnionDeclaration> {
        return namedDeclarationsToAdd[fileName]?.toList() ?: listOf()
    }

    private fun processUnionType(unionType: IDLUnionTypeDeclaration) {
        val newDependenciesToAdd: MutableMap<String, MutableSet<IDLSingleTypeDeclaration>> = mutableMapOf()
        for (member in unionType.unionMembers) {
            when (member) {
                is IDLUnionTypeDeclaration -> {
                    if (member.name !in resolvedUnionTypes && member.name !in failedToResolveUnionTypes) {
                        processUnionType(member)
                    }
                    if (member.name in failedToResolveUnionTypes) {
                        failedToResolveUnionTypes += unionType.name
                        return
                    }
                    newDependenciesToAdd.putIfAbsent(member.name, mutableSetOf())
                    newDependenciesToAdd[member.name]!!.add(IDLSingleTypeDeclaration(
                            name = unionType.name,
                            typeParameter = null,
                            nullable = false
                        ))
                }
                is IDLSingleTypeDeclaration -> {
                    if (member.typeParameter != null || !sourceSet.containsType(member.name)) {
                        failedToResolveUnionTypes += unionType.name
                        return
                    }
                    newDependenciesToAdd.putIfAbsent(member.name, mutableSetOf())
                    newDependenciesToAdd[member.name]!!.add(IDLSingleTypeDeclaration(
                            name = unionType.name,
                            typeParameter = null,
                            nullable = false
                    ))
                }
                is IDLFunctionTypeDeclaration -> {
                    failedToResolveUnionTypes += unionType.name
                    return
                }
            }
        }
        resolvedUnionTypes += unionType.name
        if (unionType.originFile != null) {
            namedDeclarationsToAdd.putIfAbsent(unionType.originFile!!, mutableSetOf())
            namedDeclarationsToAdd[unionType.originFile!!]!!.add(IDLUnionDeclaration(
                name = unionType.name,
                unions = listOf()
            ))
        } else {
            anonymousDeclarationsToAdd.add(IDLUnionDeclaration(
                name = unionType.name,
                unions = listOf()
            ))
        }
        for ((interfaceName, dependencies) in newDependenciesToAdd) {
            dependenciesToAdd.putIfAbsent(interfaceName, mutableSetOf())
            dependenciesToAdd[interfaceName]!!.addAll(dependencies)
        }
    }

    override fun lowerTypeDeclaration(declaration: IDLTypeDeclaration, owner: IDLFileDeclaration): IDLTypeDeclaration {
        if (declaration is IDLUnionTypeDeclaration) {
            if (declaration.name !in resolvedUnionTypes && declaration.name !in failedToResolveUnionTypes) {
                processUnionType(declaration)
                declaration.unionMembers.forEach { lowerTypeDeclaration(it, owner) }
            }
            return when (declaration.name) {
                in resolvedUnionTypes -> IDLSingleTypeDeclaration(
                        name = declaration.name,
                        typeParameter = null,
                        nullable = declaration.nullable,
                        comment = declaration.comment
                )
                // Keeping unresolved union types as-is to process later
                in failedToResolveUnionTypes -> declaration
                else -> raiseConcern("unprocessed UnionTypeDeclaration: $this") { declaration }
            }
        }
        if (declaration is IDLSingleTypeDeclaration) {
            return if (!declaration.isKnown() && !sourceSet.containsType(declaration.name)) {
                logger.warn("Unknown type ${declaration.name} in file ${currentFile.fileName}")
                IDLSingleTypeDeclaration(
                        name = "\$dynamic",
                        typeParameter = null,
                        nullable = false,
                        comment = declaration.comment
                )
            } else {
                declaration.copy(typeParameter = declaration.typeParameter?.let { lowerTypeDeclaration(it, owner) })
            }
        }
        if (declaration is IDLFunctionTypeDeclaration) {
            return declaration.copy(
                    returnType = lowerTypeDeclaration(declaration.returnType, owner),
                    arguments = declaration.arguments.map { lowerArgumentDeclaration(it, owner) }
            )
        }
        return declaration
    }

    private fun resolveInheritance(inheritance: IDLSingleTypeDeclaration): IDLSingleTypeDeclaration? {
        return if (sourceSet.containsType(inheritance.name)) {
            inheritance
        } else {
            null
        }
    }

    override fun lowerInterfaceDeclaration(declaration: IDLInterfaceDeclaration, owner: IDLFileDeclaration): IDLInterfaceDeclaration {
        val newDeclaration = super.lowerInterfaceDeclaration(declaration, owner)
        return newDeclaration.copy(
                parents = declaration.parents.mapNotNull { resolveInheritance(it) }
        )
    }

    override fun lowerDictionaryDeclaration(declaration: IDLDictionaryDeclaration, owner: IDLFileDeclaration): IDLDictionaryDeclaration {
        val newDeclaration = super.lowerDictionaryDeclaration(declaration, owner)
        return newDeclaration.copy(
                parents = declaration.parents.mapNotNull { resolveInheritance(it) }
        )
    }

    override fun lowerFileDeclaration(fileDeclaration: IDLFileDeclaration): IDLFileDeclaration {
        currentFile = fileDeclaration
        var newFileDeclaration = super.lowerFileDeclaration(fileDeclaration)
        newFileDeclaration = newFileDeclaration.copy(
                declarations = newFileDeclaration.declarations + anonymousDeclarationsToAdd
        )
        anonymousDeclarationsToAdd.clear()
        return newFileDeclaration
    }

    override fun lowerSourceSetDeclaration(sourceSet: IDLSourceSetDeclaration): IDLSourceSetDeclaration {
        this.sourceSet = sourceSet
        return super.lowerSourceSetDeclaration(sourceSet)
    }
}

private class DeclarationAdder(val typeResolver: TypeResolver) : IDLLowering {
    override fun lowerFileDeclaration(fileDeclaration: IDLFileDeclaration): IDLFileDeclaration {
        return fileDeclaration.copy(
            declarations = fileDeclaration.declarations +
                    typeResolver.getNamedDeclarationsToAdd(fileDeclaration.fileName)
        )
    }
}

private class DependencyResolver(val typeResolver: TypeResolver) : IDLLowering {

    override fun lowerInterfaceDeclaration(declaration: IDLInterfaceDeclaration, owner: IDLFileDeclaration): IDLInterfaceDeclaration {
        return declaration.copy(
                unions = declaration.unions + typeResolver.getDependencies(declaration)
        )
    }

    override fun lowerDictionaryDeclaration(declaration: IDLDictionaryDeclaration, owner: IDLFileDeclaration): IDLDictionaryDeclaration {
        return declaration.copy(
            unions = declaration.unions + typeResolver.getDependencies(declaration)
        )
    }

    override fun lowerEnumDeclaration(declaration: IDLEnumDeclaration, owner: IDLFileDeclaration): IDLEnumDeclaration {
        return declaration.copy(
            unions = declaration.unions + typeResolver.getDependencies(declaration)
        )
    }

    override fun lowerUnionDeclaration(declaration: IDLUnionDeclaration, owner: IDLFileDeclaration): IDLUnionDeclaration {
        return declaration.copy(
            unions = declaration.unions + typeResolver.getDependencies(declaration)
        )
    }
}

fun IDLSourceSetDeclaration.resolveTypesKeepingUnions(): IDLSourceSetDeclaration {
    val typeResolver = TypeResolver()
    val dependencyResolver = DependencyResolver(typeResolver)
    val declarationAdder = DeclarationAdder(typeResolver)
    return dependencyResolver.lowerSourceSetDeclaration(
        declarationAdder.lowerSourceSetDeclaration(
            typeResolver.lowerSourceSetDeclaration(this)
        )
    )
}

