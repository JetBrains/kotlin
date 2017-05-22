/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * The contents of this file are subject to the Netscape Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/NPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express oqr
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is Netscape
 * Communications Corporation.  Portions created by Netscape are
 * Copyright (C) 1997-1999 Netscape Communications Corporation. All
 * Rights Reserved.
 *
 * Contributor(s): 
 * Mike Ang
 * Mike McCabe
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the GNU Public License (the "GPL"), in which case the
 * provisions of the GPL are applicable instead of those above.
 * If you wish to allow use of your version of this file only
 * under the terms of the GPL and not to allow others to use your
 * version of this file under the NPL, indicate your decision by
 * deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL.  If you do not delete
 * the provisions above, a recipient may use your version of this
 * file under either the NPL or the GPL.
 */
// Modified by Google

package com.google.gwt.dev.js.rhino;

import org.jetbrains.kotlin.js.parser.ParserEvents;

import java.io.IOException;
import java.util.Observable;

/**
 * This class implements the JavaScript parser.
 * <p>
 * It is based on the C source files jsparse.c and jsparse.h in the jsref
 * package.
 *
 * @see TokenStream
 */

public class Parser extends Observable {
    public Parser(IRFactory nf, boolean insideFunction) {
        this.nf = nf;
        this.insideFunction = insideFunction;
    }

    private void mustMatchToken(TokenStream ts, int toMatch, String messageId) throws IOException, JavaScriptException {
        int tt;
        if ((tt = ts.getToken()) != toMatch) {
            reportError(ts, messageId);
            ts.ungetToken(tt); // In case the parser decides to continue
        }
    }

    private void reportError(TokenStream ts, String messageId) throws JavaScriptException {
        this.ok = false;
        ts.reportSyntaxError(messageId, null);

        /*
         * Throw an exception to unwind the recursive descent parse. We use
         * JavaScriptException here even though it is really a different use of the
         * exception than it is usually used for.
         */
        throw new JavaScriptException(messageId);
    }

    /*
     * Build a parse tree from the given TokenStream.
     *
     * @param ts the TokenStream to parse
     *
     * @return an Object representing the parsed program. If the parse fails, null
     * will be returned. (The parse failure will result in a call to the current
     * Context's ErrorReporter.)
     */
    public Node parse(TokenStream ts) throws IOException {
        this.ok = true;
        sourceTop = 0;
        functionNumber = 0;

        int tt; // last token from getToken();

        /*
         * so we have something to add nodes to until we've collected all the source
         */
        Node tempBlock = nf.createLeaf(TokenStream.BLOCK, ts.tokenPosition);

        while (true) {
            ts.flags |= TokenStream.TSF_REGEXP;
            tt = ts.getToken();
            ts.flags &= ~TokenStream.TSF_REGEXP;

            if (tt <= TokenStream.EOF) {
                break;
            }

            if (tt == TokenStream.FUNCTION) {
                try {
                    tempBlock.addChildToBack(function(ts, false));
                }
                catch (JavaScriptException e) {
                    this.ok = false;
                    break;
                }
            }
            else {
                ts.ungetToken(tt);
                tempBlock.addChildToBack(statement(ts));
            }
        }

        if (!this.ok) {
            // XXX ts.clearPushback() call here?
            return null;
        }

        return nf.createScript(tempBlock);
    }

    /*
     * The C version of this function takes an argument list, which doesn't seem
     * to be needed for tree generation... it'd only be useful for checking
     * argument hiding, which I'm not doing anyway...
     */
    private Node parseFunctionBody(TokenStream ts) throws IOException {
        int oldflags = ts.flags;
        ts.flags &= ~(TokenStream.TSF_RETURN_EXPR | TokenStream.TSF_RETURN_VOID);
        ts.flags |= TokenStream.TSF_FUNCTION;

        Node pn = nf.createBlock(ts.tokenPosition);
        try {
            int tt;
            while ((tt = ts.peekToken()) > TokenStream.EOF && tt != TokenStream.RC) {
                if (tt == TokenStream.FUNCTION) {
                    ts.getToken();
                    pn.addChildToBack(function(ts, false));
                }
                else {
                    pn.addChildToBack(statement(ts));
                }
            }
        }
        catch (JavaScriptException e) {
            this.ok = false;
        }
        finally {
            // also in finally block:
            // flushNewLines, clearPushback.

            ts.flags = oldflags;
        }

        return pn;
    }

