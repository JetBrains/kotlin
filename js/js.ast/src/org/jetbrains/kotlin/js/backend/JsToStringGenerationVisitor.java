// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend;

import kotlin.text.StringsKt;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.backend.ast.JsDoubleLiteral;
import org.jetbrains.kotlin.js.backend.ast.JsIntLiteral;
import org.jetbrains.kotlin.js.backend.ast.JsVars.JsVar;
import org.jetbrains.kotlin.js.common.IdentifierPolicyKt;
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
    private static final char[] CHARS_CLASS = "class".toCharArray();
    private static final char[] CHARS_CONSTRUCTOR = "constructor".toCharArray();
    private static final char[] CHARS_CONTINUE = "continue".toCharArray();
    private static final char[] CHARS_DEBUGGER = "debugger".toCharArray();
    private static final char[] CHARS_DEFAULT = "default".toCharArray();
    private static final char[] CHARS_DO = "do".toCharArray();
    private static final char[] CHARS_ELSE = "else".toCharArray();
    private static final char[] CHARS_EXTENDS = "extends".toCharArray();
    private static final char[] CHARS_FALSE = "false".toCharArray();
    private static final char[] CHARS_FINALLY = "finally".toCharArray();
    private static final char[] CHARS_FOR = "for".toCharArray();
    private static final char[] CHARS_FUNCTION = "function".toCharArray();
    private static final char[] CHARS_STATIC = "static".toCharArray();
    private static final char[] CHARS_GET = "get".toCharArray();
    private static final char[] CHARS_SET = "set".toCharArray();
    private static final char[] CHARS_IF = "if".toCharArray();
    private static final char[] CHARS_IN = "in".toCharArray();
    private static final char[] CHARS_NEW = "new".toCharArray();
    private static final char[] CHARS_NULL = "null".toCharArray();
    private static final char[] CHARS_RETURN = "return".toCharArray();
    private static final char[] CHARS_SWITCH = "switch".toCharArray();
    private static final char[] CHARS_THIS = "this".toCharArray();

    private static final char[] CHARS_SUPER = "super".toCharArray();
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

    protected boolean insideComments = false;
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
        printCommentsBeforeNode(x);
        pushSourceInfo(x.getSource());

        printPair(x, x.getArrayExpression());
        leftSquare();
        accept(x.getIndexExpression());
        rightSquare();

        printCommentsAfterNode(x);
        popSourceInfo();
    }

    @Override
    public void visitArray(@NotNull JsArrayLiteral x) {
        printCommentsBeforeNode(x);
        pushSourceInfo(x.getSource());

        leftSquare();
        printExpressions(x.getExpressions());
        rightSquare();

        printCommentsAfterNode(x);
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
        printCommentsBeforeNode(binaryOperation);
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

        printCommentsAfterNode(binaryOperation);
        popSourceInfo();
    }

    @Override
    public void visitBlock(@NotNull JsBlock x) {
        printJsBlock(x, true, null);
    }

    @Override
    public void visitBoolean(@NotNull JsBooleanLiteral x) {
        pushSourceInfo(x.getSource());
        printCommentsBeforeNode(x);

        if (x.getValue()) {
            p.print(CHARS_TRUE);
        }
        else {
            p.print(CHARS_FALSE);
        }

        printCommentsAfterNode(x);
        popSourceInfo();
    }

    @Override
    public void visitBreak(@NotNull JsBreak x) {
        pushSourceInfo(x.getSource());
        printCommentsBeforeNode(x);

        p.print(CHARS_BREAK);
        continueOrBreakLabel(x);

        printCommentsAfterNode(x);
        popSourceInfo();
    }

    @Override
    public void visitContinue(@NotNull JsContinue x) {
        pushSourceInfo(x.getSource());
        printCommentsBeforeNode(x);

        p.print(CHARS_CONTINUE);
        continueOrBreakLabel(x);

        printCommentsAfterNode(x);
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
        printCommentsBeforeNode(x);

        p.print(CHARS_CASE);
        space();
        accept(x.getCaseExpression());
        _colon();

        printCommentsAfterNode(x);
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
        printCommentsBeforeNode(x);
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
        printCommentsAfterNode(x);

        sourceLocationConsumer.pushSourceInfo(null);
        accept(x.getBody());
        sourceLocationConsumer.popSourceInfo();
    }

    @Override
    public void visitConditional(@NotNull JsConditional x) {
        pushSourceInfo(x.getSource());
        printCommentsBeforeNode(x);

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

        printCommentsAfterNode(x);
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
        printCommentsBeforeNode(x);

        p.print(CHARS_DEBUGGER);

        printCommentsAfterNode(x);
        popSourceInfo();
    }

    @Override
    public void visitDefault(@NotNull JsDefault x) {
        pushSourceInfo(x.getSource());
        printCommentsBeforeNode(x);

        p.print(CHARS_DEFAULT);
        _colon();

        printCommentsAfterNode(x);
        popSourceInfo();
        newlineOpt();

        sourceLocationConsumer.pushSourceInfo(null);
        printSwitchMemberStatements(x);
        sourceLocationConsumer.popSourceInfo();
    }

    @Override
    public void visitWhile(@NotNull JsWhile x) {
        pushSourceInfo(x.getSource());
        printCommentsBeforeNode(x);

        _while();
        spaceOpt();
        leftParen();
        accept(x.getCondition());
        rightParen();

        printCommentsAfterNode(x);
        popSourceInfo();

        JsStatement body = materialize(x.getBody());

        nestedPush(body);
        sourceLocationConsumer.pushSourceInfo(null);
        accept(body);
        sourceLocationConsumer.popSourceInfo();
        nestedPop(body);
    }

    @Override
    public void visitDoWhile(@NotNull JsDoWhile x) {
        sourceLocationConsumer.pushSourceInfo(null);
        printCommentsBeforeNode(x);

        p.print(CHARS_DO);

        JsStatement body = materialize(x.getBody());

        nestedPush(body);
        accept(body);
        sourceLocationConsumer.popSourceInfo();
        nestedPop(body);

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

        printCommentsAfterNode(x);
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
        printCommentsBeforeNode(x);

        boolean surroundWithParentheses = JsFirstExpressionVisitor.exec(x);
        if (surroundWithParentheses) {
            leftParen();
        }
        accept(x.getExpression());
        if (surroundWithParentheses) {
            rightParen();
        }

        printCommentsAfterNode(x);
        popSourceInfo();
    }

    @Override
    public void visitFor(@NotNull JsFor x) {
        pushSourceInfo(x.getSource());
        printCommentsBeforeNode(x);

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

        printCommentsAfterNode(x);
        popSourceInfo();

        JsStatement body = materialize(x.getBody());

        nestedPush(body);
        if (body != null) {
            sourceLocationConsumer.pushSourceInfo(null);
            accept(body);
            sourceLocationConsumer.popSourceInfo();
        }
        nestedPop(body);
    }

    @Override
    public void visitForIn(@NotNull JsForIn x) {
        pushSourceInfo(x.getSource());
        printCommentsBeforeNode(x);

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

        printCommentsAfterNode(x);
        popSourceInfo();

        JsStatement body = materialize(x.getBody());
        nestedPush(body);
        sourceLocationConsumer.pushSourceInfo(null);
        accept(body);
        sourceLocationConsumer.popSourceInfo();
        nestedPop(body);
    }

    @Override
    public void visitFunction(@NotNull JsFunction x) {
        printCommentsBeforeNode(x);

        p.print(CHARS_FUNCTION);
        space();

        printFunction(x);

        printCommentsAfterNode(x);
    }

    // [static?] [get|set?] name(<params>) { <body> }
    private void printFunction(@NotNull JsFunction x) {
        if (x.isStatic()) {
            p.print(CHARS_STATIC);
            space();
        }

        if (x.isGetter()) {
            p.print(CHARS_GET);
            space();
        } else if (x.isSetter()) {
            p.print(CHARS_SET);
            space();
        }

        if (x.getName() != null) {
            nameOf(x);
        }

        pushSourceInfo(x.getSource());
        leftParen();
        boolean notFirst = false;
        sourceLocationConsumer.pushSourceInfo(null);
        for (JsParameter param : x.getParameters()) {
            notFirst = sepCommaOptSpace(notFirst);
            accept(param);
        }
        sourceLocationConsumer.popSourceInfo();
        rightParen();
        space();

        lineBreakAfterBlock = false;

        sourceLocationConsumer.pushSourceInfo(null);
        printJsBlock(x.getBody(), true, x.getBody().getSource());
        sourceLocationConsumer.popSourceInfo();

        popSourceInfo();

        needSemi = true;
    }

    @Override
    public void visitClass(@NotNull JsClass x) {
        pushSourceInfo(x.getSource());
        printCommentsBeforeNode(x);

        p.print(CHARS_CLASS);
        space();
        if (x.getName() != null) {
            nameOf(x);
        }

        if (x.getBaseClass() != null) {
            space();
            p.print(CHARS_EXTENDS);
            space();
            accept(x.getBaseClass());
        }

        space();

        if (x.getConstructor() == null && x.getMembers().isEmpty()) {
            p.print("{}");
            newline();
        } else {
            blockOpen();

            if (x.getConstructor() != null) {
                p.print(CHARS_CONSTRUCTOR);
                x.getConstructor().setName(null);
                printFunction(x.getConstructor());
                // TODO newLineOpt ?
                newline();
            }

            for (JsFunction m : x.getMembers()) {
                printFunction(m);
                newline();
            }

            blockClose();
        }

        needSemi = false;

        printCommentsAfterNode(x);
        popSourceInfo();
    }

    @Override
    public void visitIf(@NotNull JsIf x) {
        printCommentsBeforeNode(x);
        pushSourceInfo(x.getSource());

        _if();
        spaceOpt();
        leftParen();
        accept(x.getIfExpression());
        rightParen();

        popSourceInfo();
        printCommentsAfterNode(x);

        JsStatement thenStmt = x.getThenStatement();
        JsStatement elseStatement = x.getElseStatement();
        if (elseStatement != null && isIfWithoutElse(thenStmt)) {
            thenStmt = new JsBlock(thenStmt);
        }
        nestedPush(thenStmt);

        if (thenStmt instanceof JsBlock && elseStatement != null) {
            lineBreakAfterBlock = false;
        }

        sourceLocationConsumer.pushSourceInfo(null);
        accept(materialize(thenStmt));
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
            accept(materialize(elseStatement));
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

    private static JsStatement materialize(JsStatement statement) {
       return statement instanceof JsCompositeBlock && ((JsCompositeBlock) statement).getStatements().size() > 1
              ? new JsBlock(statement)
              : statement;
    }

    @Override
    public void visitInvocation(@NotNull JsInvocation invocation) {
        pushSourceInfo(invocation.getSource());
        printCommentsBeforeNode(invocation);

        printPair(invocation, invocation.getQualifier());

        leftParen();
        printExpressions(invocation.getArguments());
        rightParen();

        printCommentsAfterNode(invocation);
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
        visitNameRef(nameRef, true);
    }

    public void visitNameRef(@NotNull JsNameRef nameRef, boolean withQualifier) {

        printCommentsBeforeNode(nameRef);
        p.maybeIndent();

        JsExpression qualifier = nameRef.getQualifier();
        if (qualifier != null && withQualifier) {
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

        pushSourceInfo(nameRef.getSource());
        p.print(nameRef.getIdent());
        popSourceInfo();

        printCommentsAfterNode(nameRef);
    }

    @Override
    public void visitNew(@NotNull JsNew x) {
        pushSourceInfo(x.getSource());
        printCommentsBeforeNode(x);

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

        printCommentsAfterNode(x);
        popSourceInfo();
    }

    @Override
    public void visitNull(@NotNull JsNullLiteral x) {
        pushSourceInfo(x.getSource());
        printCommentsBeforeNode(x);

        p.print(CHARS_NULL);

        printCommentsAfterNode(x);
        popSourceInfo();
    }

    @Override
    public void visitInt(@NotNull JsIntLiteral x) {
        pushSourceInfo(x.getSource());
        printCommentsBeforeNode(x);

        p.print(x.value);

        printCommentsAfterNode(x);
        popSourceInfo();
    }

    @Override
    public void visitDouble(@NotNull JsDoubleLiteral x) {
        pushSourceInfo(x.getSource());
        printCommentsBeforeNode(x);

        p.print(x.value);

        printCommentsAfterNode(x);
        popSourceInfo();
    }

    @Override
    public void visitObjectLiteral(@NotNull JsObjectLiteral objectLiteral) {
        pushSourceInfo(objectLiteral.getSource());
        printCommentsBeforeNode(objectLiteral);

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

            if (labelExpr instanceof JsStringLiteral) {
                JsStringLiteral stringLiteral = (JsStringLiteral) labelExpr;
                String value = stringLiteral.getValue();
                if (IdentifierPolicyKt.isValidES5Identifier(value)) {
                   labelExpr = new JsNameRef(value).withMetadataFrom(stringLiteral);
                }
            }
            // labels can be either string, integral, or decimal literals
            if (labelExpr instanceof JsNameRef) {
                visitNameRef((JsNameRef) labelExpr, false);
            } else {
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

        printCommentsAfterNode(objectLiteral);
        popSourceInfo();
    }

    @Override
    public void visitParameter(@NotNull JsParameter x) {
        pushSourceInfo(x.getSource());
        nameOf(x);
        popSourceInfo();
    }

    @Override
    public void visitPostfixOperation(@NotNull JsPostfixOperation x) {
        pushSourceInfo(x.getSource());
        printCommentsBeforeNode(x);

        JsUnaryOperator op = x.getOperator();
        JsExpression arg = x.getArg();
        // unary operators always associate correctly (I think)
        printPair(x, arg);
        p.print(op.getSymbol());

        printCommentsAfterNode(x);
        popSourceInfo();
    }

    @Override
    public void visitPrefixOperation(@NotNull JsPrefixOperation x) {
        pushSourceInfo(x.getSource());
        printCommentsBeforeNode(x);

        JsUnaryOperator op = x.getOperator();
        p.print(op.getSymbol());
        JsExpression arg = x.getArg();
        if (spaceCalc(op, arg)) {
            space();
        }
        // unary operators always associate correctly (I think)
        printPair(x, arg);

        printCommentsAfterNode(x);
        popSourceInfo();
    }

    @Override
    public void visitProgram(@NotNull JsProgram x) {
        x.acceptChildren(this);
    }

    @Override
    public void visitRegExp(@NotNull JsRegExp x) {
        pushSourceInfo(x.getSource());
        printCommentsBeforeNode(x);

        slash();
        p.print(x.getPattern());
        slash();
        String flags = x.getFlags();
        if (flags != null) {
            p.print(flags);
        }

        printCommentsAfterNode(x);
        popSourceInfo();
    }

    @Override
    public void visitReturn(@NotNull JsReturn x) {
        pushSourceInfo(x.getSource());
        printCommentsBeforeNode(x);

        p.print(CHARS_RETURN);
        JsExpression expr = x.getExpression();
        if (expr != null) {
            space();
            accept(expr);
        }

        printCommentsAfterNode(x);
        popSourceInfo();
    }

    @Override
    public void visitString(@NotNull JsStringLiteral x) {
        pushSourceInfo(x.getSource());
        printCommentsBeforeNode(x);

        p.print(javaScriptString(x.getValue()));

        printCommentsAfterNode(x);
        popSourceInfo();
    }

    @Override
    public void visit(@NotNull JsSwitch x) {
        pushSourceInfo(x.getSource());
        printCommentsBeforeNode(x);

        p.print(CHARS_SWITCH);
        spaceOpt();
        leftParen();
        accept(x.getExpression());
        rightParen();

        printCommentsAfterNode(x);
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
        printCommentsBeforeNode(x);

        p.print(CHARS_THIS);

        printCommentsAfterNode(x);
        popSourceInfo();
    }

    @Override
    public void visitSuper(@NotNull JsSuperRef x) {
        pushSourceInfo(x.getSource());
        printCommentsBeforeNode(x);

        p.print(CHARS_SUPER);

        printCommentsAfterNode(x);
        popSourceInfo();
    }

    @Override
    public void visitThrow(@NotNull JsThrow x) {
        pushSourceInfo(x.getSource());
        printCommentsBeforeNode(x);

        p.print(CHARS_THROW);
        space();
        accept(x.getExpression());

        printCommentsAfterNode(x);
        popSourceInfo();
    }

    @Override
    public void visitTry(@NotNull JsTry x) {
        printCommentsBeforeNode(x);
        pushSourceInfo(x.getSource());
        p.print(CHARS_TRY);
        spaceOpt();
        lineBreakAfterBlock = false;
        popSourceInfo();
        accept(x.getTryBlock());

        acceptList(x.getCatches());

        JsBlock finallyBlock = x.getFinallyBlock();
        if (finallyBlock != null) {
            p.print(CHARS_FINALLY);
            spaceOpt();
            accept(finallyBlock);
        }
        printCommentsAfterNode(x);
    }

    @Override
    public void visit(@NotNull JsVar var) {
        pushSourceInfo(var.getSource());
        printCommentsBeforeNode(var);

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

        printCommentsAfterNode(var);
        popSourceInfo();
    }

    @Override
    public void visitVars(@NotNull JsVars vars) {
        pushSourceInfo(vars.getSource());
        printCommentsBeforeNode(vars);

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

        printCommentsAfterNode(vars);
        popSourceInfo();
    }

    @Override
    public void visitSingleLineComment(@NotNull JsSingleLineComment comment) {
        if (needSemi && insideComments) {
            semi();
            space();
        }
        p.print("//");
        p.print(comment.getText());
        newline();
        needSemi = false;
    }

    @Override
    public void visitMultiLineComment(@NotNull JsMultiLineComment comment) {
        List<String> lines = StringsKt.lines(comment.getText());

        p.print("/*");
        p.print(lines.get(0).trim());

        for (int i = 1; i < lines.size(); i++) {
            newline();
            p.print(lines.get(i).trim());
        }

        p.print("*/");
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

    @Override
    public void visitExport(@NotNull JsExport export) {
        p.print("export");
        space();
        JsExport.Subject subject = export.getSubject();

        if (subject instanceof JsExport.Subject.All) {
            p.print("*");
        } else if (subject instanceof JsExport.Subject.Elements) {
            blockOpen();
            List<JsExport.Element> elements = ((JsExport.Subject.Elements) subject).getElements();
            for (JsExport.Element element : elements) {
                visitNameRef(element.getName());
                JsName alias = element.getAlias();
                if (alias != null) {
                    p.print(" as ");
                    nameDef(alias);
                }
                p.print(',');
                p.newline();
            }
            p.indentOut();
            p.print('}');
        }

        if (export.getFromModule() != null) {
            p.print(" from ");
            p.print(javaScriptString(export.getFromModule()));
        }
        needSemi = true;
    }

    @Override
    public void visitImport(@NotNull JsImport jsImport) {
        JsImport.Target target = jsImport.getTarget();

        p.print("import ");

        if (target instanceof JsImport.Target.Default) {
            visitNameRef(((JsImport.Target.Default) target).getName());
        } else if (target instanceof JsImport.Target.All) {
            p.print("* as ");
            visitNameRef(((JsImport.Target.All) target).getAlias());
        } else if (target instanceof JsImport.Target.Elements) {
            List<JsImport.Element> elements = ((JsImport.Target.Elements) target).getElements();

            p.print("{");
            boolean isMultiline = elements.size() > 1;
            p.indentIn();
            if (isMultiline)
                newlineOpt();
            else
                space();

            for (JsImport.Element element : elements) {
                nameDef(element.getName());
                JsNameRef alias = element.getAlias();
                if (alias != null) {
                    p.print(" as ");
                    visitNameRef(alias);
                }

                if (isMultiline) {
                    p.print(',');
                    newlineOpt();
                }
                else {
                    space();
                }
            }
            p.indentOut();
            p.print("}");
        }

        p.print(" from ");
        p.print(javaScriptString(jsImport.getModule()));
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

    private void printCommentsBeforeNode(JsNode x) {
       printComments(x.getCommentsBeforeNode(), false);
    }

    private void printCommentsAfterNode(JsNode x) {
        printComments(x.getCommentsAfterNode(), true);
    }

    private void printComments(List<JsComment> comments, boolean isAfterNode) {
        if (comments == null) return;

        boolean previousNeedSemi = needSemi;
        needSemi = isAfterNode;
        insideComments = true;

        for (JsComment comment : comments) {
            comment.accept(this);
        }

        insideComments = false;

        if (!isAfterNode) {
            needSemi = previousNeedSemi;
        }
    }

    private void popSourceInfo() {
        if (!sourceInfoStack.isEmpty() && sourceInfoStack.remove(sourceInfoStack.size() - 1) != null) {
            sourceLocationConsumer.popSourceInfo();
        }
    }

    private void printJsBlock(JsBlock x, boolean finalNewline, @Nullable Object defaultClosingBraceLocation) {
        if (!lineBreakAfterBlock) {
            finalNewline = false;
            lineBreakAfterBlock = true;
        }

        printCommentsBeforeNode(x);

        boolean needBraces = !x.isTransparent();

        if (needBraces) {
            sourceLocationConsumer.pushSourceInfo(x.getSource());
            blockOpen();
            sourceLocationConsumer.popSourceInfo();
        }

        sourceLocationConsumer.pushSourceInfo(null);

        Iterator<JsStatement> iterator = x.getStatements().iterator();
        while (iterator.hasNext()) {
            boolean isGlobal = x.isTransparent() || globalBlocks.contains(x);

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

            sourceLocationConsumer.popSourceInfo();

            Object closingBraceLocation = x.getClosingBraceSource();
            if (closingBraceLocation == null)
                closingBraceLocation = defaultClosingBraceLocation;

            if (closingBraceLocation != null) {
                pushSourceInfo(closingBraceLocation);
            }
            p.print('}');
            if (closingBraceLocation != null) {
                popSourceInfo();
            }

            if (finalNewline) {
                newlineOpt();
            }
        } else {
            sourceLocationConsumer.popSourceInfo();
        }
        needSemi = false;
        printCommentsAfterNode(x);
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
