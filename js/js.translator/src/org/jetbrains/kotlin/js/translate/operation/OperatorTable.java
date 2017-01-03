/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.translate.operation;

import com.google.common.collect.ImmutableBiMap;
import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperator;
import org.jetbrains.kotlin.js.backend.ast.JsUnaryOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.lexer.KtToken;
import org.jetbrains.kotlin.lexer.KtTokens;

import java.util.Map;

public final class OperatorTable {

    //TODO : not all operators , add and test bit operators
    private static final Map<KtToken, JsBinaryOperator> binaryOperatorsMap = ImmutableBiMap.<KtToken, JsBinaryOperator>builder()
            .put(KtTokens.PLUS, JsBinaryOperator.ADD)
            .put(KtTokens.MINUS, JsBinaryOperator.SUB)
            .put(KtTokens.MUL, JsBinaryOperator.MUL)
            .put(KtTokens.DIV, JsBinaryOperator.DIV)
            .put(KtTokens.EQ, JsBinaryOperator.ASG)
            .put(KtTokens.GT, JsBinaryOperator.GT)
            .put(KtTokens.GTEQ, JsBinaryOperator.GTE)
            .put(KtTokens.LT, JsBinaryOperator.LT)
            .put(KtTokens.LTEQ, JsBinaryOperator.LTE)
            .put(KtTokens.ANDAND, JsBinaryOperator.AND)
            .put(KtTokens.OROR, JsBinaryOperator.OR)
            .put(KtTokens.PERC, JsBinaryOperator.MOD)
            .put(KtTokens.PLUSEQ, JsBinaryOperator.ASG_ADD)
            .put(KtTokens.MINUSEQ, JsBinaryOperator.ASG_SUB)
            .put(KtTokens.DIVEQ, JsBinaryOperator.ASG_DIV)
            .put(KtTokens.MULTEQ, JsBinaryOperator.ASG_MUL)
            .put(KtTokens.PERCEQ, JsBinaryOperator.ASG_MOD)
            .put(KtTokens.IN_KEYWORD, JsBinaryOperator.INOP)
            .put(KtTokens.EQEQEQ, JsBinaryOperator.REF_EQ)
            .put(KtTokens.EXCLEQEQEQ, JsBinaryOperator.REF_NEQ)
            .build();

    private static final ImmutableBiMap<KtToken, JsUnaryOperator> unaryOperatorsMap = ImmutableBiMap.<KtToken, JsUnaryOperator>builder()
            .put(KtTokens.PLUSPLUS, JsUnaryOperator.INC)
            .put(KtTokens.MINUSMINUS, JsUnaryOperator.DEC)
            .put(KtTokens.EXCL, JsUnaryOperator.NOT)
            .put(KtTokens.MINUS, JsUnaryOperator.NEG)
            .put(KtTokens.PLUS, JsUnaryOperator.POS)
            .build();

    private OperatorTable() {
    }

    public static boolean hasCorrespondingOperator(@NotNull KtToken token) {
        return binaryOperatorsMap.containsKey(token) || unaryOperatorsMap.containsKey(token);
    }

    public static boolean hasCorrespondingBinaryOperator(@NotNull KtToken token) {
        return binaryOperatorsMap.containsKey(token);
    }

    @NotNull
    static public JsBinaryOperator getBinaryOperator(@NotNull KtToken token) {
        assert KtTokens.OPERATIONS.contains(token) : "Token should represent an operation!";
        return binaryOperatorsMap.get(token);
    }

    @NotNull
    static public JsUnaryOperator getUnaryOperator(@NotNull KtToken token) {
        assert KtTokens.OPERATIONS.contains(token) : "Token should represent an operation!";
        return unaryOperatorsMap.get(token);
    }
}