    private Node function(TokenStream ts, boolean isExpr) throws IOException, JavaScriptException {
        notifyObservers(new ParserEvents.OnFunctionParsingStart());
        CodePosition basePosition = ts.tokenPosition;

        Node nameNode;
        Node memberExprNode = null;
        if (ts.matchToken(TokenStream.NAME)) {
            nameNode = nf.createName(ts.getString(), basePosition);
            if (!ts.matchToken(TokenStream.LP)) {
                if (Context.getContext().hasFeature(Context.FEATURE_MEMBER_EXPR_AS_FUNCTION_NAME)) {
                    // Extension to ECMA: if 'function <name>' does not follow
                    // by '(', assume <name> starts memberExpr
                    Node memberExprHead = nameNode;
                    nameNode = null;
                    memberExprNode = memberExprTail(ts, false, memberExprHead, basePosition);
                }
                mustMatchToken(ts, TokenStream.LP, "msg.no.paren.parms");
            }
        }
        else if (ts.matchToken(TokenStream.LP)) {
            // Anonymous function
            nameNode = null;
        }
        else {
            if (Context.getContext().hasFeature(Context.FEATURE_MEMBER_EXPR_AS_FUNCTION_NAME)) {
                // Note that memberExpr can not start with '(' like
                // in (1+2).toString, because 'function (' already
                // processed as anonymous function
                memberExprNode = memberExpr(ts, false);
            }
            mustMatchToken(ts, TokenStream.LP, "msg.no.paren.parms");
            nameNode = null;
        }

        ++functionNumber;

        // Save current source top to restore it on exit not to include
        // function to parent source
        int savedSourceTop = sourceTop;
        int savedFunctionNumber = functionNumber;
        Node args;
        Node body;
        try {
            functionNumber = 0;
            args = nf.createLeaf(TokenStream.LP, ts.tokenPosition);

            if (!ts.matchToken(TokenStream.GWT)) {
                do {
                    CodePosition namePosition = ts.tokenPosition;
                    mustMatchToken(ts, TokenStream.NAME, "msg.no.parm");
                    String s = ts.getString();
                    args.addChildToBack(nf.createName(s, namePosition));
                }
                while (ts.matchToken(TokenStream.COMMA));

                mustMatchToken(ts, TokenStream.GWT, "msg.no.paren.after.parms");
            }

            mustMatchToken(ts, TokenStream.LC, "msg.no.brace.body");
            body = parseFunctionBody(ts);
            mustMatchToken(ts, TokenStream.RC, "msg.no.brace.after.body");
            // skip the last EOL so nested functions work...
        }
        finally {
            sourceTop = savedSourceTop;
            functionNumber = savedFunctionNumber;
        }

        Node pn = nf.createFunction(nameNode, args, body, basePosition);
        if (memberExprNode != null) {
            pn = nf.createBinary(TokenStream.ASSIGN, TokenStream.NOP, memberExprNode, pn, basePosition);
        }

        // Add EOL but only if function is not part of expression, in which
        // case it gets SEMI + EOL from Statement.
        if (!isExpr) {
            wellTerminated(ts, TokenStream.FUNCTION);
        }

        notifyObservers(new ParserEvents.OnFunctionParsingEnd(ts));
        return pn;
    }

    private Node statements(TokenStream ts) throws IOException {
        Node pn = nf.createBlock(ts.tokenPosition);

        int tt;
        while ((tt = ts.peekToken()) > TokenStream.EOF && tt != TokenStream.RC) {
            pn.addChildToBack(statement(ts));
        }

        return pn;
    }

    private Node condition(TokenStream ts) throws IOException, JavaScriptException {
        Node pn;
        mustMatchToken(ts, TokenStream.LP, "msg.no.paren.cond");
        pn = expr(ts, false);
        mustMatchToken(ts, TokenStream.GWT, "msg.no.paren.after.cond");

        // there's a check here in jsparse.c that corrects = to ==

        return pn;
    }

    private void wellTerminated(TokenStream ts, int lastExprType) throws IOException, JavaScriptException {
        int tt = ts.peekTokenSameLine();
        if (tt == TokenStream.ERROR) {
            return;
        }

        if (tt != TokenStream.EOF && tt != TokenStream.EOL && tt != TokenStream.SEMI && tt != TokenStream.RC) {
            int version = Context.getContext().getLanguageVersion();
            if ((tt == TokenStream.FUNCTION || lastExprType == TokenStream.FUNCTION) && (version < Context.VERSION_1_2)) {
                /*
                 * Checking against version < 1.2 and version >= 1.0 in the above line
                 * breaks old javascript, so we keep it this way for now... XXX warning
                 * needed?
                 */
            }
            else {
                reportError(ts, "msg.no.semi.stmt");
            }
        }
    }

    // match a NAME; return null if no match.
    private Node matchLabel(TokenStream ts) throws IOException, JavaScriptException {
        CodePosition position = ts.tokenPosition;
        int lineno = ts.getLineno();

        String label = null;
        int tt;
        tt = ts.peekTokenSameLine();
        if (tt == TokenStream.NAME) {
            ts.getToken();
            label = ts.getString();
        }

        if (lineno == ts.getLineno()) {
            wellTerminated(ts, TokenStream.ERROR);
        }

        return label != null ? nf.createString(label, position) : null;
    }

