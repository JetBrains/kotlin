/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.commonizer.*
import org.jetbrains.kotlin.descriptors.commonizer.ResultsConsumer.ModuleResult
import org.jetbrains.kotlin.descriptors.commonizer.ModulesProvider.ModuleInfo
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirClassFactory
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.*
import org.jetbrains.kotlin.descriptors.impl.AbstractTypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.types.*
import java.io.File
import kotlin.random.Random
import org.jetbrains.kotlin.storage.getValue

// expected special name for module
internal fun mockEmptyModule(moduleName: String): ModuleDescriptor {
    val module = KotlinTestUtils.createEmptyModule(moduleName)
    module.initialize(PackageFragmentProvider.Empty)
    return module
}

internal fun mockClassType(
    fqName: String,
    nullable: Boolean = false
): KotlinType = LazyWrappedType(LockBasedStorageManager.NO_LOCKS) {
    val classFqName = FqName(fqName)

    val classDescriptor = ClassDescriptorImpl(
        /* containingDeclaration = */ createPackageFragmentForClassifier(classFqName),
        /* name = */ classFqName.shortName(),
        /* modality = */ Modality.FINAL,
        /* classKind = */ ClassKind.CLASS,
        /* supertypes = */ emptyList(),
        /* source = */ SourceElement.NO_SOURCE,
        /* isExternal = */ false,
        /* storageManager = */ LockBasedStorageManager.NO_LOCKS
    )

    classDescriptor.initialize(
        /* unsubstitutedMemberScope = */ MemberScope.Empty,
        /* constructors = */ emptySet(),
        /* primaryConstructor = */ null
    )

    classDescriptor.defaultType.makeNullableAsSpecified(nullable)
}

internal fun mockTAType(
    fqName: String,
    nullable: Boolean = false,
    rightHandSideTypeProvider: () -> KotlinType
): KotlinType = LazyWrappedType(LockBasedStorageManager.NO_LOCKS) {
    val typeAliasFqName = FqName(fqName)

    val rightHandSideType = rightHandSideTypeProvider().lowerIfFlexible()

    val typeAliasDescriptor = object : AbstractTypeAliasDescriptor(
        containingDeclaration = createPackageFragmentForClassifier(typeAliasFqName),
        annotations = Annotations.EMPTY,
        name = typeAliasFqName.shortName(),
        sourceElement = SourceElement.NO_SOURCE,
        visibilityImpl = DescriptorVisibilities.PUBLIC
    ) {
        override val storageManager get() = LockBasedStorageManager.NO_LOCKS

        private val defaultTypeImpl = storageManager.createLazyValue { computeDefaultType() }
        override fun getDefaultType() = defaultTypeImpl()

        override val underlyingType by storageManager.createLazyValue { rightHandSideType.getAbbreviation() ?: rightHandSideType }
        override val expandedType by storageManager.createLazyValue { rightHandSideType }

        override val classDescriptor get() = expandedType.constructor.declarationDescriptor as? ClassDescriptor
        override val constructors by storageManager.createLazyValue { getTypeAliasConstructors() }

        private val typeConstructorTypeParametersImpl by storageManager.createLazyValue { computeConstructorTypeParameters() }
        override fun getTypeConstructorTypeParameters() = typeConstructorTypeParametersImpl

        override fun substitute(substitutor: TypeSubstitutor) = error("Unsupported")
    }

    typeAliasDescriptor.initialize(declaredTypeParameters = emptyList())

    (rightHandSideType.getAbbreviatedType()?.expandedType ?: rightHandSideType)
        .withAbbreviation(typeAliasDescriptor.defaultType)
        .makeNullableAsSpecified(nullable)
}

