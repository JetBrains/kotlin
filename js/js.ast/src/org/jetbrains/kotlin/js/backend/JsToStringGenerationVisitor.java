// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend;

import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.backend.ast.JsDoubleLiteral;
import org.jetbrains.kotlin.js.backend.ast.JsIntLiteral;
import org.jetbrains.kotlin.js.backend.ast.JsVars.JsVar;
import org.jetbrains.kotlin.js.util.TextOutput;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Produces text output from a JavaScript AST.
 */
public class JsToStringGenerationVisitor extends JsVisitor {
    private static final char[] CHARS_BREAK = "break".toCharArray();
    private static final char[] CHARS_CASE = "case".toCharArray();
    private static final char[] CHARS_CATCH = "catch".toCharArray();
    private static final char[] CHARS_CONTINUE = "continue".toCharArray();
    private static final char[] CHARS_DEBUGGER = "debugger".toCharArray();
    private static final char[] CHARS_DEFAULT = "default".toCharArray();
    private static final char[] CHARS_DO = "do".toCharArray();
    private static final char[] CHARS_ELSE = "else".toCharArray();
    private static final char[] CHARS_FALSE = "false".toCharArray();
    private static final char[] CHARS_FINALLY = "finally".toCharArray();
    private static final char[] CHARS_FOR = "for".toCharArray();
    private static final char[] CHARS_FUNCTION = "function".toCharArray();
    private static final char[] CHARS_IF = "if".toCharArray();
    private static final char[] CHARS_IN = "in".toCharArray();
    private static final char[] CHARS_NEW = "new".toCharArray();
    private static final char[] CHARS_NULL = "null".toCharArray();
    private static final char[] CHARS_RETURN = "return".toCharArray();
    private static final char[] CHARS_SWITCH = "switch".toCharArray();
    private static final char[] CHARS_THIS = "this".toCharArray();
    private static final char[] CHARS_THROW = "throw".toCharArray();
    private static final char[] CHARS_TRUE = "true".toCharArray();
    private static final char[] CHARS_TRY = "try".toCharArray();
    private static final char[] CHARS_VAR = "var".toCharArray();
    private static final char[] CHARS_WHILE = "while".toCharArray();
    private static final char[] HEX_DIGITS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static CharSequence javaScriptString(String value) {
        return javaScriptString(value, false);
    }

    /**
     * Generate JavaScript code that evaluates to the supplied string. Adapted
     * from {@link org.mozilla.javascript.ScriptRuntime#escapeString(String)}
     * . The difference is that we quote with either &quot; or &apos; depending on
     * which one is used less inside the string.
     */
    @SuppressWarnings({"ConstantConditions", "UnnecessaryFullyQualifiedName", "JavadocReference"})
    public static CharSequence javaScriptString(CharSequence chars, boolean forceDoubleQuote) {
        final int n = chars.length();
        int quoteCount = 0;
        int aposCount = 0;

        for (int i = 0; i < n; i++) {
            switch (chars.charAt(i)) {
                case '"':
                    ++quoteCount;
                    break;
                case '\'':
                    ++aposCount;
                    break;
            }
        }

        StringBuilder result = new StringBuilder(n + 16);

        char quoteChar = (quoteCount < aposCount || forceDoubleQuote) ? '"' : '\'';
        result.append(quoteChar);

        for (int i = 0; i < n; i++) {
            char c = chars.charAt(i);

            if (' ' <= c && c <= '~' && c != quoteChar && c != '\\') {
                // an ordinary print character (like C isprint())
                result.append(c);
                continue;
            }

            int escape = -1;
            switch (c) {
                case '\b':
                    escape = 'b';
                    break;
                case '\f':
                    escape = 'f';
                    break;
                case '\n':
                    escape = 'n';
                    break;
                case '\r':
                    escape = 'r';
                    break;
                case '\t':
                    escape = 't';
                    break;
                case '"':
                    escape = '"';
                    break; // only reach here if == quoteChar
                case '\'':
                    escape = '\'';
                    break; // only reach here if == quoteChar
                case '\\':
                    escape = '\\';
                    break;
            }

            if (escape >= 0) {
                // an \escaped sort of character
                result.append('\\');
                result.append((char) escape);
            }
            else {
                int hexSize;
                if (c < 256) {
                    // 2-digit hex
                    result.append("\\x");
                    hexSize = 2;
                }
                else {
                    // Unicode.
                    result.append("\\u");
                    hexSize = 4;
                }
                // append hexadecimal form of ch left-padded with 0
                for (int shift = (hexSize - 1) * 4; shift >= 0; shift -= 4) {
                    int digit = 0xf & (c >> shift);
                    result.append(HEX_DIGITS[digit]);
                }
            }
        }
        result.append(quoteChar);
        escapeClosingTags(result);
        return result;
    }

