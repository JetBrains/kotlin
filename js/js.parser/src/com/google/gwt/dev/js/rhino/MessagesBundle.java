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

package com.google.gwt.dev.js.rhino;

import java.util.ListResourceBundle;

public class MessagesBundle extends ListResourceBundle {

    public Object [][] getContents() {
        return contents;
    }

    private static final Object [][] contents = {
        {"msg.no.paren.catch", "missing ( before catch-block condition"},
        {"msg.no.brace.prop", "missing } after property list"},
        {"msg.no.paren.after.cond", "missing ) after condition"},
        {"msg.no.paren.with", "missing ( before with-statement object"},
        {"msg.no.colon.case", "missing : after case expression"},
        {"msg.unterminated.re.lit", "unterminated regular expression literal"},
        {"msg.unterminated.string.lit", "unterminated string literal"},
        {"msg.bad.lhs.assign", "Invalid assignment left-hand side."},
        {"msg.no.paren.cond", "missing ( before condition"},
        {"msg.no.bracket.index", "missing ] in index expression"},
        {"msg.no.function.ref.found", "no source found to decompile function reference {0}"},
        {"msg.caught.nfe", "number format error: {0}"},
        {"msg.bad.switch", "invalid switch statement"},
        {"msg.no.paren.switch", "missing ( before switch expression"},
        {"msg.no.bracket.arg", "missing ] after element list"},
        {"msg.token.replaces.pushback", "ungot token {0} replaces pushback token {1}"},
        {"msg.no.paren.for.ctrl", "missing ) after for-loop control"},
        {"msg.bad.label", "invalid label"},
        {"msg.bad.octal.literal", "illegal octal literal digit {0}; interpreting it as a decimal digit"},
        {"msg.no.paren.for", "missing ( after for"},
        {"msg.syntax", "syntax error"},
        {"msg.no.colon.cond", "missing : in conditional expression"},
        {"msg.no.while.do", "missing while after do-loop body"},
        {"msg.no.brace.body", "missing '{' before function body"},
        {"msg.bad.prop", "invalid property id"},
        {"msg.try.no.catchfinally", "''try'' without ''catch'' or ''finally''"},
        {"msg.missing.exponent", "missing exponent"},
        {"msg.invalid.re.flag", "invalid flag after regular expression"},
        {"msg.no.brace.catchblock", "missing '{' before catch-block body"},
        {"msg.unterminated.comment", "unterminated comment"},
        {"msg.bad.catchcond", "invalid catch block condition"},
        {"msg.no.paren.after.parms", "missing ) after formal parameters"},
        {"msg.no.paren.parms", "missing ( before function parameters"},
        {"msg.no.colon.prop", "missing : after property id"},
        {"msg.no.paren.after.switch", "missing ) after switch expression"},
        {"msg.jsni.unsupported.with", "The ''with'' statement is unsupported in JSNI blocks (perhaps you could use a local variable instead?)"},
        {"msg.bad.var", "missing variable name"},
        {"msg.bad.var.init", "invalid variable initialization"},
        {"msg.bad.return", "invalid return"},
        {"msg.no.semi.for.cond", "missing ; after for-loop condition"},
        {"msg.no.semi.stmt", "missing ; before statement"},
        {"msg.no.paren.arg", "missing ) after argument list"},
        {"msg.jsni.expected.param.type", "Expected a valid parameter type signature in JSNI method reference"},
        {"msg.no.parm", "missing formal parameter"},
        {"msg.no.brace.after.body", "missing } after function body"},
        {"msg.no.paren", "missing ) in parenthetical"},
        {"msg.no.brace.block", "missing } in compound statement"},
        {"msg.no.semi.for", "missing ; after for-loop initializer"},
        {"msg.jsni.expected.char", "Expected \"{0}\" in JSNI reference"},
        {"msg.no.brace.switch", "missing '{' before switch body"},
        {"msg.invalid.escape", "invalid Unicode escape sequence"},
        {"msg.no.paren.after.with", "missing ) after with-statement object"},
        {"msg.illegal.character", "illegal character"},
        {"msg.catch.unreachable", "any catch clauses following an unqualified catch are unreachable"},
        {"msg.no.name.after.dot", "missing name after . operator"},
        {"msg.jsni.expected.identifier", "Expected an identifier in JSNI reference"},
    };
}