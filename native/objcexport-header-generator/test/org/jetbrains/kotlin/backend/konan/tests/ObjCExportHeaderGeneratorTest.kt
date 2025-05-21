/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.tests

import org.jetbrains.kotlin.backend.konan.testUtils.HeaderGenerator
import org.jetbrains.kotlin.backend.konan.testUtils.HeaderGenerator.Configuration
import org.jetbrains.kotlin.backend.konan.testUtils.TodoAnalysisApi
import org.jetbrains.kotlin.backend.konan.testUtils.headersTestDataDir
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.fail

/**
 * ## Test Scope
 * This test will cover the generation of 'ObjC' headers.
 *
 * The input of the test are Kotlin source files;
 * The output is the generated header files;
 * The output will be compared to already checked in golden files.
 *
 * ## How to create a new test
 * Every test has a corresponding 'root' directory.
 * All directories are found in `native/objcexport-header-generator/testData/headers`.
 *
 * 1) Create new root directory (e.g. myTest) in testData/headers
 * 2) Place kotlin files into the directory e.g. testData/headers/myTest/Foo.kt
 * 3) Create a test function and call ` doTest(headersTestDataDir.resolve("myTest"))`
 * 4) The first invocation will fail the test, but generates the header that can be checked in (if sufficient)
 */
class ObjCExportHeaderGeneratorTest(private val generator: HeaderGenerator) {

    @Test
    fun `test - simpleClass`() {
        doTest(headersTestDataDir.resolve("simpleClass"))
    }

    @Test
    fun `test - simpleInterface`() {
        doTest(headersTestDataDir.resolve("simpleInterface"))
    }

    @Test
    fun `test - simpleEnumClass`() {
        doTest(headersTestDataDir.resolve("simpleEnumClass"))
    }

    @Test
    fun `test - simpleObject`() {
        doTest(headersTestDataDir.resolve("simpleObject"))
    }

    @Test
    fun `test - topLevelFunction`() {
        doTest(headersTestDataDir.resolve("topLevelFunction"))
    }

    @Test
    fun `test - topLevelProperty`() {
        doTest(headersTestDataDir.resolve("topLevelProperty"))
    }

    @Test
    @TodoAnalysisApi
    fun `test - sameClassNameInDifferentPackage`() {
        doTest(headersTestDataDir.resolve("sameClassNameInDifferentPackage"))
    }

    @Test
    fun `test - sameFunctionNameInDifferentClass`() {
        doTest(headersTestDataDir.resolve("sameFunctionNameInDifferentClass"))
    }

    @Test
    @TodoAnalysisApi
    fun `test - sameFunctionNameInDifferentInterface`() {
        doTest(headersTestDataDir.resolve("sameFunctionNameInDifferentInterface"))
    }

    @Test
    fun `test - nestedClass`() {
        doTest(headersTestDataDir.resolve("nestedClass"))
    }

    @Test
    fun `test - nestedClassWithFrameworkName`() {
        doTest(headersTestDataDir.resolve("nestedClassWithFrameworkName"), Configuration(frameworkName = "Shared"))
    }

    @Test
    fun `test - nestedGenericClass`() {
        doTest(headersTestDataDir.resolve("nestedGenericClass"))
    }

    @Test
    fun `test - nestedInterface`() {
        doTest(headersTestDataDir.resolve("nestedInterface"))
    }

    @Test
    fun `test - samePropertyAndFunctionName`() {
        doTest(headersTestDataDir.resolve("samePropertyAndFunctionName"))
    }

    @Test
    fun `test - classImplementingInterface`() {
        doTest(headersTestDataDir.resolve("classImplementingInterface"))
    }

    @Test
    fun `test - classExtendsAbstractClass`() {
        doTest(headersTestDataDir.resolve("classExtendsAbstractClass"))
    }

    @Test
    fun `test - interfaceImplementingInterface`() {
        doTest(headersTestDataDir.resolve("interfaceImplementingInterface"))
    }

    @Test
    fun `test - multipleInterfacesImplementationChain`() {
        doTest(headersTestDataDir.resolve("multipleInterfacesImplementationChain"))
    }