    private Node statement(TokenStream ts) throws IOException {
        CodePosition position = ts.lastPosition;
        try {
            return statementHelper(ts);
        }
        catch (JavaScriptException e) {
            // skip to end of statement
            int t;
            do {
                t = ts.getToken();
            }
            while (t != TokenStream.SEMI && t != TokenStream.EOL
                   && t != TokenStream.EOF && t != TokenStream.ERROR);
            return nf.createExprStatement(nf.createName("error", position), position);
        }
    }

    /**
     * Whether the "catch (e: e instanceof Exception) { ... }" syntax is
     * implemented.
     */

    private Node statementHelper(TokenStream ts) throws IOException, JavaScriptException {
        Node pn;

        int tt;

        int lastExprType; // For wellTerminated

        tt = ts.getToken();
        CodePosition position = ts.tokenPosition;

        switch (tt) {
            case TokenStream.IF: {
                Node cond = condition(ts);
                Node ifTrue = statement(ts);
                Node ifFalse = null;
                if (ts.matchToken(TokenStream.ELSE)) {
                    ifFalse = statement(ts);
                }
                pn = nf.createIf(cond, ifTrue, ifFalse, position);
                break;
            }

            case TokenStream.SWITCH: {
                pn = nf.createSwitch(position);

                Node curCase = null; // to kill warning
                Node caseStatements;

                mustMatchToken(ts, TokenStream.LP, "msg.no.paren.switch");
                pn.addChildToBack(expr(ts, false));
                mustMatchToken(ts, TokenStream.GWT, "msg.no.paren.after.switch");
                mustMatchToken(ts, TokenStream.LC, "msg.no.brace.switch");

                while ((tt = ts.getToken()) != TokenStream.RC && tt != TokenStream.EOF) {
                    switch (tt) {
                        case TokenStream.CASE:
                            curCase = nf.createUnary(TokenStream.CASE, expr(ts, false), ts.tokenPosition);
                            break;

                        case TokenStream.DEFAULT:
                            curCase = nf.createLeaf(TokenStream.DEFAULT, ts.tokenPosition);
                            // XXX check that there isn't more than one default
                            break;

                        default:
                            reportError(ts, "msg.bad.switch");
                            break;
                    }
                    mustMatchToken(ts, TokenStream.COLON, "msg.no.colon.case");

                    caseStatements = nf.createLeaf(TokenStream.BLOCK, null);

                    while ((tt = ts.peekToken()) != TokenStream.RC && tt != TokenStream.CASE
                           && tt != TokenStream.DEFAULT && tt != TokenStream.EOF) {
                        caseStatements.addChildToBack(statement(ts));
                    }
                    // assert cur_case
                    if (curCase != null) {
                        curCase.addChildToBack(caseStatements);
                    }

                    pn.addChildToBack(curCase);
                }
                break;
            }

            case TokenStream.WHILE: {
                Node cond = condition(ts);
                Node body = statement(ts);

                pn = nf.createWhile(cond, body, position);
                break;
            }

            case TokenStream.DO: {
                Node body = statement(ts);

                mustMatchToken(ts, TokenStream.WHILE, "msg.no.while.do");
                Node cond = condition(ts);

                pn = nf.createDoWhile(body, cond, position);
                break;
            }

            case TokenStream.FOR: {
                Node init; // Node init is also foo in 'foo in Object'
                Node cond; // Node cond is also object in 'foo in Object'
                Node incr = null; // to kill warning
                Node body;

                mustMatchToken(ts, TokenStream.LP, "msg.no.paren.for");
                tt = ts.peekToken();
                if (tt == TokenStream.SEMI) {
                    init = nf.createLeaf(TokenStream.VOID, null);
                }
                else {
                    if (tt == TokenStream.VAR) {
                        // set init to a var list or initial
                        ts.getToken(); // throw away the 'var' token
                        init = variables(ts, true, ts.tokenPosition);
                    }
                    else {
                        init = expr(ts, true);
                    }
                }

                tt = ts.peekToken();
                if (tt == TokenStream.RELOP && ts.getOp() == TokenStream.IN) {
                    ts.matchToken(TokenStream.RELOP);
                    // 'cond' is the object over which we're iterating
                    cond = expr(ts, false);
                }
                else { // ordinary for loop
                    mustMatchToken(ts, TokenStream.SEMI, "msg.no.semi.for");
                    if (ts.peekToken() == TokenStream.SEMI) {
                        // no loop condition
                        cond = nf.createLeaf(TokenStream.VOID, null);
                    }
                    else {
                        cond = expr(ts, false);
                    }

                    mustMatchToken(ts, TokenStream.SEMI, "msg.no.semi.for.cond");
                    if (ts.peekToken() == TokenStream.GWT) {
                        incr = nf.createLeaf(TokenStream.VOID, null);
                    }
                    else {
                        incr = expr(ts, false);
                    }
                }

                mustMatchToken(ts, TokenStream.GWT, "msg.no.paren.for.ctrl");
                body = statement(ts);

                if (incr == null) {
                    // cond could be null if 'in obj' got eaten by the init node.
                    pn = nf.createForIn(init, cond, body, position);
                }
                else {
                    pn = nf.createFor(init, cond, incr, body, position);
                }
                break;
            }

            case TokenStream.TRY: {
                Node tryblock;
                Node catchblocks;
                Node finallyblock = null;

                tryblock = statement(ts);

                catchblocks = nf.createLeaf(TokenStream.BLOCK, null);

                boolean sawDefaultCatch = false;
                int peek = ts.peekToken();
                if (peek == TokenStream.CATCH) {
                    while (ts.matchToken(TokenStream.CATCH)) {
                        if (sawDefaultCatch) {
                            reportError(ts, "msg.catch.unreachable");
                        }
                        CodePosition catchPosition = ts.tokenPosition;
                        mustMatchToken(ts, TokenStream.LP, "msg.no.paren.catch");

                        mustMatchToken(ts, TokenStream.NAME, "msg.bad.catchcond");
                        Node varName = nf.createName(ts.getString(), ts.tokenPosition);

                        Node catchCond = null;
                        if (ts.matchToken(TokenStream.IF)) {
                            catchCond = expr(ts, false);
                        }
                        else {
                            sawDefaultCatch = true;
                        }

                        mustMatchToken(ts, TokenStream.GWT, "msg.bad.catchcond");
                        mustMatchToken(ts, TokenStream.LC, "msg.no.brace.catchblock");

                        catchblocks.addChildToBack(nf.createCatch(varName, catchCond, statements(ts), catchPosition));

                        mustMatchToken(ts, TokenStream.RC, "msg.no.brace.after.body");
                    }
                }
                else if (peek != TokenStream.FINALLY) {
                    mustMatchToken(ts, TokenStream.FINALLY, "msg.try.no.catchfinally");
                }

                if (ts.matchToken(TokenStream.FINALLY)) {
                    finallyblock = statement(ts);
                }

                pn = nf.createTryCatchFinally(tryblock, catchblocks, finallyblock, position);

                break;
            }
            case TokenStream.THROW: {
                int lineno = ts.getLineno();
                pn = nf.createThrow(expr(ts, false), position);
                if (lineno == ts.getLineno()) {
                    wellTerminated(ts, TokenStream.ERROR);
                }
                break;
            }
            case TokenStream.BREAK: {
                // matchLabel only matches if there is one
                Node label = matchLabel(ts);
                pn = nf.createBreak(label, position);
                break;
            }
            case TokenStream.CONTINUE: {
                // matchLabel only matches if there is one
                Node label = matchLabel(ts);
                pn = nf.createContinue(label, position);
                break;
            }
            case TokenStream.DEBUGGER: {
                pn = nf.createDebugger(position);
                break;
            }
            case TokenStream.WITH: {
                // bruce: we don't support this is JSNI code because it's impossible
                // to identify bindings even passably well
                //

                reportError(ts, "msg.jsni.unsupported.with");

                mustMatchToken(ts, TokenStream.LP, "msg.no.paren.with");
                Node obj = expr(ts, false);
                mustMatchToken(ts, TokenStream.GWT, "msg.no.paren.after.with");
                Node body = statement(ts);
                pn = nf.createWith(obj, body, position);
                break;
            }
            case TokenStream.VAR: {
                int lineno = ts.getLineno();
                pn = variables(ts, false, position);
                if (ts.getLineno() == lineno) {
                    wellTerminated(ts, TokenStream.ERROR);
                }
                break;
            }
            case TokenStream.RETURN: {
                Node retExpr = null;
                int lineno;
                // bail if we're not in a (toplevel) function
                if ((!insideFunction) && ((ts.flags & TokenStream.TSF_FUNCTION) == 0)) {
                    reportError(ts, "msg.bad.return");
                }

                /* This is ugly, but we don't want to require a semicolon. */
                ts.flags |= TokenStream.TSF_REGEXP;
                tt = ts.peekTokenSameLine();
                ts.flags &= ~TokenStream.TSF_REGEXP;

                if (tt != TokenStream.EOF && tt != TokenStream.EOL && tt != TokenStream.SEMI && tt != TokenStream.RC) {
                    lineno = ts.getLineno();
                    retExpr = expr(ts, false);
                    if (ts.getLineno() == lineno) {
                        wellTerminated(ts, TokenStream.ERROR);
                    }
                    ts.flags |= TokenStream.TSF_RETURN_EXPR;
                }
                else {
                    ts.flags |= TokenStream.TSF_RETURN_VOID;
                }

                // XXX ASSERT pn
                pn = nf.createReturn(retExpr, position);
                break;
            }
            case TokenStream.LC:
                pn = statements(ts);
                mustMatchToken(ts, TokenStream.RC, "msg.no.brace.block");
                break;

            case TokenStream.ERROR:
                // Fall thru, to have a node for error recovery to work on
            case TokenStream.EOL:
            case TokenStream.SEMI:
                pn = nf.createLeaf(TokenStream.VOID, ts.tokenPosition);
                break;

            default: {
                lastExprType = tt;
                int tokenno = ts.getTokenno();
                ts.ungetToken(tt);
                int lineno = ts.getLineno();

                pn = expr(ts, false);

                if (ts.peekToken() == TokenStream.COLON) {
                    /*
                     * check that the last thing the tokenizer returned was a NAME and
                     * that only one token was consumed.
                     */
                    if (lastExprType != TokenStream.NAME || (ts.getTokenno() != tokenno)) {
                        reportError(ts, "msg.bad.label");
                    }

                    ts.getToken(); // eat the COLON

                    /*
                     * in the C source, the label is associated with the statement that
                     * follows: nf.addChildToBack(pn, statement(ts));
                     */
                    String name = ts.getString();
                    pn = nf.createLabel(nf.createString(name, position), position);

                    // bruce: added to make it easier to bind labels to the
                    // statements they modify
                    //
                    pn.addChildToBack(statement(ts));

                    // depend on decompiling lookahead to guess that that
                    // last name was a label.
                    return pn;
                }

                if (lastExprType == TokenStream.FUNCTION) {
                    if (nf.getLeafType(pn) != TokenStream.FUNCTION) {
                        reportError(ts, "msg.syntax");
                    }
                }

                pn = nf.createExprStatement(pn, position);

                /*
                 * Check explicitly against (multi-line) function statement.
                 *
                 * lastExprEndLine is a hack to fix an automatic semicolon insertion
                 * problem with function expressions; the ts.getLineno() == lineno check
                 * was firing after a function definition even though the next statement
                 * was on a new line, because speculative getToken calls advanced the
                 * line number even when they didn't succeed.
                 */
                if (ts.getLineno() == lineno || (lastExprType == TokenStream.FUNCTION && ts.getLineno() == lastExprEndLine)) {
                    wellTerminated(ts, lastExprType);
                }
                break;
            }
        }
        ts.matchToken(TokenStream.SEMI);

        return pn;
    }

