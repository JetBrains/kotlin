/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe.extensions

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.dataframe.Names
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.builder.buildPropertyAccessorCopy
import org.jetbrains.kotlin.fir.declarations.impl.FirPropertyAccessorImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.extensions.AnnotationFqn
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.annotated
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.toTypeProjection
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlinx.dataframe.DataColumn

class TestGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        val predicate = annotated(AnnotationFqn("org.jetbrains.kotlinx.dataframe.Gen"))
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(predicate)
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>): Set<Name> {
        return if (classSymbol.classId.shortClassName == Name.identifier("PropertiesScope1")) {
            setOf(Name.identifier("age"))
        } else {
            emptySet()
        }
    }

    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        val extensionPropertySymbol = FirPropertySymbol(callableId)
        val property = buildProperty {
            moduleData = session.moduleData
            origin = FirDeclarationOrigin.Plugin(TestKey)
            attributes = FirDeclarationAttributes()
            status = FirResolvedDeclarationStatusImpl(Visibilities.Public, Modality.FINAL, EffectiveVisibility.Public)
            returnTypeRef = buildResolvedTypeRef {
                type = ConeClassLikeTypeImpl(
                    ConeClassLikeLookupTagImpl(Names.DATA_COLUMN_CLASS_ID),
                    arrayOf(session.builtinTypes.intType.type.toTypeProjection(Variance.INVARIANT)),
                    false
                )
            }
            receiverTypeRef = buildResolvedTypeRef {
                type = ConeClassLikeTypeImpl(
                    ConeClassLikeLookupTagImpl(
                        Names.DF_CLASS_ID
                    ),
                    arrayOf(
                        ConeClassLikeTypeImpl(
                            ConeClassLikeLookupTagImpl(
                                ClassId(
                                    FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe")),
                                    Name.identifier("Cars")
                                )
                            ),
                            emptyArray(),
                            isNullable = false
                        )
                    ),
                    false
                )
            }
            dispatchReceiverType = ConeClassLikeTypeImpl(
                ConeClassLikeLookupTagImpl(
                    ClassId(
                        FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe")),
                        Name.identifier("PropertiesScope1")
                    )
                ),
                emptyArray(),
                false
            )
            name = Name.identifier("age")
            val getterSymbol = FirPropertyAccessorSymbol()
            isVar = false
            getter = buildPropertyAccessor {
                moduleData = session.moduleData
                origin = FirDeclarationOrigin.Plugin(TestKey)
                status = FirResolvedDeclarationStatusImpl(Visibilities.Public, Modality.FINAL, EffectiveVisibility.Public)
                returnTypeRef = buildResolvedTypeRef {
                    type = ConeClassLikeTypeImpl(
                        ConeClassLikeLookupTagImpl(Names.DATA_COLUMN_CLASS_ID),
                        arrayOf(session.builtinTypes.intType.type.toTypeProjection(Variance.INVARIANT)),
                        false
                    )
                }
                symbol = getterSymbol
                propertySymbol = extensionPropertySymbol
                isGetter = true
            }.also { getterSymbol.bind(it) }
            symbol = extensionPropertySymbol
            isLocal = false
        }
        extensionPropertySymbol.bind(property)
        return listOf(extensionPropertySymbol)
    }
}

object TestKey : GeneratedDeclarationKey()

class TestBodyFiller : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        TestFileLowering(pluginContext).lower(moduleFragment)
    }
}

class TestFileLowering(val context: IrPluginContext) : FileLoweringPass, IrElementTransformerVoid() {
    companion object {
        val COLUMNS_CONTAINER_ID =
            CallableId(ClassId(FqName("org.jetbrains.kotlinx.dataframe"), Name.identifier("ColumnsContainer")), Name.identifier("get"))
        val DATA_ROW_ID =
            CallableId(ClassId(FqName("org.jetbrains.kotlinx.dataframe"), Name.identifier("DataRow")), Name.identifier("get"))
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildren(this, null)
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        val origin = declaration.origin
        return if (origin is IrDeclarationOrigin.GeneratedByPlugin && origin.pluginKey == FirDataFrameReceiverInjector.DataFramePluginKey) {
            declaration
        } else {
            super.visitClass(declaration)
        }
    }

    override fun visitProperty(declaration: IrProperty): IrStatement {
        val origin = declaration.origin
        if (!(origin is IrDeclarationOrigin.GeneratedByPlugin && origin.pluginKey == TestKey)) return declaration
        val getter = declaration.getter ?: return declaration
        val returnType = getter.returnType
        val isDataColumn = returnType.classFqName!!.asString() == DataColumn::class.qualifiedName!!

        val get = if (isDataColumn) {
            context
                .referenceFunctions(COLUMNS_CONTAINER_ID)
                .single {
                    it.owner.valueParameters.size == 1 && it.owner.valueParameters[0].type == context.irBuiltIns.stringType
                }
        } else {
            context
                .referenceFunctions(DATA_ROW_ID)
                .single {
                    it.owner.valueParameters.size == 1 && it.owner.valueParameters[0].type == context.irBuiltIns.stringType
                }
        }

        val call = IrCallImpl(-1, -1, context.irBuiltIns.anyNType, get, 0, 1).also {
            val thisSymbol: IrValueSymbol = getter.extensionReceiverParameter?.symbol!!
            it.dispatchReceiver = IrGetValueImpl(-1, -1, thisSymbol)
            it.putValueArgument(0, IrConstImpl.string(-1, -1, context.irBuiltIns.stringType, declaration.name.identifier))
        }

        val typeOp = IrTypeOperatorCallImpl(-1, -1, returnType, IrTypeOperator.CAST, returnType, call)
        val returnExpression = IrReturnImpl(-1, -1, returnType, getter.symbol, typeOp)
        getter.apply {
            body = IrBlockBodyImpl(-1, -1, listOf(returnExpression))
        }


        return declaration
    }
}
