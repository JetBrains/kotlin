/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.reportWarning
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.isAbstract
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.getIncludedLibraryDescriptors
import org.jetbrains.kotlin.backend.konan.ir.typeWithStarProjections
import org.jetbrains.kotlin.backend.konan.ir.typeWithoutArguments
import org.jetbrains.kotlin.backend.konan.reportCompilationError
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

internal class TestProcessor (val context: Context) {

    object TEST_SUITE_CLASS: IrDeclarationOriginImpl("TEST_SUITE_CLASS")
    object TEST_SUITE_GENERATED_MEMBER: IrDeclarationOriginImpl("TEST_SUITE_GENERATED_MEMBER")

    companion object {
        val COMPANION_GETTER_NAME = Name.identifier("getCompanion")
        val INSTANCE_GETTER_NAME = Name.identifier("createInstance")

        val IGNORE_FQ_NAME = FqName.fromSegments(listOf("kotlin", "test" , "Ignore"))
    }

    private val symbols = context.ir.symbols

    private val baseClassSuite = symbols.baseClassSuite.owner

    private val topLevelSuiteNames = mutableSetOf<String>()

    // region Useful extensions.
    private var testSuiteCnt = 0
    private fun Name.synthesizeSuiteClassName() = identifier.synthesizeSuiteClassName()
    private fun String.synthesizeSuiteClassName() = "$this\$test\$${testSuiteCnt++}".synthesizedName

    // IrFile always uses a forward slash as a directory separator.
    private val IrFile.fileName
        get() = name.substringAfterLast('/')

    private val IrFile.topLevelSuiteName: String
        get() {
            val packageFqName = fqName
            val shortFileName = PackagePartClassUtils.getFilePartShortName(fileName)
            return if (packageFqName.isRoot) shortFileName else "$packageFqName.$shortFileName"
        }

    private fun MutableList<TestFunction>.registerFunction(
            function: IrFunction,
            kinds: Collection<Pair<FunctionKind, /* ignored: */ Boolean>>) =
        kinds.forEach { (kind, ignored) ->
            add(TestFunction(function, kind, ignored))
        }

    private fun MutableList<TestFunction>.registerFunction(
        function: IrFunction,
        kind: FunctionKind,
        ignored: Boolean
    ) = add(TestFunction(function, kind, ignored))

    private fun <T: IrElement> IrStatementsBuilder<T>.generateFunctionRegistration(
            receiver: IrValueDeclaration,
            registerTestCase: IrFunction,
            registerFunction: IrFunction,
            functions: Collection<TestFunction>) {
        functions.forEach {
            if (it.kind == FunctionKind.TEST) {
                // Call registerTestCase(name: String, testFunction: () -> Unit) method.
                +irCall(registerTestCase).apply {
                    dispatchReceiver = irGet(receiver)
                    putValueArgument(0, irString(it.function.name.identifier))
                    putValueArgument(1, IrFunctionReferenceImpl(
                            it.function.startOffset,
                            it.function.endOffset,
                            registerTestCase.valueParameters[1].type,
                            it.function.symbol,
                            typeArgumentsCount = 0,
                            valueArgumentsCount = 0,
                            reflectionTarget = null))
                    putValueArgument(2, irBoolean(it.ignored))
                }
            } else {
                // Call registerFunction(kind: TestFunctionKind, () -> Unit) method.
                +irCall(registerFunction).apply {
                    dispatchReceiver = irGet(receiver)
                    val testKindEntry = it.kind.runtimeKind
                    putValueArgument(0, IrGetEnumValueImpl(
                            it.function.startOffset,
                            it.function.endOffset,
                            symbols.testFunctionKind.typeWithoutArguments,
                            testKindEntry)
                    )
                    putValueArgument(1, IrFunctionReferenceImpl(
                            it.function.startOffset,
                            it.function.endOffset,
                            registerFunction.valueParameters[1].type,
                            it.function.symbol,
                            typeArgumentsCount = 0,
                            valueArgumentsCount = 0,
                            reflectionTarget = null))
                }
            }
        }
    }
    // endregion

    // region Classes for annotation collection.
    internal enum class FunctionKind(annotationNameString: String, val runtimeKindString: String) {
        TEST("kotlin.test.Test", ""),
        BEFORE_TEST("kotlin.test.BeforeTest", "BEFORE_TEST"),
        AFTER_TEST("kotlin.test.AfterTest", "AFTER_TEST"),
        BEFORE_CLASS("kotlin.test.BeforeClass", "BEFORE_CLASS"),
        AFTER_CLASS("kotlin.test.AfterClass", "AFTER_CLASS");