    private Node variables(TokenStream ts, boolean inForInit, CodePosition position) throws IOException, JavaScriptException {
        Node pn = nf.createVariables(position);

        while (true) {
            Node name;
            Node init;
            mustMatchToken(ts, TokenStream.NAME, "msg.bad.var");
            String s = ts.getString();
            name = nf.createName(s, ts.tokenPosition);

            // omitted check for argument hiding

            if (ts.matchToken(TokenStream.ASSIGN)) {
                if (ts.getOp() != TokenStream.NOP) {
                    reportError(ts, "msg.bad.var.init");
                }

                init = assignExpr(ts, inForInit);
                name.addChildToBack(init);
            }
            pn.addChildToBack(name);
            if (!ts.matchToken(TokenStream.COMMA)) {
                break;
            }
        }
        return pn;
    }

    private Node expr(TokenStream ts, boolean inForInit) throws IOException, JavaScriptException {
        CodePosition position = getNextTokenPosition(ts);
        Node pn = assignExpr(ts, inForInit);
        while (ts.matchToken(TokenStream.COMMA)) {
            pn = nf.createBinary(TokenStream.COMMA, pn, assignExpr(ts, inForInit), position);
        }
        return pn;
    }

    private Node assignExpr(TokenStream ts, boolean inForInit) throws IOException, JavaScriptException {
        CodePosition position = getNextTokenPosition(ts);
        Node pn = condExpr(ts, inForInit);

        if (ts.matchToken(TokenStream.ASSIGN)) {
            // omitted: "invalid assignment left-hand side" check.
            pn = nf.createBinary(TokenStream.ASSIGN, ts.getOp(), pn, assignExpr(ts, inForInit), position);
        }

        return pn;
    }