    /**
     * Escapes any closing XML tags embedded in <code>str</code>, which could
     * potentially cause a parse failure in a browser, for example, embedding a
     * closing <code>&lt;script&gt;</code> tag.
     *
     * @param str an unescaped literal; May be null
     */
    private static void escapeClosingTags(StringBuilder str) {
        if (str == null) {
            return;
        }

        int index = 0;
        while ((index = str.indexOf("</", index)) != -1) {
            str.insert(index + 1, '\\');
        }
    }

    protected boolean needSemi = true;
    private boolean lineBreakAfterBlock = true;

    /**
     * "Global" blocks are either the global block of a fragment, or a block
     * nested directly within some other global block. This definition matters
     * because the statements designated by statementEnds and statementStarts are
     * those that appear directly within these global blocks.
     */
    private Set<JsBlock> globalBlocks = new THashSet<JsBlock>();
    protected final TextOutput p;

    public JsToStringGenerationVisitor(TextOutput out) {
        p = out;
    }

    @Override
    public void visitArrayAccess(@NotNull JsArrayAccess x) {
        printPair(x, x.getArrayExpression());
        leftSquare();
        accept(x.getIndexExpression());
        rightSquare();
    }

    @Override
    public void visitArray(@NotNull JsArrayLiteral x) {
        leftSquare();
        printExpressions(x.getExpressions());
        rightSquare();
    }

    private void printExpressions(List<JsExpression> expressions) {
        boolean notFirst = false;
        for (JsExpression expression : expressions) {
            notFirst = sepCommaOptSpace(notFirst) && !(expression instanceof JsDocComment);
            boolean isEnclosed = parenPushIfCommaExpression(expression);
            accept(expression);
            if (isEnclosed) {
                rightParen();
            }
        }
    }

    @Override
    public void visitBinaryExpression(@NotNull JsBinaryOperation binaryOperation) {
        JsBinaryOperator operator = binaryOperation.getOperator();
        JsExpression arg1 = binaryOperation.getArg1();
        boolean isExpressionEnclosed = parenPush(binaryOperation, arg1, !operator.isLeftAssociative());

        accept(arg1);
        if (operator.isKeyword()) {
            _parenPopOrSpace(binaryOperation, arg1, !operator.isLeftAssociative());
        }
        else if (operator != JsBinaryOperator.COMMA) {
            if (isExpressionEnclosed) {
                rightParen();
            }
            spaceOpt();
        }

        p.print(operator.getSymbol());

        JsExpression arg2 = binaryOperation.getArg2();
        boolean isParenOpened;
        if (operator == JsBinaryOperator.COMMA) {
            isParenOpened = false;
            spaceOpt();
        }
        else if (arg2 instanceof JsBinaryOperation && ((JsBinaryOperation) arg2).getOperator() == JsBinaryOperator.AND) {
            spaceOpt();
            leftParen();
            isParenOpened = true;
        }
        else {
            if (spaceCalc(operator, arg2)) {
                isParenOpened = _parenPushOrSpace(binaryOperation, arg2, operator.isLeftAssociative());
            }
            else {
                spaceOpt();
                isParenOpened = parenPush(binaryOperation, arg2, operator.isLeftAssociative());
            }
        }
        accept(arg2);
        if (isParenOpened) {
            rightParen();
        }
    }

    @Override
    public void visitBlock(@NotNull JsBlock x) {
        printJsBlock(x, true);
    }

    @Override
    public void visitBoolean(@NotNull JsBooleanLiteral x) {
        if (x.getValue()) {
            p.print(CHARS_TRUE);
        }
        else {
            p.print(CHARS_FALSE);
        }
    }

    @Override
    public void visitBreak(@NotNull JsBreak x) {
        p.print(CHARS_BREAK);
        continueOrBreakLabel(x);
    }

