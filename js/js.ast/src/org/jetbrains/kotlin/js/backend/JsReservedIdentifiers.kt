// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.Collections;
import java.util.Set;

/**
 * Determines whether or not a particular string is a JavaScript keyword or not.
 */
public class JsReservedIdentifiers {
    public static final Set<String> reservedGlobalSymbols;

    static {
        String[] commonBuiltins = new String[] {
                // 15.1.1 Value Properties of the Global Object
                "NaN", "Infinity", "undefined",

                // 15.1.2 Function Properties of the Global Object
                "eval", "parseInt", "parseFloat", "isNan", "isFinite",

                // 15.1.3 URI Handling Function Properties
                "decodeURI", "decodeURIComponent",
                "encodeURI",
                "encodeURIComponent",

                // 15.1.4 Constructor Properties of the Global Object
                "Object", "Function", "Array", "String", "Boolean", "Number", "Date",
                "RegExp", "Error", "EvalError", "RangeError", "ReferenceError",
                "SyntaxError", "TypeError", "URIError",

                // 15.1.5 Other Properties of the Global Object
                "Math",

                // 10.1.6 Activation Object
                "arguments",

                // B.2 Additional Properties (non-normative)
                "escape", "unescape",

                // Window props (https://developer.mozilla.org/en/DOM/window)
                "applicationCache", "closed", "Components", "content", "controllers",
                "crypto", "defaultStatus", "dialogArguments", "directories",
                "document", "frameElement", "frames", "fullScreen", "globalStorage",
                "history", "innerHeight", "innerWidth", "length",
                "location", "locationbar", "localStorage", "menubar",
                "mozInnerScreenX", "mozInnerScreenY", "mozScreenPixelsPerCssPixel",
                "name", "navigator", "opener", "outerHeight", "outerWidth",
                "pageXOffset", "pageYOffset", "parent", "personalbar", "pkcs11",
                "returnValue", "screen", "scrollbars", "scrollMaxX", "scrollMaxY",
                "self", "sessionStorage", "sidebar", "status", "statusbar", "toolbar",
                "top", "window",

                // Window methods (https://developer.mozilla.org/en/DOM/window)
                "alert", "addEventListener", "atob", "back", "blur", "btoa",
                "captureEvents", "clearInterval", "clearTimeout", "close", "confirm",
                "disableExternalCapture", "dispatchEvent", "dump",
                "enableExternalCapture", "escape", "find", "focus", "forward",
                "GeckoActiveXObject", "getAttention", "getAttentionWithCycleCount",
                "getComputedStyle", "getSelection", "home", "maximize", "minimize",
                "moveBy", "moveTo", "open", "openDialog", "postMessage", "print",
                "prompt", "QueryInterface", "releaseEvents", "removeEventListener",
                "resizeBy", "resizeTo", "restore", "routeEvent", "scroll", "scrollBy",
                "scrollByLines", "scrollByPages", "scrollTo", "setInterval",
                "setResizeable", "setTimeout", "showModalDialog", "sizeToContent",
                "stop", "uuescape", "updateCommands", "XPCNativeWrapper",
                "XPCSafeJSOjbectWrapper",

                // Mozilla Window event handlers, same cite
                "onabort", "onbeforeunload", "onchange", "onclick", "onclose",
                "oncontextmenu", "ondragdrop", "onerror", "onfocus", "onhashchange",
                "onkeydown", "onkeypress", "onkeyup", "onload", "onmousedown",
                "onmousemove", "onmouseout", "onmouseover", "onmouseup",
                "onmozorientation", "onpaint", "onreset", "onresize", "onscroll",
                "onselect", "onsubmit", "onunload",

                // Safari Web Content Guide
                // http://developer.apple.com/library/safari/#documentation/AppleApplications/Reference/SafariWebContent/SafariWebContent.pdf
                // WebKit Window member data, from WebKit DOM Reference
                // (http://developer.apple.com/safari/library/documentation/AppleApplications/Reference/WebKitDOMRef/DOMWindow_idl/Classes/DOMWindow/index.html)
                // TODO(fredsa) Many, many more functions and member data to add
                "ontouchcancel", "ontouchend", "ontouchmove", "ontouchstart",
                "ongesturestart", "ongesturechange", "ongestureend",

                // extra window methods
                "uneval",

                // keywords https://developer.mozilla.org/en/New_in_JavaScript_1.7,
                // https://developer.mozilla.org/en/New_in_JavaScript_1.8.1
                "getPrototypeOf", "let", "yield",

                // "future reserved words"
                "abstract", "int", "short", "boolean", "interface", "static", "byte",
                "long", "char", "final", "native", "synchronized", "float", "package",
                "throws", "goto", "private", "transient", "implements", "protected",
                "volatile", "double", "public",

                // IE methods
                // (http://msdn.microsoft.com/en-us/library/ms535873(VS.85).aspx#)
                "attachEvent", "clientInformation", "clipboardData", "createPopup",
                "dialogHeight", "dialogLeft", "dialogTop", "dialogWidth",
                "onafterprint", "onbeforedeactivate", "onbeforeprint",
                "oncontrolselect", "ondeactivate", "onhelp", "onresizeend",

                // Common browser-defined identifiers not defined in ECMAScript
                "event", "external", "Debug", "Enumerator", "Global", "Image",
                "ActiveXObject", "VBArray", "Components",

                // Functions commonly defined on Object
                "toString", "getClass", "constructor", "prototype", "valueOf",

                // Client-side JavaScript identifiers, which are needed for linkers
                // that don't ensure GWT's window != $wnd, document != $doc, etc.
                // Taken from the Rhino book, pg 715
                "Anchor", "Applet", "Attr", "Canvas", "CanvasGradient",
                "CanvasPattern", "CanvasRenderingContext2D", "CDATASection",
                "CharacterData", "Comment", "CSS2Properties", "CSSRule",
                "CSSStyleSheet", "Document", "DocumentFragment", "DocumentType",
                "DOMException", "DOMImplementation", "DOMParser", "Element", "Event",
                "ExternalInterface", "FlashPlayer", "Form", "Frame", "History",
                "HTMLCollection", "HTMLDocument", "HTMLElement", "IFrame", "Image",
                "Input", "JSObject", "KeyEvent", "Link", "Location", "MimeType",
                "MouseEvent", "Navigator", "Node", "NodeList", "Option", "Plugin",
                "ProcessingInstruction", "Range", "RangeException", "Screen", "Select",
                "Table", "TableCell", "TableRow", "TableSelection", "Text", "TextArea",
                "UIEvent", "Window", "XMLHttpRequest", "XMLSerializer",
                "XPathException", "XPathResult", "XSLTProcessor",

                // These keywords trigger the loading of the java-plugin. For the
                // next-generation plugin, this results in starting a new Java process.
                "java", "Packages", "netscape", "sun", "JavaObject", "JavaClass",
                "JavaArray", "JavaMember",

                // GWT-defined identifiers
                "$wnd", "$doc", "$entry", "$moduleName", "$moduleBase", "$gwt_version", "$sessionId",

                // Identifiers used by JsStackEmulator; later set to obfuscatable
                "$stack", "$stackDepth", "$location",
        };

        reservedGlobalSymbols = new ObjectOpenHashSet<>(commonBuiltins.length);
        Collections.addAll(reservedGlobalSymbols, commonBuiltins);
    }

    private JsReservedIdentifiers() {
    }
}
