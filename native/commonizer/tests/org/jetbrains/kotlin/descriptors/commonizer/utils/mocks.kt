/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.commonizer.BuiltInsProvider
import org.jetbrains.kotlin.descriptors.commonizer.LeafTarget
import org.jetbrains.kotlin.descriptors.commonizer.ModulesProvider
import org.jetbrains.kotlin.descriptors.commonizer.ModulesProvider.ModuleInfo
import org.jetbrains.kotlin.descriptors.commonizer.builder.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirClassFactory
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.*
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

    val targetComponents = TargetDeclarationsBuilderComponents(
        storageManager = LockBasedStorageManager.NO_LOCKS,
        target = LeafTarget("Arbitrary target"),
        builtIns = DefaultBuiltIns.Instance,
        lazyClassifierLookupTable = LockBasedStorageManager.NO_LOCKS.createLazyValue { LazyClassifierLookupTable(emptyMap()) },
        index = 0,
        cache = DeclarationsBuilderCache(1)
    )

    val classDescriptor = CommonizedClassDescriptor(
        targetComponents = targetComponents,
        containingDeclaration = createPackageFragmentForClassifier(classFqName),
        annotations = Annotations.EMPTY,
        name = classFqName.shortName(),
        kind = ClassKind.CLASS,
        modality = Modality.FINAL,
        visibility = DescriptorVisibilities.PUBLIC,
        isCompanion = false,
        isData = false,
        isInline = false,
        isInner = false,
        isExternal = false,
        isExpect = false,
        isActual = false,
        cirDeclaredTypeParameters = emptyList(),
        companionObjectName = null,
        cirSupertypes = emptyList()
    )

    classDescriptor.unsubstitutedMemberScope = CommonizedMemberScope()

    classDescriptor.initialize(constructors = emptyList())

    classDescriptor.defaultType.makeNullableAsSpecified(nullable)
}

internal fun mockTAType(
    fqName: String,
    nullable: Boolean = false,
    rightHandSideTypeProvider: () -> KotlinType
): KotlinType = LazyWrappedType(LockBasedStorageManager.NO_LOCKS) {
    val typeAliasFqName = FqName(fqName)

    val rightHandSideType = rightHandSideTypeProvider().lowerIfFlexible()

    val typeAliasDescriptor = CommonizedTypeAliasDescriptor(
        storageManager = LockBasedStorageManager.NO_LOCKS,
        containingDeclaration = createPackageFragmentForClassifier(typeAliasFqName),
        annotations = Annotations.EMPTY,
        name = typeAliasFqName.shortName(),
        visibility = DescriptorVisibilities.PUBLIC,
        isActual = false
    )

    typeAliasDescriptor.initialize(
        declaredTypeParameters = emptyList(),
        underlyingType = LockBasedStorageManager.NO_LOCKS.createLazyValue { rightHandSideType.getAbbreviation() ?: rightHandSideType },
        expandedType = LockBasedStorageManager.NO_LOCKS.createLazyValue { rightHandSideType }
    )

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

internal class MockBuiltInsProvider(private val builtIns: KotlinBuiltIns) : BuiltInsProvider {
    override fun loadBuiltIns() = builtIns
}

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