        val annotationFqName = FqName(annotationNameString)

        companion object {
            val INSTANCE_KINDS = listOf(TEST, BEFORE_TEST, AFTER_TEST)
            val COMPANION_KINDS = listOf(BEFORE_CLASS, AFTER_CLASS)
        }
    }

    private val FunctionKind.runtimeKind: IrEnumEntrySymbol
        get() = symbols.getTestFunctionKind(this)

    private fun IrType.isTestFunctionKind() = classifierOrNull == symbols.testFunctionKind

    private data class TestFunction(
        val function: IrFunction,
        val kind: FunctionKind,
        val ignored: Boolean
    )

    private inner class TestClass(val ownerClass: IrClass) {
        var companion: IrClass? = null
        val functions = mutableListOf<TestFunction>()

        fun registerFunction(
            function: IrFunction,
            kinds: Collection<Pair<FunctionKind, /* ignored: */ Boolean>>
        ) = functions.registerFunction(function, kinds)

        fun registerFunction(function: IrFunction, kind: FunctionKind, ignored: Boolean) =
                functions.registerFunction(function, kind, ignored)
    }

    private inner class AnnotationCollector(val irFile: IrFile) : IrElementVisitorVoid {
        val testClasses = mutableMapOf<IrClass, TestClass>()
        val topLevelFunctions = mutableListOf<TestFunction>()

        private fun MutableMap<IrClass, TestClass>.getTestClass(key: IrClass) =
                getOrPut(key) { TestClass(key) }

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        fun IrFunctionSymbol.hasAnnotation(fqName: FqName) = descriptor.annotations.any { it.fqName == fqName }

        /**
         * Checks if [this] or any of its parent functions has the annotation with the given [testAnnotation].
         * If [this] contains the given annotation, returns [this].
         * If one of the parent functions contains the given annotation, returns the [IrFunctionSymbol] for it.
         * If the annotation isn't found or found only in interface methods, returns null.
         */
        fun IrFunctionSymbol.findAnnotatedFunction(testAnnotation: FqName): IrFunctionSymbol? {
            val owner = this.owner
            val parent = owner.parent
            if (parent is IrClass && parent.isInterface) {
                return null
            }

            if (hasAnnotation(testAnnotation)) {
                return this
            }

            return (owner as? IrSimpleFunction)
                ?.overriddenSymbols
                ?.firstNotNullResult {
                    it.findAnnotatedFunction(testAnnotation)
                }
        }

        fun registerClassFunction(irClass: IrClass,
                                  function: IrFunction,
                                  kinds: Collection<Pair<FunctionKind, /* ignored: */ Boolean>>) {

            fun warn(msg: String) = context.reportWarning(msg, irFile, function)

            kinds.forEach { (kind, ignored) ->
                val annotation = kind.annotationFqName
                when (kind) {
                    in FunctionKind.INSTANCE_KINDS -> with(irClass) {
                        when {
                            isInner ->
                                warn("Annotation $annotation is not allowed for methods of an inner class")

                            isAbstract() -> {
                                // We cannot create an abstract test class but it's allowed to mark its methods as
                                // tests because the class can be extended. So skip this case without warnings.
                            }

                            isCompanion ->
                                warn("Annotation $annotation is not allowed for methods of a companion object")

                            constructors.none { it.valueParameters.size == 0 } ->
                                warn("Test class has no default constructor: $fqNameForIrSerialization")

                            else ->
                                testClasses.getTestClass(irClass).registerFunction(function, kind, ignored)
                        }
                    }
                    in FunctionKind.COMPANION_KINDS ->
                        when {
                            irClass.isCompanion -> {
                                val containingClass = irClass.parentAsClass
                                val testClass = testClasses.getTestClass(containingClass)
                                testClass.companion = irClass
                                testClass.registerFunction(function, kind, ignored)
                            }

                            irClass.kind == ClassKind.OBJECT -> {
                                testClasses.getTestClass(irClass).registerFunction(function, kind, ignored)
                            }

                            else -> warn("Annotation $annotation is only allowed for methods of an object " +
                                    "(named or companion) or top level functions")
                        }
                    else -> throw IllegalStateException("Unreachable")
                }
            }
        }

        fun IrFunction.checkFunctionSignature() {
            // Test runner requires test functions to have the following signature: () -> Unit.
            if (!returnType.isUnit()) {
                context.reportCompilationError(
                        "Test function must return Unit: $fqNameForIrSerialization", irFile, this
                )
            }
            if (valueParameters.isNotEmpty()) {
                context.reportCompilationError(
                        "Test function must have no arguments: $fqNameForIrSerialization", irFile, this
                )
            }
        }

        private fun warnAboutInheritedAnnotations(
            kind: FunctionKind,
            function: IrFunctionSymbol,
            annotatedFunction: IrFunctionSymbol
        ) {
            if (function.owner != annotatedFunction.owner) {
                context.reportWarning(
                    "Super method has a test annotation ${kind.annotationFqName} but the overriding method doesn't. " +
                            "Note that the overriding method will still be executed.",
                    irFile,
                    function.owner
                )
            }
        }

        private fun warnAboutLoneIgnore(functionSymbol: IrFunctionSymbol): Unit = with(functionSymbol) {
            if (hasAnnotation(IGNORE_FQ_NAME) && !hasAnnotation(FunctionKind.TEST.annotationFqName)) {
                context.reportWarning(
                    "Unused $IGNORE_FQ_NAME annotation (not paired with ${FunctionKind.TEST.annotationFqName}).",
                    irFile,
                    owner
                )
            }
        }

        // TODO: Use symbols instead of containingDeclaration when such information is available.
        override fun visitFunction(declaration: IrFunction) {
            val symbol = declaration.symbol
            val parent = declaration.parent

            warnAboutLoneIgnore(symbol)
            val kinds = FunctionKind.values().mapNotNull { kind ->
                symbol.findAnnotatedFunction(kind.annotationFqName)?.let { annotatedFunction ->
                    warnAboutInheritedAnnotations(kind, symbol, annotatedFunction)
                    kind to (kind == FunctionKind.TEST && annotatedFunction.hasAnnotation(IGNORE_FQ_NAME))
                }
            }

            if (kinds.isEmpty()) {
                return
            }
            declaration.checkFunctionSignature()

            when (parent) {
                is IrPackageFragment -> topLevelFunctions.registerFunction(declaration, kinds)
                is IrClass -> registerClassFunction(parent, declaration, kinds)
                else -> UnsupportedOperationException("Cannot create test function $declaration (defined in $parent")
            }
        }
    }
    // endregion

