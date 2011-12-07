package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * @author ignatov
 */
public class CaseContainer extends Statement {
  private List<Statement> myCaseStatement;
  private Block myBlock;

  public CaseContainer(final List<Statement> caseStatement, final List<Statement> statements) {
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
    return AstUtil.joinNodes(myCaseStatement, COMMA_WITH_SPACE) + SPACE + "=>" + SPACE + myBlock.toKotlin();
  }
}