    @Test
    fun `test - classWithObjCNameAnnotation`() {
        doTest(headersTestDataDir.resolve("classWithObjCNameAnnotation"))
    }

    @Test
    fun `test - functionWithObjCNameAnnotation`() {
        doTest(headersTestDataDir.resolve("functionWithObjCNameAnnotation"))
    }

    @Test
    fun `test - propertyWithObjCNameAnnotation`() {
        doTest(headersTestDataDir.resolve("propertyWithObjCNameAnnotation"))
    }

    @Test
    fun `test - classWithKDoc`() {
        doTest(headersTestDataDir.resolve("classWithKDoc"))
    }

    @Test
    fun `test  - classWithHidesFromObjCAnnotation`() {
        doTest(headersTestDataDir.resolve("classWithHidesFromObjCAnnotation"))
    }

    /**
     * Disabled because of init constructors order KT-70626
     */
    @Test
    @TodoAnalysisApi
    fun `test - functionWithThrowsAnnotation`() {
        doTest(headersTestDataDir.resolve("functionWithThrowsAnnotation"))
    }

    @Test
    fun `test - functionWithErrorType`() {
        doTest(headersTestDataDir.resolve("functionWithErrorType"))
    }

    @Test
    fun `test - functionWithErrorTypeAndFrameworkName`() {
        doTest(headersTestDataDir.resolve("functionWithErrorTypeAndFrameworkName"), Configuration(frameworkName = "shared"))
    }

    @Test
    fun `test - kdocWithBlockTags`() {
        doTest(headersTestDataDir.resolve("kdocWithBlockTags"))
    }

    @Test
    fun `test - classWithMustBeDocumentedAnnotation`() {
        doTest(headersTestDataDir.resolve("classWithMustBeDocumentedAnnotation"))
    }

    @Test
    fun `test - interfaceWithMustBeDocumentedAnnotation`() {
        doTest(headersTestDataDir.resolve("interfaceWithMustBeDocumentedAnnotation"))
    }

    @Test
    fun `test - functionWithMustBeDocumentedAnnotation`() {
        doTest(headersTestDataDir.resolve("functionWithMustBeDocumentedAnnotation"))
    }

    @Test
    fun `test - parameterWithMustBeDocumentedAnnotation`() {
        doTest(headersTestDataDir.resolve("parameterWithMustBeDocumentedAnnotation"))
    }

    @Test
    @TodoAnalysisApi
    fun `test - receiverWithMustBeDocumentedAnnotation`() {
        doTest(headersTestDataDir.resolve("receiverWithMustBeDocumentedAnnotation"))
    }

    @Test
    @TodoAnalysisApi
    fun `test - dispatchAndExtensionReceiverWithMustBeDocumentedAnnotation`() {
        doTest(headersTestDataDir.resolve("dispatchAndExtensionReceiverWithMustBeDocumentedAnnotation"))
    }

    @Test
    fun `test - classWithUnresolvedSuperType`() {
        doTest(headersTestDataDir.resolve("classWithUnresolvedSuperType"))
    }

    @Test
    fun `test - classWithUnresolvedSuperTypeGenerics`() {
        doTest(headersTestDataDir.resolve("classWithUnresolvedSuperTypeGenerics"))
    }

    @Test
    fun `test - topLevelFunctionWithNumberReturn`() {
        doTest(headersTestDataDir.resolve("topLevelFunctionWithNumberReturn"))
    }

    @Test
    fun `test - classWithManyMembers`() {
        doTest(headersTestDataDir.resolve("classWithManyMembers"))
    }

    @Test
    fun `test - manyClassesAndInterfaces`() {
        doTest(headersTestDataDir.resolve("manyClassesAndInterfaces"))
    }

    @Test
    fun `test - classReferencingOtherClassAsReturnType`() {
        doTest(headersTestDataDir.resolve("classReferencingOtherClassAsReturnType"))
    }

    @Test
    fun `test - classReferencingDependencyClassAsReturnType`() {
        doTest(headersTestDataDir.resolve("classReferencingDependencyClassAsReturnType"))
    }