    @Override
    public void visitContinue(@NotNull JsContinue x) {
        p.print(CHARS_CONTINUE);
        continueOrBreakLabel(x);
    }

    private void continueOrBreakLabel(JsContinue x) {
        JsNameRef label = x.getLabel();
        if (label != null && label.getIdent() != null) {
            space();
            p.print(label.getIdent());
        }
    }

    @Override
    public void visitCase(@NotNull JsCase x) {
        p.print(CHARS_CASE);
        space();
        accept(x.getCaseExpression());
        _colon();
        newlineOpt();

        printSwitchMemberStatements(x);
    }

    private void printSwitchMemberStatements(JsSwitchMember x) {
        p.indentIn();
        for (JsStatement stmt : x.getStatements()) {
            needSemi = true;
            accept(stmt);
            if (needSemi) {
                semi();
            }
            newlineOpt();
        }
        p.indentOut();
        needSemi = false;
    }

    @Override
    public void visitCatch(@NotNull JsCatch x) {
        spaceOpt();
        p.print(CHARS_CATCH);
        spaceOpt();
        leftParen();
        nameDef(x.getParameter().getName());

        // Optional catch condition.
        //
        JsExpression catchCond = x.getCondition();
        if (catchCond != null) {
            space();
            _if();
            space();
            accept(catchCond);
        }

        rightParen();
        spaceOpt();
        accept(x.getBody());
    }

    @Override
    public void visitConditional(@NotNull JsConditional x) {
        // Associativity: for the then and else branches, it is safe to insert
        // another
        // ternary expression, but if the test expression is a ternary, it should
        // get parentheses around it.
        printPair(x, x.getTestExpression(), true);
        spaceOpt();
        p.print('?');
        spaceOpt();
        printPair(x, x.getThenExpression());
        spaceOpt();
        _colon();
        spaceOpt();
        printPair(x, x.getElseExpression());
    }

    private void printPair(JsExpression parent, JsExpression expression, boolean wrongAssoc) {
        boolean isNeedParen = parenCalc(parent, expression, wrongAssoc);
        if (isNeedParen) {
            leftParen();
        }
        accept(expression);
        if (isNeedParen) {
            rightParen();
        }
    }

    private void printPair(JsExpression parent, JsExpression expression) {
        printPair(parent, expression, false);
    }

    @Override
    public void visitDebugger(@NotNull JsDebugger x) {
        p.print(CHARS_DEBUGGER);
    }

    @Override
    public void visitDefault(@NotNull JsDefault x) {
        p.print(CHARS_DEFAULT);
        _colon();

        printSwitchMemberStatements(x);
    }

    @Override
    public void visitWhile(@NotNull JsWhile x) {
        _while();
        spaceOpt();
        leftParen();
        accept(x.getCondition());
        rightParen();
        nestedPush(x.getBody());
        accept(x.getBody());
        nestedPop(x.getBody());
    }

    @Override
    public void visitDoWhile(@NotNull JsDoWhile x) {
        p.print(CHARS_DO);
        nestedPush(x.getBody());
        accept(x.getBody());
        nestedPop(x.getBody());
        if (needSemi) {
            semi();
            newlineOpt();
        }
        else {
            spaceOpt();
            needSemi = true;
        }
        _while();
        spaceOpt();
        leftParen();
        accept(x.getCondition());
        rightParen();
    }

    @Override
    public void visitEmpty(@NotNull JsEmpty x) {
    }

    @Override
    public void visitExpressionStatement(@NotNull JsExpressionStatement x) {
        boolean surroundWithParentheses = JsFirstExpressionVisitor.exec(x);
        if (surroundWithParentheses) {
            leftParen();
        }
        accept(x.getExpression());
        if (surroundWithParentheses) {
            rightParen();
        }
    }

    @Override
    public void visitFor(@NotNull JsFor x) {
        _for();
        spaceOpt();
        leftParen();

        // The init expressions or var decl.
        //
        if (x.getInitExpression() != null) {
            accept(x.getInitExpression());
        }
        else if (x.getInitVars() != null) {
            accept(x.getInitVars());
        }

        semi();

        // The loop test.
        //
        if (x.getCondition() != null) {
            spaceOpt();
            accept(x.getCondition());
        }

        semi();

        // The incr expression.
        //
        if (x.getIncrementExpression() != null) {
            spaceOpt();
            accept(x.getIncrementExpression());
        }

        rightParen();
        nestedPush(x.getBody());
        if (x.getBody() != null) {
            accept(x.getBody());
        }
        nestedPop(x.getBody());
    }

