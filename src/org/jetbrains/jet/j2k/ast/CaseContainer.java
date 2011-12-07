package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

/**
 * @author ignatov
 */
public class CaseContainer extends Statement {
  private Statement myCaseStatement;
  private Block myBlock;

  public CaseContainer(final Statement caseStatement, final List<Statement> statements) {
    myCaseStatement = caseStatement;
    List<Statement> newStatements = new LinkedList<Statement>();
    for (Statement s : statements)
      if (s.getKind() != Kind.BREAK && s.getKind() != Kind.CONTINUE )
        newStatements.add(s);
    myBlock = new Block(newStatements);
  }

  @NotNull
  @Override
  public String toKotlin() {
    return myCaseStatement.toKotlin() + SPACE + "=>" + SPACE + myBlock.toKotlin();
  }
}
