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

import java.util.*;

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
    @NotNull
    private final SourceLocationConsumer sourceLocationConsumer;

    @NotNull
    private final List<Object> sourceInfoStack = new ArrayList<>();

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
        int n = chars.length();
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

    @NotNull
    protected final TextOutput p;

    public JsToStringGenerationVisitor(@NotNull TextOutput out, @NotNull SourceLocationConsumer sourceLocationConsumer) {
        p = out;
        this.sourceLocationConsumer = sourceLocationConsumer;
    }

    public JsToStringGenerationVisitor(@NotNull TextOutput out) {
        this(out, NoOpSourceLocationConsumer.INSTANCE);
    }

    @Override
    public void visitArrayAccess(@NotNull JsArrayAccess x) {
        pushSourceInfo(x.getSource());

        printPair(x, x.getArrayExpression());
        leftSquare();
        accept(x.getIndexExpression());
        rightSquare();

        popSourceInfo();
    }

    @Override
    public void visitArray(@NotNull JsArrayLiteral x) {
        pushSourceInfo(x.getSource());

        leftSquare();
        printExpressions(x.getExpressions());
        rightSquare();

        popSourceInfo();
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
        pushSourceInfo(binaryOperation.getSource());

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

        popSourceInfo();
    }

    @Override
    public void visitBlock(@NotNull JsBlock x) {
        printJsBlock(x, true, null);
    }

    @Override
    public void visitBoolean(@NotNull JsBooleanLiteral x) {
        pushSourceInfo(x.getSource());

        if (x.getValue()) {
            p.print(CHARS_TRUE);
        }
        else {
            p.print(CHARS_FALSE);
        }

        popSourceInfo();
    }

    @Override
    public void visitBreak(@NotNull JsBreak x) {
        pushSourceInfo(x.getSource());

        p.print(CHARS_BREAK);
        continueOrBreakLabel(x);

        popSourceInfo();
    }

    @Override
    public void visitContinue(@NotNull JsContinue x) {
        pushSourceInfo(x.getSource());

        p.print(CHARS_CONTINUE);
        continueOrBreakLabel(x);

        popSourceInfo();
    }

    private void continueOrBreakLabel(JsContinue x) {
        JsNameRef label = x.getLabel();
        if (label != null) {
            space();
            p.print(label.getIdent());
        }
    }

    @Override
    public void visitCase(@NotNull JsCase x) {
        pushSourceInfo(x.getSource());

        p.print(CHARS_CASE);
        space();
        accept(x.getCaseExpression());
        _colon();

        popSourceInfo();

        newlineOpt();

        sourceLocationConsumer.pushSourceInfo(null);
        printSwitchMemberStatements(x);
        sourceLocationConsumer.popSourceInfo();
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
        pushSourceInfo(x.getSource());

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

        popSourceInfo();

        sourceLocationConsumer.pushSourceInfo(null);
        accept(x.getBody());
        sourceLocationConsumer.popSourceInfo();
    }

    @Override
    public void visitConditional(@NotNull JsConditional x) {
        pushSourceInfo(x.getSource());

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

        popSourceInfo();
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
        pushSourceInfo(x.getSource());

        p.print(CHARS_DEBUGGER);

        popSourceInfo();
    }

    @Override
    public void visitDefault(@NotNull JsDefault x) {
        pushSourceInfo(x.getSource());

        p.print(CHARS_DEFAULT);
        _colon();

        popSourceInfo();

        sourceLocationConsumer.pushSourceInfo(null);
        printSwitchMemberStatements(x);
        sourceLocationConsumer.popSourceInfo();
    }

    @Override
    public void visitWhile(@NotNull JsWhile x) {
        pushSourceInfo(x.getSource());

        _while();
        spaceOpt();
        leftParen();
        accept(x.getCondition());
        rightParen();

        popSourceInfo();

        nestedPush(x.getBody());
        sourceLocationConsumer.pushSourceInfo(null);
        accept(x.getBody());
        sourceLocationConsumer.popSourceInfo();
        nestedPop(x.getBody());
    }

    @Override
    public void visitDoWhile(@NotNull JsDoWhile x) {
        sourceLocationConsumer.pushSourceInfo(null);

        p.print(CHARS_DO);
        nestedPush(x.getBody());
        accept(x.getBody());
        sourceLocationConsumer.popSourceInfo();
        nestedPop(x.getBody());

        pushSourceInfo(x.getCondition().getSource());
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

        popSourceInfo();
    }

    @Override
    public void visitEmpty(@NotNull JsEmpty x) {
    }

    @Override
    public void visitExpressionStatement(@NotNull JsExpressionStatement x) {
        Object source = x.getSource();
        if (source == null && !(x.getExpression() instanceof JsFunction)) {
            source = x.getExpression().getSource();
        }
        pushSourceInfo(source);

        boolean surroundWithParentheses = JsFirstExpressionVisitor.exec(x);
        if (surroundWithParentheses) {
            leftParen();
        }
        accept(x.getExpression());
        if (surroundWithParentheses) {
            rightParen();
        }

        popSourceInfo();
    }

    @Override
    public void visitFor(@NotNull JsFor x) {
        pushSourceInfo(x.getSource());

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

        popSourceInfo();

        nestedPush(x.getBody());
        if (x.getBody() != null) {
            sourceLocationConsumer.pushSourceInfo(null);
            accept(x.getBody());
            sourceLocationConsumer.popSourceInfo();
        }
        nestedPop(x.getBody());
    }

    @Override
    public void visitForIn(@NotNull JsForIn x) {
        pushSourceInfo(x.getSource());

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

        popSourceInfo();

        nestedPush(x.getBody());
        sourceLocationConsumer.pushSourceInfo(null);
        accept(x.getBody());
        sourceLocationConsumer.popSourceInfo();
        nestedPop(x.getBody());
    }

    @Override
    public void visitFunction(@NotNull JsFunction x) {
        pushSourceInfo(x.getSource());

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

        sourceLocationConsumer.pushSourceInfo(null);
        printJsBlock(x.getBody(), true, x.getBody().getSource());
        sourceLocationConsumer.popSourceInfo();

        needSemi = true;

        popSourceInfo();
    }

    @Override
    public void visitIf(@NotNull JsIf x) {
        pushSourceInfo(x.getSource());

        _if();
        spaceOpt();
        leftParen();
        accept(x.getIfExpression());
        rightParen();

        popSourceInfo();

        JsStatement thenStmt = x.getThenStatement();
        JsStatement elseStatement = x.getElseStatement();
        if (elseStatement != null && isIfWithoutElse(thenStmt)) {
            thenStmt = new JsBlock(thenStmt);
        }
        nestedPush(thenStmt);

        if (thenStmt instanceof JsBlock) {
            lineBreakAfterBlock = false;
        }

        sourceLocationConsumer.pushSourceInfo(null);
        accept(thenStmt);
        sourceLocationConsumer.popSourceInfo();

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
            sourceLocationConsumer.pushSourceInfo(null);
            accept(elseStatement);
            sourceLocationConsumer.popSourceInfo();
            if (!elseIf) {
                nestedPop(elseStatement);
            }
        }
    }

    private static boolean isIfWithoutElse(@NotNull JsStatement statement) {
        while (statement instanceof JsIf) {
            JsIf ifStatement = (JsIf) statement;
            if (ifStatement.getElseStatement() == null) {
                return true;
            }
            statement = ifStatement.getElseStatement();
        }

        return false;
    }

    @Override
    public void visitInvocation(@NotNull JsInvocation invocation) {
        pushSourceInfo(invocation.getSource());

        printPair(invocation, invocation.getQualifier());

        leftParen();
        printExpressions(invocation.getArguments());
        rightParen();

        popSourceInfo();
    }

    @Override
    public void visitLabel(@NotNull JsLabel x) {
        nameOf(x);
        _colon();
        spaceOpt();

        sourceLocationConsumer.pushSourceInfo(null);
        accept(x.getStatement());
        sourceLocationConsumer.popSourceInfo();
    }

    @Override
    public void visitNameRef(@NotNull JsNameRef nameRef) {
        pushSourceInfo(nameRef.getSource());

        JsExpression qualifier = nameRef.getQualifier();
        if (qualifier != null) {
            boolean enclose;
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

        popSourceInfo();
    }

    @Override
    public void visitNew(@NotNull JsNew x) {
        pushSourceInfo(x.getSource());

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

        popSourceInfo();
    }

    @Override
    public void visitNull(@NotNull JsNullLiteral x) {
        pushSourceInfo(x.getSource());

        p.print(CHARS_NULL);

        popSourceInfo();
    }

    @Override
    public void visitInt(@NotNull JsIntLiteral x) {
        pushSourceInfo(x.getSource());

        p.print(x.value);

        popSourceInfo();
    }

    @Override
    public void visitDouble(@NotNull JsDoubleLiteral x) {
        pushSourceInfo(x.getSource());

        p.print(x.value);

        popSourceInfo();
    }

    @Override
    public void visitObjectLiteral(@NotNull JsObjectLiteral objectLiteral) {
        pushSourceInfo(objectLiteral.getSource());

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

            pushSourceInfo(item.getSource());

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

            popSourceInfo();
        }

        if (objectLiteral.isMultiline()) {
            p.indentOut();
            newlineOpt();
        }

        p.print('}');
        popSourceInfo();
    }

    @Override
    public void visitParameter(@NotNull JsParameter x) {
        nameOf(x);
    }

    @Override
    public void visitPostfixOperation(@NotNull JsPostfixOperation x) {
        pushSourceInfo(x.getSource());

        JsUnaryOperator op = x.getOperator();
        JsExpression arg = x.getArg();
        // unary operators always associate correctly (I think)
        printPair(x, arg);
        p.print(op.getSymbol());

        popSourceInfo();
    }

    @Override
    public void visitPrefixOperation(@NotNull JsPrefixOperation x) {
        pushSourceInfo(x.getSource());

        JsUnaryOperator op = x.getOperator();
        p.print(op.getSymbol());
        JsExpression arg = x.getArg();
        if (spaceCalc(op, arg)) {
            space();
        }
        // unary operators always associate correctly (I think)
        printPair(x, arg);

        popSourceInfo();
    }

    @Override
    public void visitProgram(@NotNull JsProgram x) {
        x.acceptChildren(this);
    }

    @Override
    public void visitRegExp(@NotNull JsRegExp x) {
        pushSourceInfo(x.getSource());

        slash();
        p.print(x.getPattern());
        slash();
        String flags = x.getFlags();
        if (flags != null) {
            p.print(flags);
        }

        popSourceInfo();
    }

    @Override
    public void visitReturn(@NotNull JsReturn x) {
        pushSourceInfo(x.getSource());

        p.print(CHARS_RETURN);
        JsExpression expr = x.getExpression();
        if (expr != null) {
            space();
            accept(expr);
        }

        popSourceInfo();
    }

    @Override
    public void visitString(@NotNull JsStringLiteral x) {
        pushSourceInfo(x.getSource());

        p.print(javaScriptString(x.getValue()));

        popSourceInfo();
    }

    @Override
    public void visit(@NotNull JsSwitch x) {
        pushSourceInfo(x.getSource());

        p.print(CHARS_SWITCH);
        spaceOpt();
        leftParen();
        accept(x.getExpression());
        rightParen();

        popSourceInfo();


        sourceLocationConsumer.pushSourceInfo(null);
        spaceOpt();
        blockOpen();
        acceptList(x.getCases());
        blockClose();
        sourceLocationConsumer.popSourceInfo();
    }

    @Override
    public void visitThis(@NotNull JsThisRef x) {
        pushSourceInfo(x.getSource());

        p.print(CHARS_THIS);

        popSourceInfo();
    }

    @Override
    public void visitThrow(@NotNull JsThrow x) {
        pushSourceInfo(x.getSource());

        p.print(CHARS_THROW);
        space();
        accept(x.getExpression());

        popSourceInfo();
    }

    @Override
    public void visitTry(@NotNull JsTry x) {
        p.print(CHARS_TRY);
        spaceOpt();
        lineBreakAfterBlock = false;
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
        pushSourceInfo(var.getSource());

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

        popSourceInfo();
    }

    @Override
    public void visitVars(@NotNull JsVars vars) {
        pushSourceInfo(vars.getSource());

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

        popSourceInfo();
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
            newline();
        }

        boolean notFirst = false;
        for (Map.Entry<String, Object> entry : comment.getTags().entrySet()) {
            if (notFirst) {
                newline();
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
                newline();
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

    private void newlineOpt() {
        if (!p.isCompact()) {
            newline();
        }
    }

    private void newline() {
        p.newline();
        sourceLocationConsumer.newLine();
    }

    private void pushSourceInfo(Object location) {
        p.maybeIndent();
        sourceInfoStack.add(location);
        if (location != null) {
            sourceLocationConsumer.pushSourceInfo(location);
        }
    }

    private void popSourceInfo() {
        if (!sourceInfoStack.isEmpty() && sourceInfoStack.remove(sourceInfoStack.size() - 1) != null) {
            sourceLocationConsumer.popSourceInfo();
        }
    }

    private void printJsBlock(JsBlock x, boolean finalNewline, Object closingBracketLocation) {
        if (!lineBreakAfterBlock) {
            finalNewline = false;
            lineBreakAfterBlock = true;
        }

        sourceLocationConsumer.pushSourceInfo(null);

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
                        newline();
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

            if (closingBracketLocation != null) {
                pushSourceInfo(closingBracketLocation);
            }
            p.print('}');
            if (closingBracketLocation != null) {
                popSourceInfo();
            }

            if (finalNewline) {
                newlineOpt();
            }
        }
        needSemi = false;

        sourceLocationConsumer.popSourceInfo();
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