    @Test
    fun `test - interfaceReferencingOtherInterfaceAsReturnType`() {
        doTest(headersTestDataDir.resolve("interfaceReferencingOtherInterfaceAsReturnType"))
    }

    @Test
    fun `test - interfaceImplementingInterfaceOrder`() {
        doTest(headersTestDataDir.resolve("interfaceImplementingInterfaceOrder"))
    }

    @Test
    fun `test - extensionFunctions`() {
        doTest(headersTestDataDir.resolve("extensionFunctions"))
    }

    @Test
    fun `test - extensionProperties`() {
        doTest(headersTestDataDir.resolve("extensionProperties"))
    }


    @Test
    fun `test - classWithGenerics`() {
        doTest(headersTestDataDir.resolve("classWithGenerics"))
    }

    @Test
    fun `test - objectWithGenericSuperclass`() {
        doTest(headersTestDataDir.resolve("objectWithGenericSuperclass"))
    }

    @Test
    fun `test - since version annotation`() {
        doTest(headersTestDataDir.resolve("sinceVersionAnnotation"))
    }

    @Test
    fun `test - constructors`() {
        doTest(headersTestDataDir.resolve("constructors"))
    }

    @Test
    fun `test - companion`() {
        doTest(headersTestDataDir.resolve("companion"))
    }

    @Test
    fun `test - anonymous functions`() {
        doTest(headersTestDataDir.resolve("anonymousFunctions"))
    }

    @Test
    fun `test - sam interface`() {
        doTest(headersTestDataDir.resolve("samInterface"))
    }

    @Test
    fun `test - simple data class`() {
        doTest(headersTestDataDir.resolve("simpleDataClass"))
    }

    @Test
    fun `test - special function names`() {
        doTest(headersTestDataDir.resolve("specialFunctionNames"))
    }

    @Test
    @TodoAnalysisApi
    fun `test - special function names with explicit method family`() {
        doTest(headersTestDataDir.resolve("specialFunctionNamesExplicitMethodFamily"), Configuration(explicitMethodFamily = true))
    }

    @Test
    fun `test - vararg`() {
        doTest(headersTestDataDir.resolve("vararg"))
    }

    /**
     * KT-66066
     */
    @Test
    fun `test - member function signature order`() {
        doTest(headersTestDataDir.resolve("memberFunctionSignatureOrder"))
    }

    @Test
    fun `test - multiple inheritance`() {
        doTest(headersTestDataDir.resolve("multipleInheritance"))
    }

    @Test
    fun `test - private super interface`() {
        doTest(headersTestDataDir.resolve("privateSuperInterface"))
    }

    @Test
    fun `test- privateSuperInterfaceWithCovariantOverride`() {
        doTest(headersTestDataDir.resolve("privateSuperInterfaceWithCovariantOverride"))
    }

    @Test
    fun `test - superClassWithCovariantOverride`() {
        doTest(headersTestDataDir.resolve("superClassWithCovariantOverride"))
    }

    @Test
    fun `test - privateGenericSuperInterface`() {
        doTest(headersTestDataDir.resolve("privateGenericSuperInterface"))
    }

    @Test
    fun `test - throwable`() {
        doTest(headersTestDataDir.resolve("throwable"))
    }

    @Test
    fun `test - illegalStateException`() {
        doTest(headersTestDataDir.resolve("illegalStateException"))
    }

    @Test
    fun `test - suspend function`() {
        doTest(headersTestDataDir.resolve("suspendFunction"))
    }

    @Test
    fun `test - innerClass`() {
        doTest(headersTestDataDir.resolve("innerClass"))
    }

    /**
     * Works except properties sorting with special name [org.jetbrains.kotlin.name.Name.special]
     * See KT-66510
     */
    @Test
    @TodoAnalysisApi
    fun `test - innerClassWithExtensionFunction`() {
        doTest(headersTestDataDir.resolve("innerClassWithExtensionFunction"))
    }