    @Override
    public void visitForIn(@NotNull JsForIn x) {
        _for();
        spaceOpt();
        leftParen();

        if (x.getIterVarName() != null) {
            var();
            space();
            nameDef(x.getIterVarName());

            if (x.getIterExpression() != null) {
                spaceOpt();
                assignment();
                spaceOpt();
                accept(x.getIterExpression());
            }
        }
        else {
            // Just a name ref.
            //
            accept(x.getIterExpression());
        }

        space();
        p.print(CHARS_IN);
        space();
        accept(x.getObjectExpression());

        rightParen();
        nestedPush(x.getBody());
        accept(x.getBody());
        nestedPop(x.getBody());
    }

    @Override
    public void visitFunction(@NotNull JsFunction x) {
        p.print(CHARS_FUNCTION);
        space();
        if (x.getName() != null) {
            nameOf(x);
        }

        leftParen();
        boolean notFirst = false;
        for (Object element : x.getParameters()) {
            JsParameter param = (JsParameter) element;
            notFirst = sepCommaOptSpace(notFirst);
            accept(param);
        }
        rightParen();
        space();

        lineBreakAfterBlock = false;
        accept(x.getBody());
        needSemi = true;
    }

    @Override
    public void visitIf(@NotNull JsIf x) {
        _if();
        spaceOpt();
        leftParen();
        accept(x.getIfExpression());
        rightParen();
        JsStatement thenStmt = x.getThenStatement();
        JsStatement elseStatement = x.getElseStatement();
        if (elseStatement != null && thenStmt instanceof JsIf && ((JsIf)thenStmt).getElseStatement() == null) {
            thenStmt = new JsBlock(thenStmt);
        }
        nestedPush(thenStmt);
        accept(thenStmt);
        nestedPop(thenStmt);
        if (elseStatement != null) {
            if (needSemi) {
                semi();
                newlineOpt();
            }
            else {
                spaceOpt();
                needSemi = true;
            }
            p.print(CHARS_ELSE);
            boolean elseIf = elseStatement instanceof JsIf;
            if (!elseIf) {
                nestedPush(elseStatement);
            }
            else {
                space();
            }
            accept(elseStatement);
            if (!elseIf) {
                nestedPop(elseStatement);
            }
        }
    }

    @Override
    public void visitInvocation(@NotNull JsInvocation invocation) {
        printPair(invocation, invocation.getQualifier());

        leftParen();
        printExpressions(invocation.getArguments());
        rightParen();
    }

    @Override
    public void visitLabel(@NotNull JsLabel x) {
        nameOf(x);
        _colon();
        spaceOpt();
        accept(x.getStatement());
    }

    @Override
    public void visitNameRef(@NotNull JsNameRef nameRef) {
        JsExpression qualifier = nameRef.getQualifier();
        if (qualifier != null) {
            final boolean enclose;
            if (qualifier instanceof JsLiteral.JsValueLiteral) {
                // "42.foo" is not allowed, but "(42).foo" is.
                enclose = qualifier instanceof JsNumberLiteral;
            }
            else {
                enclose = parenCalc(nameRef, qualifier, false);
            }

            if (enclose) {
                leftParen();
            }
            accept(qualifier);
            if (enclose) {
                rightParen();
            }
            p.print('.');
        }

        p.maybeIndent();
        p.print(nameRef.getIdent());
    }

    @Override
    public void visitNew(@NotNull JsNew x) {
        p.print(CHARS_NEW);
        space();

        JsExpression constructorExpression = x.getConstructorExpression();
        boolean needsParens = JsConstructExpressionVisitor.exec(constructorExpression);
        if (needsParens) {
            leftParen();
        }
        accept(constructorExpression);
        if (needsParens) {
            rightParen();
        }

        leftParen();
        printExpressions(x.getArguments());
        rightParen();
    }

    @Override
    public void visitNull(@NotNull JsNullLiteral x) {
        p.print(CHARS_NULL);
    }