    private Node condExpr(TokenStream ts, boolean inForInit) throws IOException, JavaScriptException {
        CodePosition position = getNextTokenPosition(ts);
        Node pn = orExpr(ts, inForInit);

        if (ts.matchToken(TokenStream.HOOK)) {
            Node ifTrue = assignExpr(ts, false);
            mustMatchToken(ts, TokenStream.COLON, "msg.no.colon.cond");
            Node ifFalse = assignExpr(ts, inForInit);
            return nf.createTernary(pn, ifTrue, ifFalse, position);
        }

        return pn;
    }

    private Node orExpr(TokenStream ts, boolean inForInit) throws IOException, JavaScriptException {
        CodePosition position = getNextTokenPosition(ts);
        Node pn = andExpr(ts, inForInit);
        while (ts.matchToken(TokenStream.OR)) {
            pn = nf.createBinary(TokenStream.OR, pn, andExpr(ts, inForInit), position);
        }

        return pn;
    }

    private Node andExpr(TokenStream ts, boolean inForInit) throws IOException, JavaScriptException {
        CodePosition position = getNextTokenPosition(ts);
        Node pn = bitOrExpr(ts, inForInit);
        while (ts.matchToken(TokenStream.AND)) {
            pn = nf.createBinary(TokenStream.AND, pn, bitOrExpr(ts, inForInit), position);
        }

        return pn;
    }