    //region Symbol and IR builders

    /**
     * Builds a method in `[owner]` class with name `[getterName]`
     * returning a reference to an object represented by `[objectSymbol]`.
     */
    private fun buildObjectGetter(objectSymbol: IrClassSymbol,
                                  owner: IrClass,
                                  getterName: Name): IrSimpleFunction =
        IrFunctionImpl(
                owner.startOffset, owner.endOffset,
                TEST_SUITE_GENERATED_MEMBER,
                IrSimpleFunctionSymbolImpl(),
                getterName,
                DescriptorVisibilities.PROTECTED,
                Modality.FINAL,
                objectSymbol.typeWithStarProjections,
                isInline = false,
                isExternal = false,
                isTailrec = false,
                isSuspend = false,
                isExpect = false,
                isFakeOverride = false,
                isOperator = false,
                isInfix = false
        ).apply {
            parent = owner

            val superFunction = baseClassSuite.simpleFunctions()
                    .single { it.name == getterName && it.valueParameters.isEmpty() }

            createDispatchReceiverParameter()
            overriddenSymbols += superFunction.symbol

            body = context.createIrBuilder(symbol, symbol.owner.startOffset, symbol.owner.endOffset).irBlockBody {
                +irReturn(irGetObjectValue(objectSymbol.typeWithoutArguments, objectSymbol)
                )
            }
        }

    /**
     * Builds a method in `[testSuite]` class with name `[getterName]`
     * returning a new instance of class referenced by [classSymbol].
     */
    private fun buildInstanceGetter(classSymbol: IrClassSymbol,
                                    owner: IrClass,
                                    getterName: Name): IrSimpleFunction =
        IrFunctionImpl(
                owner.startOffset, owner.endOffset,
                TEST_SUITE_GENERATED_MEMBER,
                IrSimpleFunctionSymbolImpl(),
                getterName,
                DescriptorVisibilities.PROTECTED,
                Modality.FINAL,
                classSymbol.typeWithStarProjections,
                isInline = false,
                isExternal = false,
                isTailrec = false,
                isSuspend = false,
                isExpect = false,
                isFakeOverride = false,
                isOperator = false,
                isInfix = false
        ).apply {
            parent = owner

            val superFunction = baseClassSuite.simpleFunctions()
                    .single { it.name == getterName && it.valueParameters.isEmpty() }

            createDispatchReceiverParameter()
            overriddenSymbols += superFunction.symbol

            body = context.createIrBuilder(symbol, symbol.owner.startOffset, symbol.owner.endOffset).irBlockBody {
                val constructor = classSymbol.owner.constructors.single { it.valueParameters.isEmpty() }
                +irReturn(irCall(constructor))
            }
        }

