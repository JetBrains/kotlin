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

package org.jetbrains.kotlin.idea.highlighter;

import com.intellij.lexer.LayeredLexer;
import com.intellij.lexer.StringLiteralLexer;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.kdoc.lexer.KDocLexer;
import org.jetbrains.kotlin.lexer.KotlinLexer;
import org.jetbrains.kotlin.lexer.KtTokens;

public class KotlinHighlightingLexer extends LayeredLexer {
    public KotlinHighlightingLexer() {
        super(new KotlinLexer());

        registerSelfStoppingLayer(new KDocLexer(), new IElementType[]{KtTokens.DOC_COMMENT}, IElementType.EMPTY_ARRAY);
        registerSelfStoppingLayer(new StringLiteralLexer('\'', KtTokens.CHARACTER_LITERAL),
                                  new IElementType[]{KtTokens.CHARACTER_LITERAL}, IElementType.EMPTY_ARRAY);


    }
}