    private Node bitOrExpr(TokenStream ts, boolean inForInit) throws IOException, JavaScriptException {
        CodePosition position = getNextTokenPosition(ts);
        Node pn = bitXorExpr(ts, inForInit);
        while (ts.matchToken(TokenStream.BITOR)) {
            pn = nf.createBinary(TokenStream.BITOR, pn, bitXorExpr(ts, inForInit), position);
        }
        return pn;
    }

    private Node bitXorExpr(TokenStream ts, boolean inForInit) throws IOException, JavaScriptException {
        CodePosition position = getNextTokenPosition(ts);
        Node pn = bitAndExpr(ts, inForInit);
        while (ts.matchToken(TokenStream.BITXOR)) {
            pn = nf.createBinary(TokenStream.BITXOR, pn, bitAndExpr(ts, inForInit), position);
        }
        return pn;
    }

    private Node bitAndExpr(TokenStream ts, boolean inForInit) throws IOException, JavaScriptException {
        CodePosition position = getNextTokenPosition(ts);
        Node pn = eqExpr(ts, inForInit);
        while (ts.matchToken(TokenStream.BITAND)) {
            pn = nf.createBinary(TokenStream.BITAND, pn, eqExpr(ts, inForInit), position);
        }
        return pn;
    }

    private Node eqExpr(TokenStream ts, boolean inForInit) throws IOException, JavaScriptException {
        CodePosition position = getNextTokenPosition(ts);
        Node pn = relExpr(ts, inForInit);
        while (ts.matchToken(TokenStream.EQOP)) {
            pn = nf.createBinary(TokenStream.EQOP, ts.getOp(), pn, relExpr(ts, inForInit), position);
        }
        return pn;
    }

    private Node relExpr(TokenStream ts, boolean inForInit) throws IOException, JavaScriptException {
        CodePosition position = getNextTokenPosition(ts);
        Node pn = shiftExpr(ts);
        while (ts.matchToken(TokenStream.RELOP)) {
            int op = ts.getOp();
            if (inForInit && op == TokenStream.IN) {
                ts.ungetToken(TokenStream.RELOP);
                break;
            }

            pn = nf.createBinary(TokenStream.RELOP, op, pn, shiftExpr(ts), position);
        }
        return pn;
    }

    private Node shiftExpr(TokenStream ts) throws IOException, JavaScriptException {
        CodePosition position = getNextTokenPosition(ts);
        Node pn = addExpr(ts);
        while (ts.matchToken(TokenStream.SHOP)) {
            pn = nf.createBinary(TokenStream.SHOP, ts.getOp(), pn, addExpr(ts), position);
        }
        return pn;
    }

    private Node addExpr(TokenStream ts) throws IOException, JavaScriptException {
        CodePosition position = getNextTokenPosition(ts);
        int tt;
        Node pn = mulExpr(ts);

        while ((tt = ts.getToken()) == TokenStream.ADD || tt == TokenStream.SUB) {
            // flushNewLines
            pn = nf.createBinary(tt, pn, mulExpr(ts), position);
        }
        ts.ungetToken(tt);

        return pn;
    }

    private Node mulExpr(TokenStream ts) throws IOException, JavaScriptException {
        CodePosition position = getNextTokenPosition(ts);
        int tt;

        Node pn = unaryExpr(ts);

        while ((tt = ts.peekToken()) == TokenStream.MUL || tt == TokenStream.DIV || tt == TokenStream.MOD) {
            tt = ts.getToken();
            pn = nf.createBinary(tt, pn, unaryExpr(ts), position);
        }

        return pn;
    }