    @Test
    fun `test - sourceFileWithDotInName`() {
        doTest(headersTestDataDir.resolve("sourceFileWithDotInName"))
    }

    @Test
    fun `test - c properties`() {
        doTest(headersTestDataDir.resolve("cProperties"))
    }

    @Test
    @TodoAnalysisApi
    fun `test - objCEntryPoints`() {
        doTest(headersTestDataDir.resolve("objCEntryPoints"))
    }

    @Test
    fun `test - objCMappedPropertyExtension`() {
        doTest(headersTestDataDir.resolve("objCMappedPropertyExtension"))
    }

    /**
     * Translation works as expected except properties order
     * See KT-66510
     */
    @Test
    @TodoAnalysisApi
    fun `test - objCMappedMixedTypesExtension`() {
        doTest(headersTestDataDir.resolve("objCMappedMixedTypesExtension"))
    }

    @Test
    fun `test - functionWithReservedMethodName`() {
        doTest(headersTestDataDir.resolve("functionWithReservedMethodName"))
    }

    @Test
    fun `test - functionWithReservedMethodNameAndReturnType`() {
        doTest(headersTestDataDir.resolve("functionWithReservedMethodNameAndReturnType"))
    }

    @Test
    fun `test - nothing`() {
        doTest(headersTestDataDir.resolve("nothing"))
    }

    @Test
    fun `test - classWithReservedName`() {
        doTest(headersTestDataDir.resolve("classWithReservedName"))
    }

    @Test
    fun `test - objectWithReservedName`() {
        doTest(headersTestDataDir.resolve("objectWithReservedName"))
    }

    /**
     * Depends on unimplemented AA deprecation message: KT-67823
     */
    @Test
    @TodoAnalysisApi
    fun `test - deprecatedHidden`() {
        doTest(headersTestDataDir.resolve("deprecatedHidden"))
    }

    @Test
    fun `test - inlineClassWithNestedClass`() {
        doTest(headersTestDataDir.resolve("inlineClassWithNestedClass"))
    }

    @Test
    fun `test - privateTopLevelClassProperty`() {
        doTest(headersTestDataDir.resolve("privateTopLevelClassProperty"))
    }

    /**
     * Depends on unimplemented AA deprecation message: KT-67823
     */
    @Test
    @TodoAnalysisApi
    fun `test - deprecatedWarningAndError`() {
        doTest(headersTestDataDir.resolve("deprecatedWarningAndError"))
    }

    @Test
    fun `test - top level interface extension property`() {
        doTest(headersTestDataDir.resolve("topLevelInterfaceExtensionProperty"))
    }

    @Test
    fun `test - internalPublicApi`() {
        doTest(headersTestDataDir.resolve("internalPublicApi"))
    }

    @Test
    fun `test - extension with primitive parameter`() {
        doTest(headersTestDataDir.resolve("extensionWithPrimitiveParameter"))
    }

    @Test
    fun `test - generic super type`() {
        doTest(headersTestDataDir.resolve("genericSuperType"))
    }

    @Test
    fun `test - class and extension function in same file`() {
        doTest(headersTestDataDir.resolve("classAndExtensionFunctionInSameFile"))
    }

    @Test
    fun `test - empty top level facades`() {
        doTest(headersTestDataDir.resolve("emptyTopLevelFacades"))
    }

    @Test
    fun `test - interface extension`() {
        doTest(headersTestDataDir.resolve("interfaceExtension"))
    }

    @Test
    fun `test - collection type arguments`() {
        doTest(headersTestDataDir.resolve("collectionTypeArguments"))
    }

    @Test
    fun `test - extension order`() {
        doTest(headersTestDataDir.resolve("extensionOrder"))
    }

    @Test
    fun `test - basicConstructorWithUpperBoundParameters`() {
        doTest(headersTestDataDir.resolve("basicConstructorWithUpperBoundParameters"))
    }

    @Test
    fun `test - basicMethodParameterWithUpperBound`() {
        doTest(headersTestDataDir.resolve("basicMethodParameterWithUpperBound"))
    }

