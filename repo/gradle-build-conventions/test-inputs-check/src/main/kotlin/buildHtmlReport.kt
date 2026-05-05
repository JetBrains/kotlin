/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.id
import kotlinx.html.meta
import kotlinx.html.script
import kotlinx.html.span
import kotlinx.html.stream.createHTML
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.unsafe
import java.nio.file.Path
import kotlin.io.path.relativeTo

fun buildHtmlReport(files: List<AccessedFile>, projectPath: Path): String {
    val pageSize = 1000
    val totalPages = (files.size + pageSize - 1) / pageSize

    val stackIndex = HashMap<String, Int>()
    val stacks = ArrayList<String>()
    val itemsJson = files.joinToString(separator = ",", prefix = "[", postfix = "]") { f ->
        val stack = f.formatStacktrace()
        val idx = stackIndex.getOrPut(stack) { stacks.add(stack); stacks.size - 1 }
        "{\"p\":${jsonString(f.path.relativeTo(projectPath).toString())},\"s\":$idx}"
    }
    val stacksJson = stacks.joinToString(separator = ",", prefix = "[", postfix = "]") { jsonString(it) }
    val dataJson = "{\"items\":$itemsJson,\"stacks\":$stacksJson}"

    return createHTML().html {
        attributes["lang"] = "en"
        head {
            meta(charset = "UTF-8")
            title("Undeclared inputs (${files.size})")
            style {
                unsafe {
                    +"""
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; margin: 20px; }
                        h1 { font-size: 18px; }
                        .hint { color: #555; font-size: 13px; margin-bottom: 12px; }
                        .hint code { background: #f6f8fa; padding: 1px 4px; border-radius: 3px; }
                        details { border: 1px solid #ddd; border-radius: 4px; margin: 4px 0; padding: 6px 10px; }
                        details > summary { cursor: pointer; font-family: 'SF Mono', Menlo, monospace; font-size: 13px; }
                        pre { background: #f6f8fa; padding: 10px; border-radius: 4px; overflow-x: auto; font-size: 12px; margin: 8px 0 0 0; white-space: pre-wrap; }
                        .stack-actions { margin-top: 8px; display: flex; gap: 8px; align-items: center; }
                        .stack-actions button { padding: 4px 10px; cursor: pointer; }
                        .stack-actions .feedback { color: #2a7a2a; font-size: 12px; }
                        .pager { margin: 12px 0; display: flex; gap: 8px; align-items: center; }
                        .pager button { padding: 4px 10px; cursor: pointer; }
                        .pager button:disabled { cursor: default; opacity: 0.5; }
                    """.trimIndent()
                }
            }
        }
        body {
            h1 { +"Undeclared inputs found (${files.size})" }
            div("pager") {
                button { id = "prev"; unsafe { +"&laquo; Prev" } }
                span {
                    +"Page "
                    span { id = "pageNum"; +"1" }
                    +" of $totalPages"
                }
                button { id = "next"; unsafe { +"Next &raquo;" } }
            }
            div { id = "items" }
            script {
                unsafe {
                    +"""
                        (function() {
                          var DATA = $dataJson;
                          var ITEMS = DATA.items;
                          var STACKS = DATA.stacks;
                          var PAGE_SIZE = $pageSize;
                          var totalPages = $totalPages;
                          var current = 0;
                          var container = document.getElementById('items');
                          var prev = document.getElementById('prev');
                          var next = document.getElementById('next');
                          var label = document.getElementById('pageNum');
                          function render() {
                            var frag = document.createDocumentFragment();
                            var start = current * PAGE_SIZE;
                            var end = Math.min(start + PAGE_SIZE, ITEMS.length);
                            for (var i = start; i < end; i++) {
                              var item = ITEMS[i];
                              var stackText = STACKS[item.s];
                              var d = document.createElement('details');
                              var s = document.createElement('summary');
                              s.textContent = item.p;
                              var p = document.createElement('pre');
                              p.textContent = stackText;
                              var actions = document.createElement('div');
                              actions.className = 'stack-actions';
                              var btn = document.createElement('button');
                              btn.type = 'button';
                              btn.textContent = 'Copy stacktrace';
                              var feedback = document.createElement('span');
                              feedback.className = 'feedback';
                              btn.addEventListener('click', (function(text, fb) {
                                return function() {
                                  navigator.clipboard.writeText(text).then(function() {
                                    fb.textContent = 'Copied — paste into IDEA \u2192 Analyze Stack Trace';
                                    setTimeout(function() { fb.textContent = ''; }, 4000);
                                  }, function() {
                                    fb.textContent = 'Copy failed';
                                  });
                                };
                              })(stackText, feedback));
                              actions.appendChild(btn);
                              actions.appendChild(feedback);
                              d.appendChild(s);
                              d.appendChild(actions);
                              d.appendChild(p);
                              frag.appendChild(d);
                            }
                            container.replaceChildren(frag);
                            label.textContent = current + 1;
                            prev.disabled = current === 0;
                            next.disabled = current >= totalPages - 1;
                            window.scrollTo(0, 0);
                          }
                          prev.addEventListener('click', function() { if (current > 0) { current--; render(); } });
                          next.addEventListener('click', function() { if (current < totalPages - 1) { current++; render(); } });
                          render();
                        })();
                    """.trimIndent()
                }
            }
        }
    }
}

private fun jsonString(s: String): String = buildString {
    append('"')
    for (c in s) {
        when (c) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            '<' -> append("\\u003c")
            '>' -> append("\\u003e")
            '&' -> append("\\u0026")
            '\u2028' -> append("\\u2028")
            '\u2029' -> append("\\u2029")
            else -> if (c.code < 0x20) append("\\u%04x".format(c.code)) else append(c)
        }
    }
    append('"')
}