    @Override
    public void visitInt(@NotNull JsIntLiteral x) {
        p.print(x.value);
    }

    @Override
    public void visitDouble(@NotNull JsDoubleLiteral x) {
        p.print(x.value);
    }

    @Override
    public void visitObjectLiteral(@NotNull JsObjectLiteral objectLiteral) {
        p.print('{');
        if (objectLiteral.isMultiline()) {
            p.indentIn();
        }

        boolean notFirst = false;
        for (JsPropertyInitializer item : objectLiteral.getPropertyInitializers()) {
            if (notFirst) {
                p.print(',');
            }

            if (objectLiteral.isMultiline()) {
                newlineOpt();
            }
            else if (notFirst) {
                spaceOpt();
            }

            notFirst = true;

            JsExpression labelExpr = item.getLabelExpr();
            // labels can be either string, integral, or decimal literals
            if (labelExpr instanceof JsNameRef) {
                p.print(((JsNameRef) labelExpr).getIdent());
            }
            else if (labelExpr instanceof JsStringLiteral) {
                p.print(((JsStringLiteral) labelExpr).getValue());
            }
            else {
                accept(labelExpr);
            }

            _colon();
            space();
            JsExpression valueExpr = item.getValueExpr();
            boolean wasEnclosed = parenPushIfCommaExpression(valueExpr);
            accept(valueExpr);
            if (wasEnclosed) {
                rightParen();
            }
        }

        if (objectLiteral.isMultiline()) {
            p.indentOut();
            newlineOpt();
        }

        p.print('}');
    }

    @Override
    public void visitParameter(@NotNull JsParameter x) {
        nameOf(x);
    }

    @Override
    public void visitPostfixOperation(@NotNull JsPostfixOperation x) {
        JsUnaryOperator op = x.getOperator();
        JsExpression arg = x.getArg();
        // unary operators always associate correctly (I think)
        printPair(x, arg);
        p.print(op.getSymbol());
    }

    @Override
    public void visitPrefixOperation(@NotNull JsPrefixOperation x) {
        JsUnaryOperator op = x.getOperator();
        p.print(op.getSymbol());
        JsExpression arg = x.getArg();
        if (spaceCalc(op, arg)) {
            space();
        }
        // unary operators always associate correctly (I think)
        printPair(x, arg);
    }

    @Override
    public void visitProgram(@NotNull JsProgram x) {
        p.print("<JsProgram>");
    }

    @Override
    public void visitRegExp(@NotNull JsRegExp x) {
        slash();
        p.print(x.getPattern());
        slash();
        String flags = x.getFlags();
        if (flags != null) {
            p.print(flags);
        }
    }

    @Override
    public void visitReturn(@NotNull JsReturn x) {
        p.print(CHARS_RETURN);
        JsExpression expr = x.getExpression();
        if (expr != null) {
            space();
            accept(expr);
        }
    }

    @Override
    public void visitString(@NotNull JsStringLiteral x) {
        p.print(javaScriptString(x.getValue()));
    }

    @Override
    public void visit(@NotNull JsSwitch x) {
        p.print(CHARS_SWITCH);
        spaceOpt();
        leftParen();
        accept(x.getExpression());
        rightParen();
        spaceOpt();
        blockOpen();
        acceptList(x.getCases());
        blockClose();
    }

    @Override
    public void visitThis(@NotNull JsThisRef x) {
        p.print(CHARS_THIS);
    }

    @Override
    public void visitThrow(@NotNull JsThrow x) {
        p.print(CHARS_THROW);
        space();
        accept(x.getExpression());
    }

    @Override
    public void visitTry(@NotNull JsTry x) {
        p.print(CHARS_TRY);
        spaceOpt();
        accept(x.getTryBlock());

        acceptList(x.getCatches());

        JsBlock finallyBlock = x.getFinallyBlock();
        if (finallyBlock != null) {
            p.print(CHARS_FINALLY);
            spaceOpt();
            accept(finallyBlock);
        }
    }

    @Override
    public void visit(@NotNull JsVar var) {
        nameOf(var);
        JsExpression initExpr = var.getInitExpression();
        if (initExpr != null) {
            spaceOpt();
            assignment();
            spaceOpt();
            boolean isEnclosed = parenPushIfCommaExpression(initExpr);
            accept(initExpr);
            if (isEnclosed) {
                rightParen();
            }
        }
    }