    @Test
    fun `test - methodWithMultipleUpperBoundsParameters`() {
        doTest(headersTestDataDir.resolve("methodWithMultipleUpperBoundsParameters"))
    }

    @Test
    fun `test - basicGenericsInAndOut`() {
        doTest(headersTestDataDir.resolve("basicGenericsInAndOut"))
    }

    @Test
    fun `test - methodWithMultipleTypeParameters`() {
        doTest(headersTestDataDir.resolve("methodWithMultipleTypeParameters"))
    }

    @Test
    fun `test - enum c-keywords and special names translation`() {
        doTest(headersTestDataDir.resolve("enumCKeywordsAndSpecialNamesTranslation"))
    }

    @Test
    fun `test - KotlinUnit is forwarded`() {
        doTest(headersTestDataDir.resolve("kotlinUnitIsForwarded"))
    }

    @Test
    fun `test - nullable functional type arguments and return types translated`() {
        doTest(headersTestDataDir.resolve("nullableFunctionalTypeArgumentsAndReturnTypesTranslated"))
    }

    @Test
    fun `test - methods mangling`() {
        doTest(headersTestDataDir.resolve("methodsMangling"))
    }

    @Test
    fun `test - methods mangling with the same parameter names`() {
        doTest(headersTestDataDir.resolve("methodsManglingWithTheSameParameterNames"))
    }

    @Test
    fun `test - mangle receiver`() {
        doTest(headersTestDataDir.resolve("mangleReceiver"))
    }

    @Test
    fun `test - mangle property`() {
        doTest(headersTestDataDir.resolve("mangleProperty"))
    }

    @Test
    fun `test - subclass parameter type translation without upper bound`() {
        doTest(headersTestDataDir.resolve("subclassParameterTypeTranslationWithoutUpperBound"))
    }

    @Test
    fun `test - mangle init constructors`() {
        doTest(headersTestDataDir.resolve("mangleInitConstructors"))
    }

    @Test
    fun `test - generic extension property is not translated as static one`() {
        doTest(headersTestDataDir.resolve("genericExtensionPropertyIsNotTranslatedAsStaticOne"))
    }

    @Test
    fun `test - mangle generics`() {
        doTest(headersTestDataDir.resolve("mangleGenerics"))
    }

    @Test
    fun `test - class type property translation`() {
        doTest(headersTestDataDir.resolve("classTypePropertyTranslation"))
    }

    @Test
    fun `test - extensions mangling`() {
        doTest(headersTestDataDir.resolve("extensionsMangling"))
    }

    @Test
    fun `test - var with private setter translated as immutable property`() {
        doTest(headersTestDataDir.resolve("varWithPrivateSetterTranslatedAsImmutableProperty"))
    }

    /**
     * Disabled because of init constructors order KT-70626
     */
    @Test
    @TodoAnalysisApi
    fun `test - mangle throws annotation`() {
        doTest(headersTestDataDir.resolve("mangleThrowsAnnotation"))
    }

    @Test
    fun `test - functions annotated with @ObjCName`() {
        doTest(headersTestDataDir.resolve("functionsAnnotatedWithObjCName"))
    }

    @Test
    fun `test - classifiers annotated with @ObjCName`() {
        doTest(headersTestDataDir.resolve("classifiersAnnotatedWithObjCName"))
    }

    @Test
    fun `test - properties annotated with @ObjCName`() {
        doTest(headersTestDataDir.resolve("propertiesAnnotatedWithObjCName"))
    }

    @Test
    fun `test - frameworkName is not added when class @ObjCName is the same kotlin name exact == true`() {
        doTest(headersTestDataDir.resolve("frameworkNameWithObjCNameAndExact"), Configuration(frameworkName = "Shared"))
    }

    private fun doTest(root: File, configuration: Configuration = Configuration()) {
        if (!root.isDirectory) fail("Expected ${root.absolutePath} to be directory")
        val generatedHeaders = generator.generateHeaders(root, configuration).toString()
        KotlinTestUtils.assertEqualsToFile(root.resolve("!${root.nameWithoutExtension}.h"), generatedHeaders)
    }
}