    private val baseClassSuiteConstructor = baseClassSuite.constructors.single {
        it.valueParameters.size == 2
                && it.valueParameters[0].type.isString()  // name: String
                && it.valueParameters[1].type.isBoolean() // ignored: Boolean
    }

    /**
     * Builds a constructor for a test suite class representing a test class (any class in the original IrFile with
     * method(s) annotated with @Test). The test suite class is a subclass of ClassTestSuite<T>
     * where T is the test class.
     */
    private fun buildClassSuiteConstructor(suiteName: String,
                                           testClassType: IrType,
                                           testCompanionType: IrType,
                                           testSuite: IrClassSymbol,
                                           owner: IrClass,
                                           functions: Collection<TestFunction>,
                                           ignored: Boolean): IrConstructor =
        IrConstructorImpl(
                testSuite.owner.startOffset, testSuite.owner.endOffset,
                TEST_SUITE_GENERATED_MEMBER,
                IrConstructorSymbolImpl(),
                Name.special("<init>"),
                DescriptorVisibilities.PUBLIC,
                testSuite.typeWithStarProjections,
                isInline = false,
                isExternal = false,
                isPrimary = true,
                isExpect = false
        ).apply {
            parent = owner

            fun IrClass.getFunction(name: String, predicate: (IrSimpleFunction) -> Boolean) =
                    simpleFunctions().single { it.name.asString() == name && predicate(it) }

            val registerTestCase = baseClassSuite.getFunction("registerTestCase") {
                it.valueParameters.size == 3
                        && it.valueParameters[0].type.isString()   // name: String
                        && it.valueParameters[1].type.isFunction() // function: testClassType.() -> Unit
                        && it.valueParameters[2].type.isBoolean()  // ignored: Boolean
            }
            val registerFunction = baseClassSuite.getFunction("registerFunction") {
                it.valueParameters.size == 2
                        && it.valueParameters[0].type.isTestFunctionKind() // kind: TestFunctionKind
                        && it.valueParameters[1].type.isFunction()         // function: () -> Unit
            }

            body = context.createIrBuilder(symbol, symbol.owner.startOffset, symbol.owner.endOffset).irBlockBody {
                +irDelegatingConstructorCall(baseClassSuiteConstructor).apply {
                    putTypeArgument(0, testClassType)
                    putTypeArgument(1, testCompanionType)

                    putValueArgument(0, irString(suiteName))
                    putValueArgument(1, irBoolean(ignored))
                }
                generateFunctionRegistration(testSuite.owner.thisReceiver!!,
                        registerTestCase, registerFunction, functions)
            }
        }

    private val IrClass.ignored: Boolean get() = annotations.hasAnnotation(IGNORE_FQ_NAME)

    /**
     * Builds a test suite class representing a test class (any class in the original IrFile with method(s)
     * annotated with @Test). The test suite class is a subclass of ClassTestSuite<T> where T is the test class.
     */
    private fun buildClassSuite(testClass: IrClass,
                                testCompanion: IrClass?,
                                functions: Collection<TestFunction>): IrClass =
        IrClassImpl(
                testClass.startOffset, testClass.endOffset,
                TEST_SUITE_CLASS,
                IrClassSymbolImpl(),
                testClass.name.synthesizeSuiteClassName(),
                ClassKind.CLASS,
                DescriptorVisibilities.PRIVATE,
                Modality.FINAL,
                isCompanion = false,
                isInner = false,
                isData = false,
                isExternal = false,
                isInline = false,
                isExpect = false,
                isFun = false
        ).apply {
            createParameterDeclarations()

            val testClassType = testClass.defaultType
            val testCompanionType = if (testClass.kind == ClassKind.OBJECT) {
                testClassType
            } else {
                testCompanion?.defaultType ?: context.irBuiltIns.nothingType
            }

            val constructor = buildClassSuiteConstructor(
                    testClass.fqNameForIrSerialization.toString(), testClassType, testCompanionType,
                    symbol, this, functions, testClass.ignored
            )

            val instanceGetter: IrFunction
            val companionGetter: IrFunction?

            if (testClass.kind == ClassKind.OBJECT) {
                instanceGetter = buildObjectGetter(testClass.symbol, this, INSTANCE_GETTER_NAME)
                companionGetter = buildObjectGetter(testClass.symbol, this, COMPANION_GETTER_NAME)
            } else {
                instanceGetter = buildInstanceGetter(testClass.symbol, this, INSTANCE_GETTER_NAME)
                companionGetter = testCompanion?.let {
                    buildObjectGetter(it.symbol, this, COMPANION_GETTER_NAME)
                }
            }

            declarations += constructor
            declarations += instanceGetter
            companionGetter?.let { declarations += it }

            superTypes += symbols.baseClassSuite.typeWith(listOf(testClassType, testCompanionType))
            addFakeOverrides(context.irBuiltIns)
        }
    //endregion