private fun createPackageFragmentForClassifier(classifierFqName: FqName): PackageFragmentDescriptor =
    object : PackageFragmentDescriptor {
        private val module: ModuleDescriptor by lazy { mockEmptyModule("<module4_${classifierFqName.shortName()}_x${Random.nextInt()}>") }
        override fun getContainingDeclaration(): ModuleDescriptor = module
        override val fqName = classifierFqName.parentOrNull() ?: FqName.ROOT
        override fun getMemberScope() = MemberScope.Empty
        override fun getOriginal() = this
        override fun getName() = fqName.shortNameOrSpecial()
        override fun getSource() = SourceElement.NO_SOURCE
        override val annotations = Annotations.EMPTY
        override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D): R = error("not supported")
        override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) = error("not supported")
        override fun toString() = "package $name"
    }

internal val MOCK_CLASSIFIERS = CirKnownClassifiers(
    commonized = object : CirCommonizedClassifiers {
        private val MOCK_CLASS_NODE = CirClassNode(
            CommonizedGroup(0),
            LockBasedStorageManager.NO_LOCKS.createNullableLazyValue {
                CirClassFactory.create(
                    annotations = emptyList(),
                    name = Name.identifier("kotlin.Any"),
                    typeParameters = emptyList(),
                    visibility = DescriptorVisibilities.PUBLIC,
                    modality = Modality.OPEN,
                    kind = ClassKind.CLASS,
                    companion = null,
                    isCompanion = false,
                    isData = false,
                    isInline = false,
                    isInner = false,
                    isExternal = false
                )
            },
            ClassId.fromString("kotlin/Any")
        )

        override fun classNode(classId: ClassId) = MOCK_CLASS_NODE
        override fun typeAliasNode(typeAliasId: ClassId) = error("This method should not be called")
        override fun addClassNode(classId: ClassId, node: CirClassNode) = error("This method should not be called")
        override fun addTypeAliasNode(typeAliasId: ClassId, node: CirTypeAliasNode) = error("This method should not be called")
    },
    forwardDeclarations = object : CirForwardDeclarations {
        override fun isExportedForwardDeclaration(classId: ClassId) = false
        override fun addExportedForwardDeclaration(classId: ClassId) = error("This method should not be called")
    },
    dependeeLibraries = emptyMap()
)

internal class MockModulesProvider private constructor(
    private val modules: Map<String, ModuleDescriptor>,
) : ModulesProvider {
    private val moduleInfos: Map<String, ModuleInfo> = modules.mapValues { (name, _) -> fakeModuleInfo(name) }

    override fun loadModuleInfos() = moduleInfos
    override fun loadModules(dependencies: Collection<ModuleDescriptor>): Map<String, ModuleDescriptor> = modules

    private fun fakeModuleInfo(name: String) = ModuleInfo(name, File("/tmp/commonizer/mocks/$name"), null)

    companion object {
        @JvmName("createByModuleNames")
        fun create(moduleNames: List<String>) = MockModulesProvider(
            moduleNames.associateWith { name -> mockEmptyModule("<$name>") }
        )

        @JvmName("createByModules")
        fun create(modules: List<ModuleDescriptor>) = MockModulesProvider(
            modules.associateBy { module -> module.name.strip() }
        )

        @JvmName("createBySingleModule")
        fun create(module: ModuleDescriptor) = MockModulesProvider(
            mapOf(module.name.strip() to module)
        )
    }
}

internal class MockResultsConsumer : ResultsConsumer {
    private val _modulesByTargets = LinkedHashMap<CommonizerTarget, Collection<ModuleResult>>() // use linked hash map to preserve order
    val modulesByTargets: Map<CommonizerTarget, Collection<ModuleResult>>
        get() = _modulesByTargets

    val sharedTarget: SharedTarget by lazy { modulesByTargets.keys.filterIsInstance<SharedTarget>().single() }
    val leafTargets: Set<LeafTarget> by lazy { modulesByTargets.keys.filterIsInstance<LeafTarget>().toSet() }

    lateinit var status: ResultsConsumer.Status

    override fun consumeResults(target: CommonizerTarget, moduleResults: Collection<ModuleResult>) {
        val oldValue = _modulesByTargets.put(target, moduleResults)
        check(oldValue == null)
    }

    override fun successfullyFinished(status: ResultsConsumer.Status) {
        this.status = status
    }
}