    private Node unaryExpr(TokenStream ts) throws IOException, JavaScriptException {
        int tt;

        ts.flags |= TokenStream.TSF_REGEXP;
        tt = ts.getToken();
        ts.flags &= ~TokenStream.TSF_REGEXP;
        CodePosition position = ts.tokenPosition;

        switch (tt) {
            case TokenStream.UNARYOP:
                return nf.createUnary(TokenStream.UNARYOP, ts.getOp(), unaryExpr(ts), position);

            case TokenStream.ADD:
            case TokenStream.SUB:
                return nf.createUnary(TokenStream.UNARYOP, tt, unaryExpr(ts), position);

            case TokenStream.INC:
            case TokenStream.DEC:
                return nf.createUnary(tt, TokenStream.PRE, memberExpr(ts, true), position);

            case TokenStream.DELPROP:
                return nf.createUnary(TokenStream.DELPROP, unaryExpr(ts), position);

            case TokenStream.ERROR:
                break;

            default:
                ts.ungetToken(tt);

                int lineno = ts.getLineno();

                Node pn = memberExpr(ts, true);

                /*
                 * don't look across a newline boundary for a postfix incop.
                 *
                 * the rhino scanner seems to work differently than the js scanner here;
                 * in js, it works to have the line number check precede the peekToken
                 * calls. It'd be better if they had similar behavior...
                 */
                int peeked;
                if (((peeked = ts.peekToken()) == TokenStream.INC || peeked == TokenStream.DEC)
                    && ts.getLineno() == lineno) {
                    int pf = ts.getToken();
                    return nf.createUnary(pf, TokenStream.POST, pn, position);
                }
                return pn;
        }
        return nf.createName("err", position); // Only reached on error. Try to continue.
    }

    private Node argumentList(TokenStream ts, Node listNode) throws IOException, JavaScriptException {
        boolean matched;
        ts.flags |= TokenStream.TSF_REGEXP;
        matched = ts.matchToken(TokenStream.GWT);
        ts.flags &= ~TokenStream.TSF_REGEXP;
        if (!matched) {
            do {
                listNode.addChildToBack(assignExpr(ts, false));
            }
            while (ts.matchToken(TokenStream.COMMA));

            mustMatchToken(ts, TokenStream.GWT, "msg.no.paren.arg");
        }
        return listNode;
    }

    private Node memberExpr(TokenStream ts, boolean allowCallSyntax) throws IOException, JavaScriptException {
        int tt;

        Node pn;
        CodePosition position = ts.tokenPosition;

        /* Check for new expressions. */
        ts.flags |= TokenStream.TSF_REGEXP;
        tt = ts.peekToken();
        ts.flags &= ~TokenStream.TSF_REGEXP;
        if (tt == TokenStream.NEW) {
            /* Eat the NEW token. */
            ts.getToken();

            /* Make a NEW node to append to. */
            pn = nf.createLeaf(TokenStream.NEW, position);
            pn.addChildToBack(memberExpr(ts, false));

            if (ts.matchToken(TokenStream.LP)) {
                /* Add the arguments to pn, if any are supplied. */
                pn = argumentList(ts, pn);
            }

            /*
             * XXX there's a check in the C source against "too many constructor
             * arguments" - how many do we claim to support?
             */

            /*
             * Experimental syntax: allow an object literal to follow a new
             * expression, which will mean a kind of anonymous class built with the
             * JavaAdapter. the object literal will be passed as an additional
             * argument to the constructor.
             */
            tt = ts.peekToken();
            if (tt == TokenStream.LC) {
                pn.addChildToBack(primaryExpr(ts));
            }
        }
        else {
            pn = primaryExpr(ts);
        }

        return memberExprTail(ts, allowCallSyntax, pn, position);
    }

    private Node memberExprTail(
            TokenStream ts, boolean allowCallSyntax,
            Node pn, CodePosition position
    ) throws IOException, JavaScriptException {
        lastExprEndLine = ts.getLineno();
        int tt;
        while ((tt = ts.getToken()) > TokenStream.EOF) {
            if (tt == TokenStream.DOT) {
                ts.treatKeywordAsIdentifier = true;
                mustMatchToken(ts, TokenStream.NAME, "msg.no.name.after.dot");
                ts.treatKeywordAsIdentifier = false;
                pn = nf.createBinary(TokenStream.DOT, pn, nf.createName(ts.getString(), ts.tokenPosition), position);
                /*
                 * pn = nf.createBinary(ts.DOT, pn, memberExpr(ts)) is the version in
                 * Brendan's IR C version. Not in ECMA... does it reflect the 'new'
                 * operator syntax he mentioned?
                 */
                lastExprEndLine = ts.getLineno();
            }
            else if (tt == TokenStream.LB) {
                pn = nf.createBinary(TokenStream.LB, pn, expr(ts, false), position);

                mustMatchToken(ts, TokenStream.RB, "msg.no.bracket.index");
                lastExprEndLine = ts.getLineno();
            }
            else if (allowCallSyntax && tt == TokenStream.LP) {
                /* make a call node */
                pn = nf.createUnary(TokenStream.CALL, pn, position);

                /* Add the arguments to pn, if any are supplied. */
                pn = argumentList(ts, pn);
                lastExprEndLine = ts.getLineno();
            }
            else {
                ts.ungetToken(tt);

                break;
            }
        }
        return pn;
    }

