package org.jetbrains.kotlin.commonizer.transformer

import org.jetbrains.kotlin.commonizer.TargetDependent
import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.mergedtree.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class InlineTypeAliasCirNodeTransformerTest {

    private val storageManager = LockBasedStorageManager("test")

    private val classifiers = CirKnownClassifiers(
        classifierIndices = TargetDependent.empty(),
        targetDependencies = TargetDependent.empty(),
        commonizedNodes = CirCommonizedClassifierNodes.default(),
        commonDependencies = CirProvidedClassifiers.EMPTY
    )

    @Test
    fun `test artificial supertypes - regular package`() {
        val root = setup(CirEntityId.create("regular/package/__X"))
        InlineTypeAliasCirNodeTransformer(storageManager, classifiers).invoke(root)
        val artificialXClass = root.assertInlinedXClass
        assertEquals(
            emptyList(), artificialXClass.supertypes,
            "Expected no artificial supertype for artificial class X"
        )
    }

    @Test
    fun `test artificial supertypes - cnames structs`() {
        val root = setup(CirEntityId.create("cnames/structs/__X"))
        InlineTypeAliasCirNodeTransformer(storageManager, classifiers).invoke(root)
        val artificialXClass = root.assertInlinedXClass

        assertEquals(
            setOf(CirEntityId.create("kotlinx/cinterop/COpaque")),
            artificialXClass.supertypes.map { it as CirClassType }.map { it.classifierId }.toSet(),
            "Expected 'COpaque' supertype being attached automatically"
        )
    }

    @Test
    fun `test artificial supertypes - objcnames classes`() {
        val root = setup(CirEntityId.create("objcnames/classes/__X"))
        InlineTypeAliasCirNodeTransformer(storageManager, classifiers).invoke(root)
        val artificialXClass = root.assertInlinedXClass

        assertEquals(
            setOf(CirEntityId.create("kotlinx/cinterop/ObjCObjectBase")),
            artificialXClass.supertypes.map { it as CirClassType }.map { it.classifierId }.toSet(),
            "Expected 'ObjCObjectBase' supertype being attached automatically"
        )
    }

    @Test
    fun `test artificial supertypes - objcnames protocols`() {
        val root = setup(CirEntityId.create("objcnames/protocols/__X"))
        InlineTypeAliasCirNodeTransformer(storageManager, classifiers).invoke(root)
        val artificialXClass = root.assertInlinedXClass

        assertEquals(
            setOf(CirEntityId.create("kotlinx/cinterop/ObjCObject")),
            artificialXClass.supertypes.map { it as CirClassType }.map { it.classifierId }.toSet(),
            "Expected 'ObjCObject' supertype being attached automatically"
        )
    }

    private fun setup(typeAliasPointingTo: CirEntityId): CirRootNode {
        val root = buildRootNode(storageManager, CirProvidedClassifiers.EMPTY, 1)
        root.modules[CirName.create("test-module")] = buildModuleNode(storageManager, 1).apply {
            packages[CirPackageName.create("under.test")] = buildPackageNode(storageManager, 2).apply {

                typeAliases[CirName.create("X")] = buildTypeAliasNode(
                    storageManager, 2, classifiers, CirEntityId.create("under/test/X")
                ).apply {
                    val underlyingType = CirClassType.createInterned(
                        classId = typeAliasPointingTo,
                        outerType = null, visibility = Visibilities.Public,
                        arguments = emptyList(), isMarkedNullable = false
                    )
                    targetDeclarations[0] = CirTypeAlias.create(
                        annotations = emptyList(),
                        name = CirName.create("X"),
                        typeParameters = emptyList(),
                        visibility = Visibilities.Public,
                        underlyingType = underlyingType,
                        expandedType = underlyingType
                    )
                }

                classes[CirName.create("X")] = buildClassNode(
                    storageManager, 2, classifiers, null, CirEntityId.create("under/test/X")
                ).apply {
                    targetDeclarations[1] = CirClass.create(
                        name = CirName.create("X"), typeParameters = emptyList(),
                        supertypes = emptyList(), visibility = Visibilities.Public,
                        companion = null, isCompanion = false, isData = false, isExternal = false,
                        isInner = false, isValue = false, kind = ClassKind.CLASS,
                        modality = Modality.FINAL, annotations = emptyList(),
                    )
                }
            }
        }

        return root
    }

    private val CirRootNode.xClassNode: CirClassNode
        get() = modules.getValue(CirName.create("test-module"))
            .packages.getValue(CirPackageName.create("under.test"))
            .classes.getValue(CirName.create("X"))

    private val CirRootNode.assertInlinedXClass: CirClass
        get() = assertNotNull(xClassNode.targetDeclarations[0], "Missing inlined class 'X' at index 0")

}