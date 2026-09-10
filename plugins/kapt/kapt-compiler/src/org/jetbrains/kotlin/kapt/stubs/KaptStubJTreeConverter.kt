/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.stubs

import com.sun.tools.javac.code.BoundKind
import org.jetbrains.kotlin.kapt.KaptContextForStubGeneration
import org.jetbrains.kotlin.kapt.base.plus
import org.jetbrains.kotlin.kapt.base.util.TopLevelJava9Aware
import org.jetbrains.kotlin.kapt.base.util.getPackageNameJava9Aware
import org.jetbrains.kotlin.kapt.javac.KaptJavaFileObject
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.code.TypeTag
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.JCTree.JCAnnotation
import com.sun.tools.javac.tree.JCTree.JCBlock
import com.sun.tools.javac.tree.JCTree.JCClassDecl
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit
import com.sun.tools.javac.tree.JCTree.JCExpression
import com.sun.tools.javac.tree.JCTree.JCFieldAccess
import com.sun.tools.javac.tree.JCTree.JCIdent
import com.sun.tools.javac.tree.JCTree.JCImport
import com.sun.tools.javac.tree.JCTree.JCMethodDecl
import com.sun.tools.javac.tree.JCTree.JCModifiers
import com.sun.tools.javac.tree.JCTree.JCStatement
import com.sun.tools.javac.tree.JCTree.JCTypeParameter
import com.sun.tools.javac.tree.JCTree.JCVariableDecl
import com.sun.tools.javac.tree.JCTree.Tag
import com.sun.tools.javac.tree.TreeMaker
import com.sun.tools.javac.tree.TreeScanner
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.kapt.base.mapJList
import org.jetbrains.kotlin.kapt.util.prettyPrint
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import javax.lang.model.element.ElementKind
import kotlin.math.sign
import com.sun.tools.javac.util.List as JavacList