    @Override
    public void visitVars(@NotNull JsVars vars) {
        var();
        space();
        boolean sep = false;
        for (JsVar var : vars) {
            if (sep) {
                if (vars.isMultiline()) {
                    newlineOpt();
                }
                p.print(',');
                spaceOpt();
            }
            else {
                sep = true;
            }

            accept(var);
        }
    }

    @Override
    public void visitDocComment(@NotNull JsDocComment comment) {
        boolean asSingleLine = comment.getTags().size() == 1;
        if (!asSingleLine) {
            newlineOpt();
        }
        p.print("/**");
        if (asSingleLine) {
            space();
        }
        else {
            p.newline();
        }

        boolean notFirst = false;
        for (Map.Entry<String, Object> entry : comment.getTags().entrySet()) {
            if (notFirst) {
                p.newline();
                p.print(' ');
                p.print('*');
            }
            else {
                notFirst = true;
            }

            p.print('@');
            p.print(entry.getKey());
            Object value = entry.getValue();
            if (value != null) {
                space();
                if (value instanceof CharSequence) {
                    p.print((CharSequence) value);
                }
                else {
                    visitNameRef((JsNameRef) value);
                }
            }

            if (!asSingleLine) {
                p.newline();
            }
        }

        if (asSingleLine) {
            space();
        }
        else {
            newlineOpt();
        }

        p.print('*');
        p.print('/');
        if (asSingleLine) {
            spaceOpt();
        }
    }

    protected final void newlineOpt() {
        if (!p.isCompact()) {
            p.newline();
        }
    }

    protected void printJsBlock(JsBlock x, boolean finalNewline) {
        if (!lineBreakAfterBlock) {
            finalNewline = false;
            lineBreakAfterBlock = true;
        }

        boolean needBraces = !x.isGlobalBlock();
        if (needBraces) {
            blockOpen();
        }

        Iterator<JsStatement> iterator = x.getStatements().iterator();
        while (iterator.hasNext()) {
            boolean isGlobal = x.isGlobalBlock() || globalBlocks.contains(x);

            JsStatement statement = iterator.next();
            if (statement instanceof JsEmpty) {
                continue;
            }

            needSemi = true;
            boolean stmtIsGlobalBlock = false;
            if (isGlobal) {
                if (statement instanceof JsBlock) {
                    // A block inside a global block is still considered global
                    stmtIsGlobalBlock = true;
                    globalBlocks.add((JsBlock) statement);
                }
            }

            accept(statement);
            if (stmtIsGlobalBlock) {
                //noinspection SuspiciousMethodCalls
                globalBlocks.remove(statement);
            }
            if (needSemi) {
                /*
                * Special treatment of function declarations: If they are the only item in a
                * statement (i.e. not part of an assignment operation), just give them
                * a newline instead of a semi.
                */
                boolean functionStmt =
                        statement instanceof JsExpressionStatement && ((JsExpressionStatement) statement).getExpression() instanceof JsFunction;
                /*
                * Special treatment of the last statement in a block: only a few
                * statements at the end of a block require semicolons.
                */
                boolean lastStatement = !iterator.hasNext() && needBraces && !JsRequiresSemiVisitor.exec(statement);
                if (functionStmt) {
                    if (lastStatement) {
                        newlineOpt();
                    }
                    else {
                        p.newline();
                    }
                }
                else {
                    if (lastStatement) {
                        p.printOpt(';');
                    }
                    else {
                        semi();
                    }
                    newlineOpt();
                }
            }
        }

        if (needBraces) {
            // _blockClose() modified
            p.indentOut();
            p.print('}');
            if (finalNewline) {
                newlineOpt();
            }
        }
        needSemi = false;
    }

    private void assignment() {
        p.print('=');
    }

    private void blockClose() {
        p.indentOut();
        p.print('}');
        newlineOpt();
    }

    private void blockOpen() {
        p.print('{');
        p.indentIn();
        newlineOpt();
    }

    private void _colon() {
        p.print(':');
    }

    private void _for() {
        p.print(CHARS_FOR);
    }

    private void _if() {
        p.print(CHARS_IF);
    }

    private void leftParen() {
        p.print('(');
    }

