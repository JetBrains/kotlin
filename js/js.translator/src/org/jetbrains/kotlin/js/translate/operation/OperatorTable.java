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
import com.google.dart.compiler.backend.js.ast.JsBinaryOperator;
import com.google.dart.compiler.backend.js.ast.JsUnaryOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Map;

public final class OperatorTable {

    //TODO : not all operators , add and test bit operators
    private static final Map<JetToken, JsBinaryOperator> binaryOperatorsMap = ImmutableBiMap.<JetToken, JsBinaryOperator>builder()
            .put(JetTokens.PLUS, JsBinaryOperator.ADD)
            .put(JetTokens.MINUS, JsBinaryOperator.SUB)
            .put(JetTokens.MUL, JsBinaryOperator.MUL)
            .put(JetTokens.DIV, JsBinaryOperator.DIV)
            .put(JetTokens.EQ, JsBinaryOperator.ASG)
            .put(JetTokens.GT, JsBinaryOperator.GT)
            .put(JetTokens.GTEQ, JsBinaryOperator.GTE)
            .put(JetTokens.LT, JsBinaryOperator.LT)
            .put(JetTokens.LTEQ, JsBinaryOperator.LTE)
            .put(JetTokens.ANDAND, JsBinaryOperator.AND)
            .put(JetTokens.OROR, JsBinaryOperator.OR)
            .put(JetTokens.PERC, JsBinaryOperator.MOD)
            .put(JetTokens.PLUSEQ, JsBinaryOperator.ASG_ADD)
            .put(JetTokens.MINUSEQ, JsBinaryOperator.ASG_SUB)
            .put(JetTokens.DIVEQ, JsBinaryOperator.ASG_DIV)
            .put(JetTokens.MULTEQ, JsBinaryOperator.ASG_MUL)
            .put(JetTokens.PERCEQ, JsBinaryOperator.ASG_MOD)
            .put(JetTokens.IN_KEYWORD, JsBinaryOperator.INOP)
            .put(JetTokens.EQEQEQ, JsBinaryOperator.REF_EQ)
            .put(JetTokens.EXCLEQEQEQ, JsBinaryOperator.REF_NEQ)
            .build();

    private static final ImmutableBiMap<JetToken, JsUnaryOperator> unaryOperatorsMap = ImmutableBiMap.<JetToken, JsUnaryOperator>builder()
            .put(JetTokens.PLUSPLUS, JsUnaryOperator.INC)
            .put(JetTokens.MINUSMINUS, JsUnaryOperator.DEC)
            .put(JetTokens.EXCL, JsUnaryOperator.NOT)
            .put(JetTokens.MINUS, JsUnaryOperator.NEG)
            .put(JetTokens.PLUS, JsUnaryOperator.POS)
            .build();

    private OperatorTable() {
    }

    public static boolean hasCorrespondingOperator(@NotNull JetToken token) {
        return binaryOperatorsMap.containsKey(token) || unaryOperatorsMap.containsKey(token);
    }

    public static boolean hasCorrespondingBinaryOperator(@NotNull JetToken token) {
        return binaryOperatorsMap.containsKey(token);
    }

    @NotNull
    static public JsBinaryOperator getBinaryOperator(@NotNull JetToken token) {
        assert JetTokens.OPERATIONS.contains(token) : "Token should represent an operation!";
        return binaryOperatorsMap.get(token);
    }

    @NotNull
    static public JsUnaryOperator getUnaryOperator(@NotNull JetToken token) {
        assert JetTokens.OPERATIONS.contains(token) : "Token should represent an operation!";
        return unaryOperatorsMap.get(token);
    }
}