class KaptStubJTreeConverter(
    kaptContext: KaptContextForStubGeneration,
    generateNonExistentClass: Boolean,
) : ParameterizedKaptStubConverter<JCTree, JCExpression, JCStatement, JCClassDecl, JCVariableDecl, JCAnnotation, JCTypeParameter, JCMethodDecl, JCModifiers, JCBlock>(
    kaptContext,
    generateNonExistentClass
) {
    private class JTreeKaptStub(
        private val compilationUnit: JCCompilationUnit,
        kaptMetadata: ByteArray? = null,
    ) : KaptStub(kaptMetadata) {
        override fun packageName(): String = compilationUnit.getPackageNameJava9Aware()?.toString() ?: ""

        override fun simpleClassName(): String =
            (compilationUnit.defs.first { it is JCClassDecl } as JCClassDecl).simpleName.toString()

        override fun sourceFileName(): String = compilationUnit.sourcefile.name

        override fun getText(context: Context): String = compilationUnit.prettyPrint(context)
    }

    private val kdocCommentKeeper = KaptDocCommentKeeper(kaptContext)

    private val treeMakerImportMethod = TreeMaker::class.java.declaredMethods.single { it.name == "Import" }

    private fun <T : JCTree> T.keepKdocCommentsIfNecessary(node: Any): T {
        kdocCommentKeeper.saveKDocComment(this, node)
        return this
    }

    private fun postProcess(topLevel: JCCompilationUnit) {
        topLevel.accept(object : TreeScanner() {
            override fun visitClassDef(clazz: JCClassDecl) {
                // Delete enums inside enum values
                if (clazz.isEnum()) {
                    for (child in clazz.defs) {
                        if (child is JCVariableDecl) {
                            deleteAllEnumsInside(child)
                        }
                    }
                }

                super.visitClassDef(clazz)
            }

            private fun JCClassDecl.isEnum() = mods.flags and Opcodes.ACC_ENUM.toLong() != 0L

            private fun deleteAllEnumsInside(def: JCTree) {
                def.accept(object : TreeScanner() {
                    override fun visitClassDef(clazz: JCClassDecl) {
                        clazz.defs = mapJList(clazz.defs) { child ->
                            if (child is JCClassDecl && child.isEnum()) null else child
                        }

                        super.visitClassDef(clazz)
                    }
                })
            }
        })
    }

    override fun makeNonExistentClassStub(): KaptStub {
        val nonExistentClass = treeMaker.ClassDef(
            treeMaker.Modifiers((Flags.PUBLIC or Flags.FINAL).toLong()),
            treeMaker.name(NON_EXISTENT_CLASS_NAME.shortName().asString()),
            JavacList.nil(),
            null,
            JavacList.nil(),
            JavacList.nil()
        )

        val topLevel = treeMaker.TopLevelJava9Aware(treeMaker.FqName(NON_EXISTENT_CLASS_NAME.parent()), JavacList.of(nonExistentClass))

        topLevel.sourcefile = KaptJavaFileObject(topLevel, nonExistentClass)

        // We basically don't need to add binding for NonExistentClass
        return JTreeKaptStub(topLevel)
    }

    override fun makeStubForTopLevelClass(
        declaration: IrDeclaration,
        lineMappings: KaptLineMappingCollector,
        packageName: String,
        clazz: ClassNode,
    ): KaptStub? {
        val classDeclaration = convertClass(clazz, lineMappings, packageName)?.second ?: return null

        val firFile = findFirFile(declaration)
        val imports = convertImports(firFile, classDeclaration.simpleName.toString())

        val classes = JavacList.of<JCTree>(classDeclaration)

        val packageClause = if (packageName.isEmpty()) null else treeMaker.FqName(packageName)
        val topLevel = treeMaker.TopLevelJava9Aware(packageClause, JavacList.from(imports) + classes)
        topLevel.docComments = kdocCommentKeeper.getDocTable(topLevel)
        topLevel.sourcefile = KaptJavaFileObject(topLevel, classDeclaration)

        postProcess(topLevel)

        return JTreeKaptStub(topLevel, lineMappings.serialize())
    }

    override fun makeTypeApply(type: JCExpression, typeArguments: List<JCExpression>): JCExpression =
        treeMaker.TypeApply(type, JavacList.from(typeArguments))

    override fun makeField(
        field: FieldNode,
        modifiers: JCModifiers,
        initializer: JCExpression?,
        convertedType: JCExpression,
    ): JCVariableDecl {
        return treeMaker.VarDef(modifiers, treeMaker.name(field.name), convertedType, initializer)
            .keepKdocCommentsIfNecessary(field)
    }

    override fun makeParameter(
        name: String,
        type: JCExpression,
        modifiers: JCModifiers,
        isVararg: Boolean,
    ): JCVariableDecl {
        return treeMaker.VarDef(modifiers, treeMaker.name(name), type, null)
    }

    override fun makeEnumValueInitializer(
        name: String,
        args: List<JCExpression>,
        def: JCClassDecl?,
    ): JCExpression {
        @Suppress("InconsistentCommentForJavaParameter")
        return treeMaker.NewClass(
            /* enclosing = */ null,
            /* typeArgs = */ JavacList.nil(),
            /* clazz = */ treeMaker.Ident(treeMaker.name(name)),
            /* args = */ JavacList.from(args),
            /* def = */ def
        )
    }

    override fun makeClassDecl(
        clazz: ClassNode,
        simpleName: String,
        modifiers: JCModifiers,
        genericType: SignatureParser.ClassGenericSignature<JCExpression, JCTypeParameter>,
        superTypes: ClassSupertypes<JCExpression>,
        enumValues: List<JCVariableDecl>,
        sortedConvertedFields: List<JCVariableDecl>,
        sortedConvertedMethods: List<JCMethodDecl>,
        nestedClasses: List<JCClassDecl>,
    ): JCClassDecl {
        return treeMaker.ClassDef(
            modifiers,
            treeMaker.name(simpleName),
            JavacList.from(genericType.typeParameters),
            superTypes.superClass,
            JavacList.from(superTypes.interfaces),
            JavacList.from<JCTree>(enumValues) +
                    JavacList.from(sortedConvertedFields) +
                    JavacList.from(sortedConvertedMethods) +
                    JavacList.from(nestedClasses)
        ).keepKdocCommentsIfNecessary(clazz)
    }

    override fun makeAssignExpression(
        name: String,
        expression: JCExpression,
    ): JCExpression = treeMaker.Assign(treeMaker.SimpleName(name), expression)

    override fun makeSingleImport(fqName: FqName): JCTree {
        val importedExpr = makeQualifiedName(fqName.asString())
        return treeMakerImportMethod.invoke(treeMaker, importedExpr, false) as JCImport
    }

    override fun makeStarImport(fqName: FqName): JCTree {
        val importedExpr = makeQualifiedName(fqName.asString())
        return treeMakerImportMethod.invoke(
            treeMaker, treeMaker.Select(importedExpr, treeMaker.nameTable.names.asterisk), false
        ) as JCImport
    }

    override fun isNonExistentClass(type: JCExpression): Boolean =
        type is JCFieldAccess &&
                type.name.toString() == NON_EXISTENT_CLASS_NAME.shortName().asString() &&
                (type.selected as? JCIdent)?.name?.toString() == NON_EXISTENT_CLASS_NAME.parent().asString()

    override fun makeAnnotation(
        type: JCExpression,
        arguments: List<JCExpression>,
    ): JCAnnotation {
        return treeMaker.Annotation(type, JavacList.from(arguments))
    }

    override fun makeModifiers(kind: ElementKind, isEnumField: Boolean, flags: Long, annotations: List<JCAnnotation>): JCModifiers =
        treeMaker.Modifiers(flags, JavacList.from(annotations))

    override fun makeTypeParameter(name: String, allBounds: List<JCExpression>): JCTypeParameter =
        treeMaker.TypeParameter(treeMaker.name(name), JavacList.from(allBounds))

    override fun makeUnboundWildcard(): JCExpression =
        treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.UNBOUND), null)

    override fun makeWildcard(asmWilcard: Char, bound: JCExpression): JCExpression = when (asmWilcard) {
        '=' -> bound
        '+' -> treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.EXTENDS), bound)
        '-' -> treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.SUPER), bound)
        else -> error("Unknown variance, '=', '+' or '-' expected")
    }

    override fun makeArrayType(
        baseType: JCExpression,
        arrayDimensions: Int,
    ): JCExpression {
        var resultExpression = baseType
        var count = arrayDimensions
        while (count > 0) {
            resultExpression = treeMaker.TypeArray(resultExpression)
            count--
        }
        return resultExpression
    }

    override fun makePrimitiveType(primitiveTypeSig: Char): JCExpression = when (primitiveTypeSig) {
        'V' -> treeMaker.TypeIdent(TypeTag.VOID)
        'Z' -> treeMaker.TypeIdent(TypeTag.BOOLEAN)
        'C' -> treeMaker.TypeIdent(TypeTag.CHAR)
        'B' -> treeMaker.TypeIdent(TypeTag.BYTE)
        'S' -> treeMaker.TypeIdent(TypeTag.SHORT)
        'I' -> treeMaker.TypeIdent(TypeTag.INT)
        'F' -> treeMaker.TypeIdent(TypeTag.FLOAT)
        'J' -> treeMaker.TypeIdent(TypeTag.LONG)
        'D' -> treeMaker.TypeIdent(TypeTag.DOUBLE)
        else -> error("Illegal primitive type $primitiveTypeSig")
    }

    override fun makeSimpleName(simpleName: String): JCExpression =
        treeMaker.SimpleName(simpleName)

    override fun makeSelect(left: JCExpression, simpleName: String): JCExpression =
        treeMaker.Select(left, treeMaker.name(simpleName))

    override fun makeType(asmType: Type): JCExpression = treeMaker.Type(asmType)

    override fun makeQualifiedName(javaName: String): JCExpression = treeMaker.FqName(javaName)
    override fun makeQualifiedName(fqName: FqName): JCExpression = treeMaker.FqName(fqName)

    override fun makeArray(expressions: List<JCExpression>): JCExpression =
        treeMaker.NewArray(null, JavacList.nil(), JavacList.from(expressions))

    override fun makeValueOfPrimitiveTypeOrString(value: Any?): JCExpression? {
        fun specialFpValueNumerator(value: Double): Double = if (value.isNaN()) 0.0 else 1.0 * value.sign

        return when (value) {
            is Char -> {
                treeMaker.Literal(TypeTag.CHAR, value.code)
            }
            is Byte -> {
                treeMaker.TypeCast(treeMaker.TypeIdent(TypeTag.BYTE), treeMaker.Literal(TypeTag.INT, value.toInt()))
            }
            is Short -> {
                treeMaker.TypeCast(treeMaker.TypeIdent(TypeTag.SHORT), treeMaker.Literal(TypeTag.INT, value.toInt()))
            }
            is Boolean, is Int -> {
                treeMaker.Literal(value)
            }
            is Long -> {
                treeMaker.Literal(value)
            }
            is String -> {
                treeMaker.Literal(value)
            }
            is Float if value.isFinite() -> {
                treeMaker.Literal(value)
            }
            is Float -> {
                treeMaker.Binary(
                    Tag.DIV,
                    treeMaker.Literal(specialFpValueNumerator(value.toDouble()).toFloat()),
                    treeMaker.Literal(0.0F)
                )
            }
            is Double if value.isFinite() -> {
                treeMaker.Literal(value)
            }
            is Double -> {
                treeMaker.Binary(Tag.DIV, treeMaker.Literal(specialFpValueNumerator(value)), treeMaker.Literal(0.0))
            }
            is UByte -> {
                treeMaker.TypeCast(treeMaker.TypeIdent(TypeTag.BYTE), treeMaker.Literal(TypeTag.INT, value.toInt()))
            }
            is UShort -> {
                treeMaker.TypeCast(treeMaker.TypeIdent(TypeTag.SHORT), treeMaker.Literal(TypeTag.INT, value.toInt()))
            }
            is UInt -> {
                treeMaker.Literal(value.toInt())
            }
            is ULong -> {
                treeMaker.Literal(value.toLong())
            }

            else -> null
        }
    }

    override fun makeNullLiteral(): JCExpression =
        treeMaker.Literal(TypeTag.BOT, null)

    override fun makeBlock(statements: List<JCStatement>): JCBlock =
        treeMaker.Block(0, JavacList.from(statements))

    override fun makeSimpleCallStatement(
        name: String,
        args: List<JCExpression>,
    ): JCStatement {
        val call = treeMaker.Apply(JavacList.nil(), treeMaker.SimpleName(name), JavacList.from(args))
        return treeMaker.Exec(call)
    }

    override fun makeReturn(arg: JCExpression): JCStatement =
        treeMaker.Return(arg)

    override fun makeMethod(
        method: MethodNode,
        irClass: IrClass,
        modifiers: JCModifiers,
        genericSignature: SignatureParser.MethodGenericSignature<JCExpression, JCTypeParameter>,
        parameters: List<JCVariableDecl>,
        exceptionTypes: List<JCExpression>,
        body: JCBlock?,
        defaultValue: JCExpression?,
    ): JCMethodDecl = treeMaker.MethodDef(
        modifiers,
        treeMaker.name(method.name),
        genericSignature.returnType,
        JavacList.from(genericSignature.typeParameters),
        JavacList.from(parameters),
        JavacList.from(exceptionTypes),
        body,
        defaultValue
    ).keepKdocCommentsIfNecessary(method)
}
