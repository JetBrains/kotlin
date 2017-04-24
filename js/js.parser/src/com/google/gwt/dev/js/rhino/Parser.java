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
 * 
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

  private void mustMatchToken(TokenStream ts, int toMatch, String messageId)
      throws IOException, JavaScriptException {
    int tt;
    if ((tt = ts.getToken()) != toMatch) {
      reportError(ts, messageId);
      ts.ungetToken(tt); // In case the parser decides to continue
    }
  }

  private void reportError(TokenStream ts, String messageId)
      throws JavaScriptException {
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
  public Object parse(TokenStream ts) throws IOException {
    this.ok = true;
    sourceTop = 0;
    functionNumber = 0;

    int tt; // last token from getToken();
    int baseLineno = ts.getLineno(); // line number where source starts

    /*
     * so we have something to add nodes to until we've collected all the source
     */
    Object tempBlock = nf.createLeaf(TokenStream.BLOCK);
    ((Node) tempBlock).setIsSyntheticBlock(true);

    while (true) {
      ts.flags |= ts.TSF_REGEXP;
      tt = ts.getToken();
      ts.flags &= ~ts.TSF_REGEXP;

      if (tt <= ts.EOF) {
        break;
      }

      if (tt == ts.FUNCTION) {
        try {
          nf.addChildToBack(tempBlock, function(ts, false));
        } catch (JavaScriptException e) {
          this.ok = false;
          break;
        }
      } else {
        ts.ungetToken(tt);
        nf.addChildToBack(tempBlock, statement(ts));
      }
    }

    if (!this.ok) {
      // XXX ts.clearPushback() call here?
      return null;
    }

    Object pn = nf.createScript(tempBlock, ts.getSourceName(), baseLineno, ts
      .getLineno(), null);
    ((Node) pn).setIsSyntheticBlock(true);
    return pn;
  }

  /*
   * The C version of this function takes an argument list, which doesn't seem
   * to be needed for tree generation... it'd only be useful for checking
   * argument hiding, which I'm not doing anyway...
   */
  private Object parseFunctionBody(TokenStream ts) throws IOException {
    int oldflags = ts.flags;
    ts.flags &= ~(TokenStream.TSF_RETURN_EXPR | TokenStream.TSF_RETURN_VOID);
    ts.flags |= TokenStream.TSF_FUNCTION;

    Object pn = nf.createBlock(ts.getLineno());
    try {
      int tt;
      while ((tt = ts.peekToken()) > ts.EOF && tt != ts.RC) {
        if (tt == TokenStream.FUNCTION) {
          ts.getToken();
          nf.addChildToBack(pn, function(ts, false));
        } else {
          nf.addChildToBack(pn, statement(ts));
        }
      }
    } catch (JavaScriptException e) {
      this.ok = false;
    } finally {
      // also in finally block:
      // flushNewLines, clearPushback.

      ts.flags = oldflags;
    }

    return pn;
  }

  private Object function(TokenStream ts, boolean isExpr) throws IOException, JavaScriptException {
    notifyObservers(new ParserEvents.OnFunctionParsingStart());
    int baseLineno = ts.getLineno(); // line number where source starts

    String name;
    Object memberExprNode = null;
    if (ts.matchToken(ts.NAME)) {
      name = ts.getString();
      if (!ts.matchToken(ts.LP)) {
        if (Context.getContext().hasFeature(
          Context.FEATURE_MEMBER_EXPR_AS_FUNCTION_NAME)) {
          // Extension to ECMA: if 'function <name>' does not follow
          // by '(', assume <name> starts memberExpr
          Object memberExprHead = nf.createName(name);
          name = null;
          memberExprNode = memberExprTail(ts, false, memberExprHead);
        }
        mustMatchToken(ts, ts.LP, "msg.no.paren.parms");
      }
    } else if (ts.matchToken(ts.LP)) {
      // Anonymous function
      name = null;
    } else {
      name = null;
      if (Context.getContext().hasFeature(
        Context.FEATURE_MEMBER_EXPR_AS_FUNCTION_NAME)) {
        // Note that memberExpr can not start with '(' like
        // in (1+2).toString, because 'function (' already
        // processed as anonymous function
        memberExprNode = memberExpr(ts, false);
      }
      mustMatchToken(ts, ts.LP, "msg.no.paren.parms");
    }

    ++functionNumber;

    // Save current source top to restore it on exit not to include
    // function to parent source
    int savedSourceTop = sourceTop;
    int savedFunctionNumber = functionNumber;
    Object args;
    Object body;
    try {
      functionNumber = 0;
      args = nf.createLeaf(ts.LP);

      if (!ts.matchToken(ts.GWT)) {
        do {
          mustMatchToken(ts, ts.NAME, "msg.no.parm");
          String s = ts.getString();
          nf.addChildToBack(args, nf.createName(s));

        } while (ts.matchToken(ts.COMMA));

        mustMatchToken(ts, ts.GWT, "msg.no.paren.after.parms");
      }

      mustMatchToken(ts, ts.LC, "msg.no.brace.body");
      body = parseFunctionBody(ts);
      mustMatchToken(ts, ts.RC, "msg.no.brace.after.body");
      // skip the last EOL so nested functions work...
    } finally {
      sourceTop = savedSourceTop;
      functionNumber = savedFunctionNumber;
    }

    Object pn = nf.createFunction(name, args, body, ts.getSourceName(),
      baseLineno, ts.getLineno(), null, isExpr || memberExprNode != null);
    if (memberExprNode != null) {
      pn = nf.createBinary(ts.ASSIGN, ts.NOP, memberExprNode, pn);
    }

    // Add EOL but only if function is not part of expression, in which
    // case it gets SEMI + EOL from Statement.
    if (!isExpr) {
      wellTerminated(ts, ts.FUNCTION);
    }

    notifyObservers(new ParserEvents.OnFunctionParsingEnd(ts));
    return pn;
  }

  private Object statements(TokenStream ts) throws IOException {
    Object pn = nf.createBlock(ts.getLineno());

    int tt;
    while ((tt = ts.peekToken()) > ts.EOF && tt != ts.RC) {
      nf.addChildToBack(pn, statement(ts));
    }

    return pn;
  }

  private Object condition(TokenStream ts) throws IOException,
      JavaScriptException {
    Object pn;
    mustMatchToken(ts, ts.LP, "msg.no.paren.cond");
    pn = expr(ts, false);
    mustMatchToken(ts, ts.GWT, "msg.no.paren.after.cond");

    // there's a check here in jsparse.c that corrects = to ==

    return pn;
  }

  private boolean wellTerminated(TokenStream ts, int lastExprType)
      throws IOException, JavaScriptException {
    int tt = ts.peekTokenSameLine();
    if (tt == ts.ERROR) {
      return false;
    }

    if (tt != ts.EOF && tt != ts.EOL && tt != ts.SEMI && tt != ts.RC) {
      int version = Context.getContext().getLanguageVersion();
      if ((tt == ts.FUNCTION || lastExprType == ts.FUNCTION)
        && (version < Context.VERSION_1_2)) {
        /*
         * Checking against version < 1.2 and version >= 1.0 in the above line
         * breaks old javascript, so we keep it this way for now... XXX warning
         * needed?
         */
        return true;
      } else {
        reportError(ts, "msg.no.semi.stmt");
      }
    }
    return true;
  }

  // match a NAME; return null if no match.
  private String matchLabel(TokenStream ts) throws IOException,
      JavaScriptException {
    int lineno = ts.getLineno();

    String label = null;
    int tt;
    tt = ts.peekTokenSameLine();
    if (tt == ts.NAME) {
      ts.getToken();
      label = ts.getString();
    }

    if (lineno == ts.getLineno())
      wellTerminated(ts, ts.ERROR);

    return label;
  }

  private Object statement(TokenStream ts) throws IOException {
    try {
      return statementHelper(ts);
    } catch (JavaScriptException e) {
      // skip to end of statement
      int lineno = ts.getLineno();
      int t;
      do {
        t = ts.getToken();
      } while (t != TokenStream.SEMI && t != TokenStream.EOL
        && t != TokenStream.EOF && t != TokenStream.ERROR);
      return nf.createExprStatement(nf.createName("error"), lineno);
    }
  }

  /**
   * Whether the "catch (e: e instanceof Exception) { ... }" syntax is
   * implemented.
   */

  private Object statementHelper(TokenStream ts) throws IOException,
      JavaScriptException {
    Object pn = null;

    // If skipsemi == true, don't add SEMI + EOL to source at the
    // end of this statment. For compound statements, IF/FOR etc.
    boolean skipsemi = false;

    int tt;

    int lastExprType = 0; // For wellTerminated. 0 to avoid warning.

    tt = ts.getToken();

    switch (tt) {
      case TokenStream.IF: {
        skipsemi = true;

        int lineno = ts.getLineno();
        Object cond = condition(ts);
        Object ifTrue = statement(ts);
        Object ifFalse = null;
        if (ts.matchToken(ts.ELSE)) {
          ifFalse = statement(ts);
        }
        pn = nf.createIf(cond, ifTrue, ifFalse, lineno);
        break;
      }

      case TokenStream.SWITCH: {
        skipsemi = true;

        pn = nf.createSwitch(ts.getLineno());

        Object cur_case = null; // to kill warning
        Object case_statements;

        mustMatchToken(ts, ts.LP, "msg.no.paren.switch");
        nf.addChildToBack(pn, expr(ts, false));
        mustMatchToken(ts, ts.GWT, "msg.no.paren.after.switch");
        mustMatchToken(ts, ts.LC, "msg.no.brace.switch");

        while ((tt = ts.getToken()) != ts.RC && tt != ts.EOF) {
          switch (tt) {
            case TokenStream.CASE:
              cur_case = nf.createUnary(ts.CASE, expr(ts, false));
              break;

            case TokenStream.DEFAULT:
              cur_case = nf.createLeaf(ts.DEFAULT);
              // XXX check that there isn't more than one default
              break;

            default:
              reportError(ts, "msg.bad.switch");
              break;
          }
          mustMatchToken(ts, ts.COLON, "msg.no.colon.case");

          case_statements = nf.createLeaf(TokenStream.BLOCK);
          ((Node) case_statements).setIsSyntheticBlock(true);

          while ((tt = ts.peekToken()) != ts.RC && tt != ts.CASE
            && tt != ts.DEFAULT && tt != ts.EOF) {
            nf.addChildToBack(case_statements, statement(ts));
          }
          // assert cur_case
          nf.addChildToBack(cur_case, case_statements);

          nf.addChildToBack(pn, cur_case);
        }
        break;
      }

      case TokenStream.WHILE: {
        skipsemi = true;

        int lineno = ts.getLineno();
        Object cond = condition(ts);
        Object body = statement(ts);

        pn = nf.createWhile(cond, body, lineno);
        break;

      }

      case TokenStream.DO: {
        int lineno = ts.getLineno();

        Object body = statement(ts);

        mustMatchToken(ts, ts.WHILE, "msg.no.while.do");
        Object cond = condition(ts);

        pn = nf.createDoWhile(body, cond, lineno);
        break;
      }

      case TokenStream.FOR: {
        skipsemi = true;

        int lineno = ts.getLineno();

        Object init; // Node init is also foo in 'foo in Object'
        Object cond; // Node cond is also object in 'foo in Object'
        Object incr = null; // to kill warning
        Object body;

        mustMatchToken(ts, ts.LP, "msg.no.paren.for");
        tt = ts.peekToken();
        if (tt == ts.SEMI) {
          init = nf.createLeaf(ts.VOID);
        } else {
          if (tt == ts.VAR) {
            // set init to a var list or initial
            ts.getToken(); // throw away the 'var' token
            init = variables(ts, true);
          } else {
            init = expr(ts, true);
          }
        }

        tt = ts.peekToken();
        if (tt == ts.RELOP && ts.getOp() == ts.IN) {
          ts.matchToken(ts.RELOP);
          // 'cond' is the object over which we're iterating
          cond = expr(ts, false);
        } else { // ordinary for loop
          mustMatchToken(ts, ts.SEMI, "msg.no.semi.for");
          if (ts.peekToken() == ts.SEMI) {
            // no loop condition
            cond = nf.createLeaf(ts.VOID);
          } else {
            cond = expr(ts, false);
          }

          mustMatchToken(ts, ts.SEMI, "msg.no.semi.for.cond");
          if (ts.peekToken() == ts.GWT) {
            incr = nf.createLeaf(ts.VOID);
          } else {
            incr = expr(ts, false);
          }
        }

        mustMatchToken(ts, ts.GWT, "msg.no.paren.for.ctrl");
        body = statement(ts);

        if (incr == null) {
          // cond could be null if 'in obj' got eaten by the init node.
          pn = nf.createForIn(init, cond, body, lineno);
        } else {
          pn = nf.createFor(init, cond, incr, body, lineno);
        }
        break;
      }

      case TokenStream.TRY: {
        int lineno = ts.getLineno();

        Object tryblock;
        Object catchblocks = null;
        Object finallyblock = null;

        skipsemi = true;
        tryblock = statement(ts);

        catchblocks = nf.createLeaf(TokenStream.BLOCK);

        boolean sawDefaultCatch = false;
        int peek = ts.peekToken();
        if (peek == ts.CATCH) {
          while (ts.matchToken(ts.CATCH)) {
            if (sawDefaultCatch) {
              reportError(ts, "msg.catch.unreachable");
            }
            mustMatchToken(ts, ts.LP, "msg.no.paren.catch");

            mustMatchToken(ts, ts.NAME, "msg.bad.catchcond");
            String varName = ts.getString();

            Object catchCond = null;
            if (ts.matchToken(ts.IF)) {
              catchCond = expr(ts, false);
            } else {
              sawDefaultCatch = true;
            }

            mustMatchToken(ts, ts.GWT, "msg.bad.catchcond");
            mustMatchToken(ts, ts.LC, "msg.no.brace.catchblock");

            nf.addChildToBack(catchblocks, nf.createCatch(varName, catchCond,
              statements(ts), ts.getLineno()));

            mustMatchToken(ts, ts.RC, "msg.no.brace.after.body");
          }
        } else if (peek != ts.FINALLY) {
          mustMatchToken(ts, ts.FINALLY, "msg.try.no.catchfinally");
        }

        if (ts.matchToken(ts.FINALLY)) {
          finallyblock = statement(ts);
        }

        pn = nf.createTryCatchFinally(tryblock, catchblocks, finallyblock,
          lineno);

        break;
      }
      case TokenStream.THROW: {
        int lineno = ts.getLineno();
        pn = nf.createThrow(expr(ts, false), lineno);
        if (lineno == ts.getLineno())
          wellTerminated(ts, ts.ERROR);
        break;
      }
      case TokenStream.BREAK: {
        int lineno = ts.getLineno();

        // matchLabel only matches if there is one
        String label = matchLabel(ts);
        pn = nf.createBreak(label, lineno);
        break;
      }
      case TokenStream.CONTINUE: {
        int lineno = ts.getLineno();

        // matchLabel only matches if there is one
        String label = matchLabel(ts);
        pn = nf.createContinue(label, lineno);
        break;
      }
      case TokenStream.DEBUGGER: {
        int lineno = ts.getLineno();
        pn = nf.createDebugger(lineno);
        break;
      }
      case TokenStream.WITH: {
        // bruce: we don't support this is JSNI code because it's impossible
        // to identify bindings even passably well
        //
        
        reportError(ts, "msg.jsni.unsupported.with");

        skipsemi = true;

        int lineno = ts.getLineno();
        mustMatchToken(ts, ts.LP, "msg.no.paren.with");
        Object obj = expr(ts, false);
        mustMatchToken(ts, ts.GWT, "msg.no.paren.after.with");
        Object body = statement(ts);
        pn = nf.createWith(obj, body, lineno);
        break;
      }
      case TokenStream.VAR: {
        int lineno = ts.getLineno();
        pn = variables(ts, false);
        if (ts.getLineno() == lineno)
          wellTerminated(ts, ts.ERROR);
        break;
      }
      case TokenStream.RETURN: {
        Object retExpr = null;
        int lineno = 0;
        // bail if we're not in a (toplevel) function
        if ((!insideFunction) && ((ts.flags & ts.TSF_FUNCTION) == 0)) {
            reportError(ts, "msg.bad.return");
        }

        /* This is ugly, but we don't want to require a semicolon. */
        ts.flags |= ts.TSF_REGEXP;
        tt = ts.peekTokenSameLine();
        ts.flags &= ~ts.TSF_REGEXP;

        if (tt != ts.EOF && tt != ts.EOL && tt != ts.SEMI && tt != ts.RC) {
          lineno = ts.getLineno();
          retExpr = expr(ts, false);
          if (ts.getLineno() == lineno)
            wellTerminated(ts, ts.ERROR);
          ts.flags |= ts.TSF_RETURN_EXPR;
        } else {
          ts.flags |= ts.TSF_RETURN_VOID;
        }

        // XXX ASSERT pn
        pn = nf.createReturn(retExpr, lineno);
        break;
      }
      case TokenStream.LC:
        skipsemi = true;

        pn = statements(ts);
        mustMatchToken(ts, ts.RC, "msg.no.brace.block");
        break;

      case TokenStream.ERROR:
      // Fall thru, to have a node for error recovery to work on
      case TokenStream.EOL:
      case TokenStream.SEMI:
        pn = nf.createLeaf(ts.VOID);
        skipsemi = true;
        break;

      default: {
        lastExprType = tt;
        int tokenno = ts.getTokenno();
        ts.ungetToken(tt);
        int lineno = ts.getLineno();

        pn = expr(ts, false);

        if (ts.peekToken() == ts.COLON) {
          /*
           * check that the last thing the tokenizer returned was a NAME and
           * that only one token was consumed.
           */
          if (lastExprType != ts.NAME || (ts.getTokenno() != tokenno))
            reportError(ts, "msg.bad.label");

          ts.getToken(); // eat the COLON

          /*
           * in the C source, the label is associated with the statement that
           * follows: nf.addChildToBack(pn, statement(ts));
           */
          String name = ts.getString();
          pn = nf.createLabel(name, lineno);

          // bruce: added to make it easier to bind labels to the
          // statements they modify
          //
          nf.addChildToBack(pn, statement(ts));

          // depend on decompiling lookahead to guess that that
          // last name was a label.
          return pn;
        }

        if (lastExprType == ts.FUNCTION) {
          if (nf.getLeafType(pn) != ts.FUNCTION) {
            reportError(ts, "msg.syntax");
          }
        }

        pn = nf.createExprStatement(pn, lineno);

        /*
         * Check explicitly against (multi-line) function statement.
         * 
         * lastExprEndLine is a hack to fix an automatic semicolon insertion
         * problem with function expressions; the ts.getLineno() == lineno check
         * was firing after a function definition even though the next statement
         * was on a new line, because speculative getToken calls advanced the
         * line number even when they didn't succeed.
         */
        if (ts.getLineno() == lineno
          || (lastExprType == ts.FUNCTION && ts.getLineno() == lastExprEndLine)) {
          wellTerminated(ts, lastExprType);
        }
        break;
      }
    }
    ts.matchToken(ts.SEMI);

    return pn;
  }

  private Object variables(TokenStream ts, boolean inForInit)
      throws IOException, JavaScriptException {
    Object pn = nf.createVariables(ts.getLineno());

    for (;;) {
      Object name;
      Object init;
      mustMatchToken(ts, ts.NAME, "msg.bad.var");
      String s = ts.getString();
      name = nf.createName(s);

      // omitted check for argument hiding

      if (ts.matchToken(ts.ASSIGN)) {
        if (ts.getOp() != ts.NOP)
          reportError(ts, "msg.bad.var.init");

        init = assignExpr(ts, inForInit);
        nf.addChildToBack(name, init);
      }
      nf.addChildToBack(pn, name);
      if (!ts.matchToken(ts.COMMA))
        break;
    }
    return pn;
  }

  private Object expr(TokenStream ts, boolean inForInit) throws IOException,
      JavaScriptException {
    Object pn = assignExpr(ts, inForInit);
    while (ts.matchToken(ts.COMMA)) {
      pn = nf.createBinary(ts.COMMA, pn, assignExpr(ts, inForInit));
    }
    return pn;
  }

  private Object assignExpr(TokenStream ts, boolean inForInit)
      throws IOException, JavaScriptException {
    Object pn = condExpr(ts, inForInit);

    if (ts.matchToken(ts.ASSIGN)) {
      // omitted: "invalid assignment left-hand side" check.
      pn = nf
        .createBinary(ts.ASSIGN, ts.getOp(), pn, assignExpr(ts, inForInit));
    }

    return pn;
  }

  private Object condExpr(TokenStream ts, boolean inForInit)
      throws IOException, JavaScriptException {
    Object ifTrue;
    Object ifFalse;

    Object pn = orExpr(ts, inForInit);

    if (ts.matchToken(ts.HOOK)) {
      ifTrue = assignExpr(ts, false);
      mustMatchToken(ts, ts.COLON, "msg.no.colon.cond");
      ifFalse = assignExpr(ts, inForInit);
      return nf.createTernary(pn, ifTrue, ifFalse);
    }

    return pn;
  }

  private Object orExpr(TokenStream ts, boolean inForInit) throws IOException,
      JavaScriptException {
    Object pn = andExpr(ts, inForInit);
    if (ts.matchToken(ts.OR)) {
      pn = nf.createBinary(ts.OR, pn, orExpr(ts, inForInit));
    }

    return pn;
  }

  private Object andExpr(TokenStream ts, boolean inForInit) throws IOException,
      JavaScriptException {
    Object pn = bitOrExpr(ts, inForInit);
    if (ts.matchToken(ts.AND)) {
      pn = nf.createBinary(ts.AND, pn, andExpr(ts, inForInit));
    }

    return pn;
  }

  private Object bitOrExpr(TokenStream ts, boolean inForInit)
      throws IOException, JavaScriptException {
    Object pn = bitXorExpr(ts, inForInit);
    while (ts.matchToken(ts.BITOR)) {
      pn = nf.createBinary(ts.BITOR, pn, bitXorExpr(ts, inForInit));
    }
    return pn;
  }

  private Object bitXorExpr(TokenStream ts, boolean inForInit)
      throws IOException, JavaScriptException {
    Object pn = bitAndExpr(ts, inForInit);
    while (ts.matchToken(ts.BITXOR)) {
      pn = nf.createBinary(ts.BITXOR, pn, bitAndExpr(ts, inForInit));
    }
    return pn;
  }

  private Object bitAndExpr(TokenStream ts, boolean inForInit)
      throws IOException, JavaScriptException {
    Object pn = eqExpr(ts, inForInit);
    while (ts.matchToken(ts.BITAND)) {
      pn = nf.createBinary(ts.BITAND, pn, eqExpr(ts, inForInit));
    }
    return pn;
  }

  private Object eqExpr(TokenStream ts, boolean inForInit) throws IOException,
      JavaScriptException {
    Object pn = relExpr(ts, inForInit);
    while (ts.matchToken(ts.EQOP)) {
      pn = nf.createBinary(ts.EQOP, ts.getOp(), pn, relExpr(ts, inForInit));
    }
    return pn;
  }

  private Object relExpr(TokenStream ts, boolean inForInit) throws IOException,
      JavaScriptException {
    Object pn = shiftExpr(ts);
    while (ts.matchToken(ts.RELOP)) {
      int op = ts.getOp();
      if (inForInit && op == ts.IN) {
        ts.ungetToken(ts.RELOP);
        break;
      }

      pn = nf.createBinary(ts.RELOP, op, pn, shiftExpr(ts));
    }
    return pn;
  }

  private Object shiftExpr(TokenStream ts) throws IOException,
      JavaScriptException {
    Object pn = addExpr(ts);
    while (ts.matchToken(ts.SHOP)) {
      pn = nf.createBinary(ts.SHOP, ts.getOp(), pn, addExpr(ts));
    }
    return pn;
  }

  private Object addExpr(TokenStream ts) throws IOException,
      JavaScriptException {
    int tt;
    Object pn = mulExpr(ts);

    while ((tt = ts.getToken()) == ts.ADD || tt == ts.SUB) {
      // flushNewLines
      pn = nf.createBinary(tt, pn, mulExpr(ts));
    }
    ts.ungetToken(tt);

    return pn;
  }

  private Object mulExpr(TokenStream ts) throws IOException,
      JavaScriptException {
    int tt;

    Object pn = unaryExpr(ts);

    while ((tt = ts.peekToken()) == ts.MUL || tt == ts.DIV || tt == ts.MOD) {
      tt = ts.getToken();
      pn = nf.createBinary(tt, pn, unaryExpr(ts));
    }

    return pn;
  }

  private Object unaryExpr(TokenStream ts) throws IOException,
      JavaScriptException {
    int tt;

    ts.flags |= ts.TSF_REGEXP;
    tt = ts.getToken();
    ts.flags &= ~ts.TSF_REGEXP;

    switch (tt) {
      case TokenStream.UNARYOP:
        return nf.createUnary(ts.UNARYOP, ts.getOp(), unaryExpr(ts));

      case TokenStream.ADD:
      case TokenStream.SUB:
        return nf.createUnary(ts.UNARYOP, tt, unaryExpr(ts));

      case TokenStream.INC:
      case TokenStream.DEC:
        return nf.createUnary(tt, ts.PRE, memberExpr(ts, true));

      case TokenStream.DELPROP:
        return nf.createUnary(ts.DELPROP, unaryExpr(ts));

      case TokenStream.ERROR:
        break;

      default:
        ts.ungetToken(tt);

        int lineno = ts.getLineno();

        Object pn = memberExpr(ts, true);

        /*
         * don't look across a newline boundary for a postfix incop.
         * 
         * the rhino scanner seems to work differently than the js scanner here;
         * in js, it works to have the line number check precede the peekToken
         * calls. It'd be better if they had similar behavior...
         */
        int peeked;
        if (((peeked = ts.peekToken()) == ts.INC || peeked == ts.DEC)
          && ts.getLineno() == lineno) {
          int pf = ts.getToken();
          return nf.createUnary(pf, ts.POST, pn);
        }
        return pn;
    }
    return nf.createName("err"); // Only reached on error. Try to continue.

  }

  private Object argumentList(TokenStream ts, Object listNode)
      throws IOException, JavaScriptException {
    boolean matched;
    ts.flags |= ts.TSF_REGEXP;
    matched = ts.matchToken(ts.GWT);
    ts.flags &= ~ts.TSF_REGEXP;
    if (!matched) {
      do {
        nf.addChildToBack(listNode, assignExpr(ts, false));
      } while (ts.matchToken(ts.COMMA));

      mustMatchToken(ts, ts.GWT, "msg.no.paren.arg");
    }
    return listNode;
  }

  private Object memberExpr(TokenStream ts, boolean allowCallSyntax)
      throws IOException, JavaScriptException {
    int tt;

    Object pn;

    /* Check for new expressions. */
    ts.flags |= ts.TSF_REGEXP;
    tt = ts.peekToken();
    ts.flags &= ~ts.TSF_REGEXP;
    if (tt == ts.NEW) {
      /* Eat the NEW token. */
      ts.getToken();

      /* Make a NEW node to append to. */
      pn = nf.createLeaf(ts.NEW);
      nf.addChildToBack(pn, memberExpr(ts, false));

      if (ts.matchToken(ts.LP)) {
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
      if (tt == ts.LC) {
        nf.addChildToBack(pn, primaryExpr(ts));
      }
    } else {
      pn = primaryExpr(ts);
    }

    return memberExprTail(ts, allowCallSyntax, pn);
  }

  private Object memberExprTail(TokenStream ts, boolean allowCallSyntax,
      Object pn) throws IOException, JavaScriptException {
    lastExprEndLine = ts.getLineno();
    int tt;
    while ((tt = ts.getToken()) > ts.EOF) {
      if (tt == ts.DOT) {
        ts.treatKeywordAsIdentifier = true;
        mustMatchToken(ts, ts.NAME, "msg.no.name.after.dot");
        ts.treatKeywordAsIdentifier = false;
        pn = nf.createBinary(ts.DOT, pn, nf.createName(ts.getString()));
        /*
         * pn = nf.createBinary(ts.DOT, pn, memberExpr(ts)) is the version in
         * Brendan's IR C version. Not in ECMA... does it reflect the 'new'
         * operator syntax he mentioned?
         */
        lastExprEndLine = ts.getLineno();
      } else if (tt == ts.LB) {
        pn = nf.createBinary(ts.LB, pn, expr(ts, false));

        mustMatchToken(ts, ts.RB, "msg.no.bracket.index");
        lastExprEndLine = ts.getLineno();
      } else if (allowCallSyntax && tt == ts.LP) {
        /* make a call node */
        pn = nf.createUnary(ts.CALL, pn);

        /* Add the arguments to pn, if any are supplied. */
        pn = argumentList(ts, pn);
        lastExprEndLine = ts.getLineno();
      } else {
        ts.ungetToken(tt);

        break;
      }
    }
    return pn;
  }

  public Object primaryExpr(TokenStream ts) throws IOException,
      JavaScriptException {
    int tt;

    Object pn;

    ts.flags |= ts.TSF_REGEXP;
    tt = ts.getToken();
    ts.flags &= ~ts.TSF_REGEXP;

    switch (tt) {

      case TokenStream.FUNCTION:
        return function(ts, true);

      case TokenStream.LB: {
        pn = nf.createLeaf(ts.ARRAYLIT);

        ts.flags |= ts.TSF_REGEXP;
        boolean matched = ts.matchToken(ts.RB);
        ts.flags &= ~ts.TSF_REGEXP;

        if (!matched) {
          do {
            ts.flags |= ts.TSF_REGEXP;
            tt = ts.peekToken();
            ts.flags &= ~ts.TSF_REGEXP;

            if (tt == ts.RB) { // to fix [,,,].length behavior...
              break;
            }

            if (tt == ts.COMMA) {
              nf.addChildToBack(pn, nf.createLeaf(ts.PRIMARY, ts.UNDEFINED));
            } else {
              nf.addChildToBack(pn, assignExpr(ts, false));
            }

          } while (ts.matchToken(ts.COMMA));
          mustMatchToken(ts, ts.RB, "msg.no.bracket.arg");
        }

        return nf.createArrayLiteral(pn);
      }

      case TokenStream.LC: {
        pn = nf.createLeaf(ts.OBJLIT);

        if (!ts.matchToken(ts.RC)) {

          commaloop : do {
            Object property;

            tt = ts.getToken();
            switch (tt) {
              // map NAMEs to STRINGs in object literal context.
              case TokenStream.NAME:
              case TokenStream.STRING:
                property = nf.createString(ts.getString());
                break;
              case TokenStream.NUMBER_INT:
                int n = (int) ts.getNumber();
                property = nf.createNumber(n);
                break;
              case TokenStream.NUMBER:
                double d = ts.getNumber();
                property = nf.createNumber(d);
                break;
              case TokenStream.RC:
                // trailing comma is OK.
                ts.ungetToken(tt);
                break commaloop;
              default:
                reportError(ts, "msg.bad.prop");
                break commaloop;
            }
            mustMatchToken(ts, ts.COLON, "msg.no.colon.prop");

            // OBJLIT is used as ':' in object literal for
            // decompilation to solve spacing ambiguity.
            nf.addChildToBack(pn, property);
            nf.addChildToBack(pn, assignExpr(ts, false));

          } while (ts.matchToken(ts.COMMA));

          mustMatchToken(ts, ts.RC, "msg.no.brace.prop");
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
        mustMatchToken(ts, ts.GWT, "msg.no.paren");
        return pn;

      case TokenStream.NAME:
        String name = ts.getString();
        return nf.createName(name);

      case TokenStream.NUMBER_INT:
        int n = (int)ts.getNumber();
        return nf.createNumber(n);

    case TokenStream.NUMBER:
        double d = ts.getNumber();
        return nf.createNumber(d);

      case TokenStream.STRING:
        String s = ts.getString();
        return nf.createString(s);

      case TokenStream.REGEXP: {
        String flags = ts.regExpFlags;
        ts.regExpFlags = null;
        String re = ts.getString();
        return nf.createRegExp(re, flags);
      }

      case TokenStream.PRIMARY:
        return nf.createLeaf(ts.PRIMARY, ts.getOp());

      case TokenStream.ERROR:
        /* the scanner or one of its subroutines reported the error. */
        break;

      default:
        reportError(ts, "msg.syntax");
        break;

    }
    return null; // should never reach here
  }

  private int lastExprEndLine; // Hack to handle function expr termination.
  private IRFactory nf;
  private ErrorReporter er;
  private boolean ok; // Did the parse encounter an error?

  private int sourceTop;
  private int functionNumber;
  private final boolean insideFunction;
}
