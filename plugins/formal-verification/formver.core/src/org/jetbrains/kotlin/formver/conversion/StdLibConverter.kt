/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.formver.embeddings.ListSizeFieldEmbedding
import org.jetbrains.kotlin.formver.embeddings.callables.NamedFunctionSignature
import org.jetbrains.kotlin.formver.embeddings.expression.*
import org.jetbrains.kotlin.formver.names.NameMatcher

fun VariableEmbedding.sameSize(): ExpEmbedding =
    EqCmp(FieldAccess(this, ListSizeFieldEmbedding), Old(FieldAccess(this, ListSizeFieldEmbedding)))

fun VariableEmbedding.increasedSize(amount: Int): ExpEmbedding =
    EqCmp(FieldAccess(this, ListSizeFieldEmbedding), Add(Old(FieldAccess(this, ListSizeFieldEmbedding)), IntLit(amount)))

fun NamedFunctionSignature.stdLibPreConditions(): List<ExpEmbedding> =
    NameMatcher.match(this.name) {
        ifInCollectionsPkg {
            ifFunctionName("get") {
                val receiver = receiver!!
                val indexArg = formalArgs[1]
                return listOf(
                    GeCmp(indexArg, IntLit(0)),
                    GtCmp(FieldAccess(receiver, ListSizeFieldEmbedding), indexArg),
                )
            }
            ifFunctionName("subList") {
                val receiver = receiver!!
                val fromIndexArg = formalArgs[1]
                val toIndexArg = formalArgs[2]
                return listOf(
                    LeCmp(fromIndexArg, toIndexArg),
                    GeCmp(fromIndexArg, IntLit(0)),
                    LeCmp(toIndexArg, FieldAccess(receiver, ListSizeFieldEmbedding))
                )
            }
        }
        return listOf()
    }

fun NamedFunctionSignature.stdLibPostConditions(returnVariable: VariableEmbedding): List<ExpEmbedding> =
    NameMatcher.match(this.name) {
        val receiver = receiver
        ifInCollectionsPkg {
            ifFunctionName("emptyList") {
                return listOf(
                    EqCmp(FieldAccess(returnVariable, ListSizeFieldEmbedding), IntLit(0))
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
                    Implies(returnVariable, EqCmp(FieldAccess(receiver, ListSizeFieldEmbedding), IntLit(0))),
                    Implies(Not(returnVariable), GtCmp(FieldAccess(receiver, ListSizeFieldEmbedding), IntLit(0)))
                )
            }
            ifFunctionName("subList") {
                val fromIndexArg = formalArgs[1]
                val toIndexArg = formalArgs[2]
                return listOf(
                    receiver!!.sameSize(),
                    EqCmp(FieldAccess(returnVariable, ListSizeFieldEmbedding), Sub(toIndexArg, fromIndexArg))
                )
            }
        }
        return listOf()
    }