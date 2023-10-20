/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.formver.embeddings.ListSizeFieldEmbedding
import org.jetbrains.kotlin.formver.embeddings.callables.NamedFunctionSignature
import org.jetbrains.kotlin.formver.names.NameMatcher
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Exp.*

fun LocalVar.sameSize(): Exp = EqCmp(fieldAccess(ListSizeFieldEmbedding.toViper()), Old(fieldAccess(ListSizeFieldEmbedding.toViper())))
fun LocalVar.increasedSize(amount: Int): Exp =
    EqCmp(fieldAccess(ListSizeFieldEmbedding.toViper()), Add(Old(fieldAccess(ListSizeFieldEmbedding.toViper())), IntLit(amount)))

fun NamedFunctionSignature.stdLibPreConditions(): List<Exp> =
    NameMatcher.match(this.name) {
        ifInCollectionsPkg {
            ifFunctionName("get") {
                val receiver = receiver!!.toViper()
                val indexArg = formalArgs[1].toViper()
                return listOf(
                    GeCmp(indexArg, IntLit(0)),
                    GtCmp(receiver.fieldAccess(ListSizeFieldEmbedding.toViper()), indexArg),
                )
            }
            ifFunctionName("subList") {
                val receiver = receiver!!.toViper()
                val fromIndexArg = formalArgs[1].toViper()
                val toIndexArg = formalArgs[2].toViper()
                return listOf(
                    LeCmp(fromIndexArg, toIndexArg),
                    GeCmp(fromIndexArg, IntLit(0)),
                    LeCmp(toIndexArg, receiver.fieldAccess(ListSizeFieldEmbedding.toViper()))
                )
            }
        }
        return listOf()
    }

fun NamedFunctionSignature.stdLibPostConditions(): List<Exp> =
    NameMatcher.match(this.name) {
        val retVar = this@stdLibPostConditions.returnVar.toViper()
        val receiver = receiver?.toViper()
        ifInCollectionsPkg {
            ifFunctionName("emptyList") {
                return listOf(
                    EqCmp(retVar.fieldAccess(ListSizeFieldEmbedding.toViper()), IntLit(0))
                )
            }
            ifFunctionName("get") {
                return listOf(receiver!!.sameSize())
            }
            ifFunctionName("add") {
                return listOf(receiver!!.increasedSize(1))
            }
            ifFunctionName("isEmpty") {
                return listOf(
                    receiver!!.sameSize(),
                    Implies(retVar, EqCmp(receiver.fieldAccess(ListSizeFieldEmbedding.toViper()), IntLit(0))),
                    Implies(Not(retVar), GtCmp(receiver.fieldAccess(ListSizeFieldEmbedding.toViper()), IntLit(0)))
                )
            }
            ifFunctionName("subList") {
                val fromIndexArg = formalArgs[1].toViper()
                val toIndexArg = formalArgs[2].toViper()
                return listOf(
                    receiver!!.sameSize(),
                    EqCmp(retVar.fieldAccess(ListSizeFieldEmbedding.toViper()), Sub(toIndexArg, fromIndexArg))
                )
            }
        }
        return listOf()
    }