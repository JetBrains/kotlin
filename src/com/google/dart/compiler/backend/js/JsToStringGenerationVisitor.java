// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.backend.js.ast.JsVars.JsVar;
import com.google.dart.compiler.util.TextOutput;
import gnu.trove.TIntArrayList;

import java.util.*;

import static com.google.dart.compiler.backend.js.ast.JsNumberLiteral.JsDoubleLiteral;
import static com.google.dart.compiler.backend.js.ast.JsNumberLiteral.JsIntLiteral;

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

    /**
     * How many lines of code to print inside of a JsBlock when printing terse.
     */
    private static final int JSBLOCK_LINES_TO_PRINT = 3;

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
                /*
                * Emit characters from 0 to 31 that don't have a single character
                * escape sequence in octal where possible. This saves one or two
                * characters compared to the hexadecimal format '\xXX'.
                *
                * These short octal sequences may only be used at the end of the string
                * or where the following character is a non-digit. Otherwise, the
                * following character would be incorrectly interpreted as belonging to
                * the sequence.
                */
                if (c < ' ' && (i == n - 1 || chars.charAt(i + 1) < '0' || chars.charAt(i + 1) > '9')) {
                    result.append('\\');
                    if (c > 0x7) {
                        result.append((char) ('0' + (0x7 & (c >> 3))));
                    }
                    result.append((char) ('0' + (0x7 & c)));
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
    private Set<JsBlock> globalBlocks = new HashSet<JsBlock>();
    private final TextOutput p;
    private TIntArrayList statementEnds = new TIntArrayList();
    private TIntArrayList statementStarts = new TIntArrayList();

    public JsToStringGenerationVisitor(TextOutput out) {
        this.p = out;
    }

    @Override
    public boolean visit(JsArrayAccess x, JsContext ctx) {
        JsExpression arrayExpr = x.getArrayExpr();
        printPair(x, arrayExpr);
        _lsquare();
        accept(x.getIndexExpr());
        _rsquare();
        return false;
    }

    @Override
    public boolean visit(JsArrayLiteral x, JsContext ctx) {
        _lsquare();
        printExpressions(x.getExpressions());
        _rsquare();
        return false;
    }

    private void printExpressions(List<JsExpression> expressions) {
        boolean sep = false;
        for (JsExpression arg : expressions) {
            sep = sepCommaOptSpace(sep);
            boolean isEnclosed = parenPushIfCommaExpression(arg);
            accept(arg);
            if (isEnclosed) {
                rightParen();
            }
        }
    }

    @Override
    public boolean visit(JsBinaryOperation binaryOperation, JsContext ctx) {
        JsBinaryOperator op = binaryOperation.getOperator();
        JsExpression arg1 = binaryOperation.getArg1();
        boolean isExpressionEnclosed = _parenPush(binaryOperation, arg1, !op.isLeftAssociative());

        accept(arg1);
        if (op.isKeyword()) {
            _parenPopOrSpace(binaryOperation, arg1, !op.isLeftAssociative());
        }
        else if (op != JsBinaryOperator.COMMA) {
            if (isExpressionEnclosed) {
                rightParen();
            }
            spaceOpt();
        }

        p.print(op.getSymbol());

        JsExpression arg2 = binaryOperation.getArg2();
        boolean isParenOpened;
        if (op == JsBinaryOperator.COMMA) {
            isParenOpened = false;
            spaceOpt();
        }
        else if (arg2 instanceof JsBinaryOperation && isBinaryOperationShouldBeEnclosed(((JsBinaryOperation) arg2).getOperator())) {
            spaceOpt();
            leftParen();
            isParenOpened = true;
        }
        else {
            if (spaceCalc(op, arg2)) {
                isParenOpened = _parenPushOrSpace(binaryOperation, arg2, op.isLeftAssociative());
            }
            else {
                spaceOpt();
                isParenOpened = _parenPush(binaryOperation, arg2, op.isLeftAssociative());
            }
        }
        accept(arg2);
        if (isParenOpened) {
            rightParen();
        }

        return false;
    }

    private static boolean isBinaryOperationShouldBeEnclosed(JsBinaryOperator operator) {
        return operator == JsBinaryOperator.AND;
    }

    @Override
    public boolean visit(JsBlock x, JsContext ctx) {
        printJsBlock(x, true, true);
        return false;
    }

    @Override
    public boolean visit(JsLiteral.JsBooleanLiteral x, JsContext ctx) {
        if (x.getValue()) {
            p.print(CHARS_TRUE);
        }
        else {
            _false();
        }
        return false;
    }

    @Override
    public boolean visit(JsBreak x, JsContext ctx) {
        _break();
        continueOrBreakLabel(x);
        return false;
    }

    @Override
    public boolean visit(JsContinue x, JsContext ctx) {
        _continue();
        continueOrBreakLabel(x);
        return false;
    }

    private void continueOrBreakLabel(JsContinue x) {
        JsNameRef label = x.getLabel();
        if (label != null) {
            space();
            _nameRef(label);
        }
    }

    @Override
    public boolean visit(JsCase x, JsContext ctx) {
        _case();
        space();
        accept(x.getCaseExpr());
        _colon();
        newlineOpt();

        printSwitchMemberStatements(x);
        return false;
    }

    private void printSwitchMemberStatements(JsSwitchMember x) {
        indent();
        for (JsStatement stmt : x.getStatements()) {
            needSemi = true;
            accept(stmt);
            if (needSemi) {
                semi();
            }
            newlineOpt();
        }
        outdent();
        needSemi = false;
    }

    @Override
    public boolean visit(JsCatch x, JsContext ctx) {
        spaceOpt();
        _catch();
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

        return false;
    }

    @Override
    public boolean visit(JsConditional x, JsContext ctx) {
        // Associativity: for the then and else branches, it is safe to insert
        // another
        // ternary expression, but if the test expression is a ternary, it should
        // get parentheses around it.
        printPair(x, x.getTestExpression());
        spaceOpt();
        questionMark();
        spaceOpt();
        printPair(x, x.getThenExpression());
        spaceOpt();
        _colon();
        spaceOpt();
        printPair(x, x.getElseExpression());
        return false;
    }

    private void printPair(JsExpression parent, JsExpression expression) {
        boolean isNeedParen = parenCalc(parent, expression, false);
        if (isNeedParen) {
            leftParen();
        }
        accept(expression);
        if (isNeedParen) {
            rightParen();
        }
    }

    @Override
    public boolean visit(JsDebugger x, JsContext ctx) {
        _debugger();
        return false;
    }

    @Override
    public boolean visit(JsDefault x, JsContext ctx) {
        _default();
        _colon();

        printSwitchMemberStatements(x);
        return false;
    }

    @Override
    public boolean visit(JsWhile x, JsContext ctx) {
        _while();
        spaceOpt();
        leftParen();
        accept(x.getCondition());
        rightParen();
        _nestedPush(x.getBody());
        accept(x.getBody());
        _nestedPop(x.getBody());
        return false;
    }

    @Override
    public boolean visit(JsDoWhile x, JsContext ctx) {
        _do();
        _nestedPush(x.getBody());
        accept(x.getBody());
        _nestedPop(x.getBody());
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
        return false;
    }

    @Override
    public boolean visit(JsEmpty x, JsContext ctx) {
        return false;
    }

    @Override
    public boolean visit(JsExprStmt x, JsContext ctx) {
        boolean surroundWithParentheses = JsFirstExpressionVisitor.exec(x);
        if (surroundWithParentheses) {
            leftParen();
        }
        accept(x.getExpression());
        if (surroundWithParentheses) {
            rightParen();
        }
        return false;
    }

    @Override
    public boolean visit(JsFor x, JsContext ctx) {
        _for();
        spaceOpt();
        leftParen();

        // The init expressions or var decl.
        //
        if (x.getInitExpr() != null) {
            accept(x.getInitExpr());
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
        if (x.getIncrExpr() != null) {
            spaceOpt();
            accept(x.getIncrExpr());
        }

        rightParen();
        _nestedPush(x.getBody());
        accept(x.getBody());
        _nestedPop(x.getBody());
        return false;
    }

    @Override
    public boolean visit(JsForIn x, JsContext ctx) {
        _for();
        spaceOpt();
        leftParen();

        if (x.getIterVarName() != null) {
            var();
            space();
            nameDef(x.getIterVarName());

            if (x.getIterExpr() != null) {
                spaceOpt();
                assignment();
                spaceOpt();
                accept(x.getIterExpr());
            }
        }
        else {
            // Just a name ref.
            //
            accept(x.getIterExpr());
        }

        space();
        _in();
        space();
        accept(x.getObjExpr());

        rightParen();
        _nestedPush(x.getBody());
        accept(x.getBody());
        _nestedPop(x.getBody());
        return false;
    }

    @Override
    public boolean visit(JsFunction x, JsContext ctx) {
        _function();
        space();
        if (x.getName() != null) {
            nameOf(x);
        }

        leftParen();
        boolean sep = false;
        for (Object element : x.getParameters()) {
            JsParameter param = (JsParameter) element;
            sep = sepCommaOptSpace(sep);
            accept(param);
        }
        rightParen();
        space();

        lineBreakAfterBlock = false;
        accept(x.getBody());
        needSemi = true;
        return false;
    }

    @Override
    public boolean visit(JsIf x, JsContext ctx) {
        _if();
        spaceOpt();
        leftParen();
        accept(x.getIfExpression());
        rightParen();
        JsStatement thenStmt = x.getThenStatement();
        _nestedPush(thenStmt);
        accept(thenStmt);
        _nestedPop(thenStmt);
        JsStatement elseStmt = x.getElseStatement();
        if (elseStmt != null) {
            if (needSemi) {
                semi();
                newlineOpt();
            }
            else {
                spaceOpt();
                needSemi = true;
            }
            _else();
            boolean elseIf = elseStmt instanceof JsIf;
            if (!elseIf) {
                _nestedPush(elseStmt);
            }
            else {
                space();
            }
            accept(elseStmt);
            if (!elseIf) {
                _nestedPop(elseStmt);
            }
        }
        return false;
    }

    @Override
    public boolean visit(JsInvocation x, JsContext ctx) {
        JsExpression qualifier = x.getQualifier();
        printPair(x, qualifier);

        leftParen();
        printExpressions(x.getArguments());
        rightParen();
        return false;
    }

    @Override
    public boolean visit(JsLabel x, JsContext ctx) {
        nameOf(x);
        _colon();
        spaceOpt();
        accept(x.getStmt());
        return false;
    }

    @Override
    public boolean visit(JsNameRef x, JsContext ctx) {
        JsExpression q = x.getQualifier();
        if (q != null) {
            _parenPush(x, q, false);
            if (q instanceof JsNumberLiteral) {
                /**
                 * Fix for Issue #3796. "42.foo" is not allowed, but "(42).foo" is.
                 */
                leftParen();
            }
            accept(q);
            if (q instanceof JsNumberLiteral) {
                rightParen();
            }
            parenPop(x, q, false);
            _dot();
        }
        _nameRef(x);
        return false;
    }

    @Override
    public boolean visit(JsNew x, JsContext ctx) {
        _new();
        space();

        JsExpression ctorExpr = x.getConstructorExpression();
        boolean needsParens = JsConstructExpressionVisitor.exec(ctorExpr);
        if (needsParens) {
            leftParen();
        }
        accept(ctorExpr);
        if (needsParens) {
            rightParen();
        }

        leftParen();
        printExpressions(x.getArguments());
        rightParen();

        return false;
    }

    @Override
    public boolean visit(JsNullLiteral x, JsContext ctx) {
        _null();
        return false;
    }

    @Override
    public boolean visit(JsIntLiteral x, JsContext ctx) {
        p.print(x.value);
        return false;
    }

    @Override
    public boolean visit(JsDoubleLiteral x, JsContext ctx) {
        p.print(x.value);
        return false;
    }

    @Override
    public boolean visit(JsObjectLiteral objectLiteral, JsContext context) {
        _lbrace();
        if (objectLiteral.isMultiline()) {
            p.indentIn();
        }

        boolean isNotFirst = false;
        for (JsPropertyInitializer item : objectLiteral.getPropertyInitializers()) {
            if (isNotFirst) {
                p.print(',');
            }

            if (objectLiteral.isMultiline()) {
                newlineOpt();
            }
            else if (isNotFirst) {
                spaceOpt();
            }

            isNotFirst = true;

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

        _rbrace();
        return false;
    }

    @Override
    public boolean visit(JsParameter x, JsContext ctx) {
        nameOf(x);
        return false;
    }

    @Override
    public boolean visit(JsPostfixOperation x, JsContext ctx) {
        JsUnaryOperator op = x.getOperator();
        JsExpression arg = x.getArg();
        // unary operators always associate correctly (I think)
        printPair(x, arg);
        p.print(op.getSymbol());
        return false;
    }

    @Override
    public boolean visit(JsPrefixOperation x, JsContext ctx) {
        JsUnaryOperator op = x.getOperator();
        p.print(op.getSymbol());
        JsExpression arg = x.getArg();
        if (spaceCalc(op, arg)) {
            space();
        }
        // unary operators always associate correctly (I think)
        printPair(x, arg);
        return false;
    }

    @Override
    public boolean visit(JsProgram x, JsContext ctx) {
        p.print("<JsProgram>");
        return false;
    }

    @Override
    public boolean visit(JsProgramFragment x, JsContext ctx) {
        p.print("<JsProgramFragment>");
        return false;
    }

    @Override
    public boolean visit(JsPropertyInitializer x, JsContext ctx) {
        // Since there are separators, we actually print the property init
        // in visit(JsObjectLiteral).
        //
        return false;
    }

    @Override
    public boolean visit(JsRegExp x, JsContext ctx) {
        _slash();
        p.print(x.getPattern());
        _slash();
        String flags = x.getFlags();
        if (flags != null) {
            p.print(flags);
        }
        return false;
    }

    @Override
    public boolean visit(JsReturn x, JsContext ctx) {
        _return();
        JsExpression expr = x.getExpr();
        if (expr != null) {
            space();
            accept(expr);
        }
        return false;
    }

    @Override
    public boolean visit(JsStringLiteral x, JsContext ctx) {
        printStringLiteral(x.getValue());
        return false;
    }

    @Override
    public boolean visit(JsSwitch x, JsContext ctx) {
        _switch();
        spaceOpt();
        leftParen();
        accept(x.getExpr());
        rightParen();
        spaceOpt();
        _blockOpen();
        acceptList(x.getCases());
        _blockClose();
        return false;
    }

    @Override
    public boolean visit(JsLiteral.JsThisRef x, JsContext ctx) {
        p.print(CHARS_THIS);
        return false;
    }

    @Override
    public boolean visit(JsThrow x, JsContext ctx) {
        p.print(CHARS_THROW);
        space();
        accept(x.getExpression());
        return false;
    }

    @Override
    public boolean visit(JsTry x, JsContext ctx) {
        p.print(CHARS_TRY);
        spaceOpt();
        accept(x.getTryBlock());

        acceptList(x.getCatches());

        JsBlock finallyBlock = x.getFinallyBlock();
        if (finallyBlock != null) {
            spaceOpt();
            _finally();
            spaceOpt();
            accept(finallyBlock);
        }

        return false;
    }

    @Override
    public boolean visit(JsVar var, JsContext ctx) {
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
        return false;
    }

    @Override
    public boolean visit(JsVars vars, JsContext context) {
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
        return false;
    }

    protected void _newline() {
        p.newline();
    }

    protected void newlineOpt() {
        p.newlineOpt();
    }

    protected void printJsBlock(JsBlock x, boolean truncate, boolean finalNewline) {
        if (!lineBreakAfterBlock) {
            finalNewline = false;
            lineBreakAfterBlock = true;
        }

        boolean needBraces = !x.isGlobalBlock();
        if (needBraces) {
            _blockOpen();
        }

        int count = 0;
        Iterator<JsStatement> iterator = x.getStatements().iterator();
        while (iterator.hasNext()) {
            boolean isGlobal = x.isGlobalBlock() || globalBlocks.contains(x);

            if (truncate && count > JSBLOCK_LINES_TO_PRINT) {
                p.print("[...]");
                newlineOpt();
                break;
            }
            JsStatement statement = iterator.next();
            if (statement instanceof JsEmpty) {
                continue;
            }

            needSemi = true;
            boolean shouldRecordPositions = isGlobal && !(statement instanceof JsBlock);
            boolean stmtIsGlobalBlock = false;
            if (isGlobal) {
                if (statement instanceof JsBlock) {
                    // A block inside a global block is still considered global
                    stmtIsGlobalBlock = true;
                    globalBlocks.add((JsBlock) statement);
                }
            }
            if (shouldRecordPositions) {
                statementStarts.add(p.getPosition());
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
                        statement instanceof JsExprStmt && ((JsExprStmt) statement).getExpression() instanceof JsFunction;
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
                        _newline();
                    }
                }
                else {
                    if (lastStatement) {
                        semiOpt();
                    }
                    else {
                        semi();
                    }
                    newlineOpt();
                }
            }
            if (shouldRecordPositions) {
                assert (statementStarts.size() == statementEnds.size() + 1);
                statementEnds.add(p.getPosition());
            }
            ++count;
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

    private void _blockClose() {
        p.indentOut();
        p.print('}');
        newlineOpt();
    }

    private void _blockOpen() {
        p.print('{');
        p.indentIn();
        newlineOpt();
    }

    private void _break() {
        p.print(CHARS_BREAK);
    }

    private void _case() {
        p.print(CHARS_CASE);
    }

    private void _catch() {
        p.print(CHARS_CATCH);
    }

    private void _colon() {
        p.print(':');
    }

    private void _continue() {
        p.print(CHARS_CONTINUE);
    }

    private void _debugger() {
        p.print(CHARS_DEBUGGER);
    }

    private void _default() {
        p.print(CHARS_DEFAULT);
    }

    private void _do() {
        p.print(CHARS_DO);
    }

    private void _dot() {
        p.print('.');
    }

    private void _else() {
        p.print(CHARS_ELSE);
    }

    private void _false() {
        p.print(CHARS_FALSE);
    }

    private void _finally() {
        p.print(CHARS_FINALLY);
    }

    private void _for() {
        p.print(CHARS_FOR);
    }

    private void _function() {
        p.print(CHARS_FUNCTION);
    }

    private void _if() {
        p.print(CHARS_IF);
    }

    private void _in() {
        p.print(CHARS_IN);
    }

    private void _lbrace() {
        p.print('{');
    }

    private void leftParen() {
        p.print('(');
    }

    private void _lsquare() {
        p.print('[');
    }

    private void nameDef(JsName name) {
        p.print(name.getIdent());
    }

    private void nameOf(HasName hasName) {
        nameDef(hasName.getName());
    }

    private void _nameRef(JsNameRef nameRef) {
        p.print(nameRef.getIdent());
    }

    private boolean _nestedPop(JsStatement statement) {
        boolean pop = !(statement instanceof JsBlock);
        if (pop) {
            p.indentOut();
        }
        return pop;
    }

    private boolean _nestedPush(JsStatement statement) {
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

    private void _new() {
        p.print(CHARS_NEW);
    }

    private void _null() {
        p.print(CHARS_NULL);
    }

    private static boolean parenCalc(JsExpression parent, JsExpression child, boolean wrongAssoc) {
        int parentPrec = JsPrecedenceVisitor.exec(parent);
        int childPrec = JsPrecedenceVisitor.exec(child);
        return parentPrec > childPrec || parentPrec == childPrec && wrongAssoc;
    }

    private boolean parenPop(JsExpression parent, JsExpression child, boolean wrongAssoc) {
        boolean doPop = parenCalc(parent, child, wrongAssoc);
        if (doPop) {
            rightParen();
        }
        return doPop;
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

    private boolean _parenPush(JsExpression parent, JsExpression child, boolean wrongAssoc) {
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

    private void questionMark() {
        p.print('?');
    }

    private void _rbrace() {
        p.print('}');
    }

    private void _return() {
        p.print(CHARS_RETURN);
    }

    private void rightParen() {
        p.print(')');
    }

    private void _rsquare() {
        p.print(']');
    }

    private void semi() {
        p.print(';');
    }

    private void semiOpt() {
        p.printOpt(';');
    }

    private boolean sepCommaOptSpace(boolean sep) {
        if (sep) {
            p.print(',');
            spaceOpt();
        }
        return true;
    }

    private void _slash() {
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
            if (binary.getOperator().getPrecedence() > op.getPrecedence()) {
                return spaceCalc(op, binary.getArg1());
            }
            return false;
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

    private void _switch() {
        p.print(CHARS_SWITCH);
    }

    private void var() {
        p.print(CHARS_VAR);
    }

    private void _while() {
        p.print(CHARS_WHILE);
    }

    private void indent() {
        p.indentIn();
    }

    private void outdent() {
        p.indentOut();
    }

    private void printStringLiteral(String value) {
        p.print(javaScriptString(value));
    }
}
