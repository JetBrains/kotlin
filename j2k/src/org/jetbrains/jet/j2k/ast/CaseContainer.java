/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.LinkedList;
import java.util.List;

public class CaseContainer extends Statement {
    private final List<Statement> myCaseStatement;
    private final Block myBlock;

    public CaseContainer(List<Statement> caseStatement, @NotNull List<Statement> statements) {
        myCaseStatement = caseStatement;
        List<Statement> newStatements = new LinkedList<Statement>();
        for (Statement s : statements)
            if (s.getKind() != Kind.BREAK && s.getKind() != Kind.CONTINUE) {
                newStatements.add(s);
            }
        myBlock = new Block(newStatements);
    }

    @NotNull
    @Override
    public String toKotlin() {
        return AstUtil.joinNodes(myCaseStatement, COMMA_WITH_SPACE) + SPACE + "->" + SPACE + myBlock.toKotlin();
    }
}