    // region IR generation methods
    private fun generateClassSuite(irFile: IrFile, testClass: TestClass) =
            with(buildClassSuite(testClass.ownerClass, testClass.companion,testClass.functions)) {
                irFile.addChild(this)
                val irConstructor = constructors.single()
                context.createIrBuilder(irFile.symbol, testClass.ownerClass.startOffset, testClass.ownerClass.endOffset).run {
                    irFile.addTopLevelInitializer(
                            irCall(irConstructor),
                            this@TestProcessor.context,
                            threadLocal = true)
                }
            }

    /** Check if this fqName already used or not. */
    private fun checkSuiteName(irFile: IrFile, name: String): Boolean {
        if (topLevelSuiteNames.contains(name)) {
            context.reportCompilationError("Package '${irFile.fqName}' has top-level test " +
                    "functions in several files with the same name: '${irFile.fileName}'")
        }
        topLevelSuiteNames.add(name)
        return true
    }

    private val topLevelSuite = symbols.topLevelSuite.owner
    private val topLevelSuiteConstructor = topLevelSuite.constructors.single {
        it.valueParameters.size == 1
                && it.valueParameters[0].type.isString()
    }
    private val topLevelSuiteRegisterFunction = topLevelSuite.simpleFunctions().single {
        it.name.asString() == "registerFunction"
                && it.valueParameters.size == 2
                && it.valueParameters[0].type.isTestFunctionKind()
                && it.valueParameters[1].type.isFunction()
    }
    private val topLevelSuiteRegisterTestCase = topLevelSuite.simpleFunctions().single {
        it.name.asString() == "registerTestCase"
                && it.valueParameters.size == 3
                && it.valueParameters[0].type.isString()
                && it.valueParameters[1].type.isFunction()
                && it.valueParameters[2].type.isBoolean()
    }

    private fun generateTopLevelSuite(irFile: IrFile, functions: Collection<TestFunction>) {
        val builder = context.createIrBuilder(irFile.symbol)
        val suiteName = irFile.topLevelSuiteName
        if (!checkSuiteName(irFile, suiteName)) {
            return
        }

        // TODO: an awful hack, we make this initializer thread local, so that it doesn't freeze suite,
        // and later on we could modify some suite's properties. This shall be redesigned.
        irFile.addTopLevelInitializer(builder.irBlock {
            val constructorCall = irCall(topLevelSuiteConstructor).apply {
                putValueArgument(0, IrConstImpl.string(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                                context.irBuiltIns.stringType, suiteName))
            }
            val testSuiteVal = irTemporary(constructorCall,  "topLevelTestSuite")
            generateFunctionRegistration(testSuiteVal,
                    topLevelSuiteRegisterTestCase,
                    topLevelSuiteRegisterFunction,
                    functions)
        }, context, threadLocal = true)
    }

    private fun createTestSuites(irFile: IrFile, annotationCollector: AnnotationCollector) {
        annotationCollector.testClasses.filter {
            it.value.functions.any { it.kind == FunctionKind.TEST }
        }.forEach { _, testClass ->
            generateClassSuite(irFile, testClass)
        }
        if (annotationCollector.topLevelFunctions.isNotEmpty()) {
            generateTopLevelSuite(irFile, annotationCollector.topLevelFunctions)
        }
    }
    // endregion

    private fun shouldProcessFile(irFile: IrFile): Boolean = irFile.packageFragmentDescriptor.module.let {
        // Process test annotations in source libraries too.
        it == context.moduleDescriptor || it in context.getIncludedLibraryDescriptors()
    }

    fun process(irFile: IrFile) {
        // TODO: uses descriptors.
        if (!shouldProcessFile(irFile)) {
            return
        }

        val annotationCollector = AnnotationCollector(irFile)
        irFile.acceptChildrenVoid(annotationCollector)
        createTestSuites(irFile, annotationCollector)
    }
}