    private void leftSquare() {
        p.print('[');
    }

    private void nameDef(JsName name) {
        p.print(name.getIdent());
    }

    private void nameOf(HasName hasName) {
        nameDef(hasName.getName());
    }

    private boolean nestedPop(JsStatement statement) {
        boolean pop = !(statement instanceof JsBlock);
        if (pop) {
            p.indentOut();
        }
        return pop;
    }

    private boolean nestedPush(JsStatement statement) {
        boolean push = !(statement instanceof JsBlock);
        if (push) {
            newlineOpt();
            p.indentIn();
        }
        else {
            spaceOpt();
        }
        return push;
    }

    private static boolean parenCalc(JsExpression parent, JsExpression child, boolean wrongAssoc) {
        int parentPrec = JsPrecedenceVisitor.exec(parent);
        int childPrec = JsPrecedenceVisitor.exec(child);
        return parentPrec > childPrec || parentPrec == childPrec && wrongAssoc;
    }

    private boolean _parenPopOrSpace(JsExpression parent, JsExpression child, boolean wrongAssoc) {
        boolean doPop = parenCalc(parent, child, wrongAssoc);
        if (doPop) {
            rightParen();
        }
        else {
            space();
        }
        return doPop;
    }

    private boolean parenPush(JsExpression parent, JsExpression child, boolean wrongAssoc) {
        boolean doPush = parenCalc(parent, child, wrongAssoc);
        if (doPush) {
            leftParen();
        }
        return doPush;
    }

    private boolean parenPushIfCommaExpression(JsExpression x) {
        boolean doPush = x instanceof JsBinaryOperation && ((JsBinaryOperation) x).getOperator() == JsBinaryOperator.COMMA;
        if (doPush) {
            leftParen();
        }
        return doPush;
    }

    private boolean _parenPushOrSpace(JsExpression parent, JsExpression child, boolean wrongAssoc) {
        boolean doPush = parenCalc(parent, child, wrongAssoc);
        if (doPush) {
            leftParen();
        }
        else {
            space();
        }
        return doPush;
    }

    private void rightParen() {
        p.print(')');
    }

    private void rightSquare() {
        p.print(']');
    }

    private void semi() {
        p.print(';');
    }

    private boolean sepCommaOptSpace(boolean sep) {
        if (sep) {
            p.print(',');
            spaceOpt();
        }
        return true;
    }

    private void slash() {
        p.print('/');
    }

    private void space() {
        p.print(' ');
    }

    /**
     * Decide whether, if <code>op</code> is printed followed by <code>arg</code>,
     * there needs to be a space between the operator and expression.
     *
     * @return <code>true</code> if a space needs to be printed
     */
    private static boolean spaceCalc(JsOperator op, JsExpression arg) {
        if (op.isKeyword()) {
            return true;
        }
        if (arg instanceof JsBinaryOperation) {
            JsBinaryOperation binary = (JsBinaryOperation) arg;
            /*
            * If the binary operation has a higher precedence than op, then it won't
            * be parenthesized, so check the first argument of the binary operation.
            */
            return binary.getOperator().getPrecedence() > op.getPrecedence() && spaceCalc(op, binary.getArg1());
        }
        if (arg instanceof JsPrefixOperation) {
            JsOperator op2 = ((JsPrefixOperation) arg).getOperator();
            return (op == JsBinaryOperator.SUB || op == JsUnaryOperator.NEG)
                   && (op2 == JsUnaryOperator.DEC || op2 == JsUnaryOperator.NEG)
                   || (op == JsBinaryOperator.ADD && op2 == JsUnaryOperator.INC);
        }
        if (arg instanceof JsNumberLiteral && (op == JsBinaryOperator.SUB || op == JsUnaryOperator.NEG)) {
            if (arg instanceof JsIntLiteral) {
                return ((JsIntLiteral) arg).value < 0;
            }
            else {
                assert arg instanceof JsDoubleLiteral;
                //noinspection CastConflictsWithInstanceof
                return ((JsDoubleLiteral) arg).value < 0;
            }
        }
        return false;
    }

    private void spaceOpt() {
        p.printOpt(' ');
    }

    private void var() {
        p.print(CHARS_VAR);
    }

    private void _while() {
        p.print(CHARS_WHILE);
    }
}