    public Node primaryExpr(TokenStream ts) throws IOException, JavaScriptException {
        int tt;

        Node pn;

        ts.flags |= TokenStream.TSF_REGEXP;
        tt = ts.getToken();
        CodePosition position = ts.tokenPosition;
        ts.flags &= ~TokenStream.TSF_REGEXP;

        switch (tt) {

            case TokenStream.FUNCTION:
                return function(ts, true);

            case TokenStream.LB: {
                pn = nf.createLeaf(TokenStream.ARRAYLIT, position);

                ts.flags |= TokenStream.TSF_REGEXP;
                boolean matched = ts.matchToken(TokenStream.RB);
                ts.flags &= ~TokenStream.TSF_REGEXP;

                if (!matched) {
                    do {
                        ts.flags |= TokenStream.TSF_REGEXP;
                        tt = ts.peekToken();
                        ts.flags &= ~TokenStream.TSF_REGEXP;

                        if (tt == TokenStream.RB) { // to fix [,,,].length behavior...
                            break;
                        }

                        if (tt == TokenStream.COMMA) {
                            pn.addChildToBack(nf.createLeaf(TokenStream.PRIMARY, TokenStream.UNDEFINED, position));
                        }
                        else {
                            pn.addChildToBack(assignExpr(ts, false));
                        }
                    }
                    while (ts.matchToken(TokenStream.COMMA));
                    mustMatchToken(ts, TokenStream.RB, "msg.no.bracket.arg");
                }

                return nf.createArrayLiteral(pn);
            }

            case TokenStream.LC: {
                pn = nf.createLeaf(TokenStream.OBJLIT, position);

                if (!ts.matchToken(TokenStream.RC)) {

                    commaloop:
                    do {
                        Node property;

                        tt = ts.getToken();
                        switch (tt) {
                            // map NAMEs to STRINGs in object literal context.
                            case TokenStream.NAME:
                            case TokenStream.STRING:
                                property = nf.createString(ts.getString(), ts.tokenPosition);
                                break;
                            case TokenStream.NUMBER_INT:
                                property = nf.createIntNumber(ts.getNumber(), ts.tokenPosition);
                                break;
                            case TokenStream.NUMBER:
                                double d = ts.getNumber();
                                property = nf.createNumber(d, ts.tokenPosition);
                                break;
                            case TokenStream.RC:
                                // trailing comma is OK.
                                ts.ungetToken(tt);
                                break commaloop;
                            default:
                                reportError(ts, "msg.bad.prop");
                                break commaloop;
                        }
                        mustMatchToken(ts, TokenStream.COLON, "msg.no.colon.prop");

                        // OBJLIT is used as ':' in object literal for
                        // decompilation to solve spacing ambiguity.
                        pn.addChildToBack(property);
                        pn.addChildToBack(assignExpr(ts, false));
                    }
                    while (ts.matchToken(TokenStream.COMMA));

                    mustMatchToken(ts, TokenStream.RC, "msg.no.brace.prop");
                }
                return nf.createObjectLiteral(pn);
            }

            case TokenStream.LP:

                /*
                 * Brendan's IR-jsparse.c makes a new node tagged with TOK_LP here...
                 * I'm not sure I understand why. Isn't the grouping already implicit in
                 * the structure of the parse tree? also TOK_LP is already overloaded (I
                 * think) in the C IR as 'function call.'
                 */
                pn = expr(ts, false);
                mustMatchToken(ts, TokenStream.GWT, "msg.no.paren");
                return pn;

            case TokenStream.NAME:
                String name = ts.getString();
                return nf.createName(name, position);

            case TokenStream.NUMBER_INT:
                return nf.createIntNumber(ts.getNumber(), position);

            case TokenStream.NUMBER:
                double d = ts.getNumber();
                return nf.createNumber(d, position);

            case TokenStream.STRING:
                String s = ts.getString();
                return nf.createString(s, position);

            case TokenStream.REGEXP: {
                String flags = ts.regExpFlags;
                ts.regExpFlags = null;
                String re = ts.getString();
                return nf.createRegExp(re, flags, position);
            }

            case TokenStream.PRIMARY:
                return nf.createLeaf(TokenStream.PRIMARY, ts.getOp(), position);

            case TokenStream.ERROR:
                /* the scanner or one of its subroutines reported the error. */
                break;

            default:
                reportError(ts, "msg.syntax");
                break;
        }
        return null; // should never reach here
    }

    private static CodePosition getNextTokenPosition(TokenStream ts) throws IOException, JavaScriptException {
        int tt = ts.getToken();
        CodePosition result = ts.tokenPosition;
        ts.ungetToken(tt);
        return result;
    }

    private int lastExprEndLine; // Hack to handle function expr termination.
    private final IRFactory nf;
    private boolean ok; // Did the parse encounter an error?

    private int sourceTop;
    private int functionNumber;
    private final boolean insideFunction;
}
