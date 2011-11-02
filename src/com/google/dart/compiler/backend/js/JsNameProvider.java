//// Copyright 2011, the Dart project authors. All rights reserved.
//// Redistribution and use in source and binary forms, with or without
//// modification, are permitted provided that the following conditions are
//// met:
////
////  * Redistributions of source code must retain the above copyright
////    notice, this list of conditions and the following disclaimer.
////  * Redistributions in binary form must reproduce the above
////    copyright notice, this list of conditions and the following
////    disclaimer in the documentation and/or other materials provided
////    with the distribution.
////  * Neither the name of Google Inc. nor the names of its
////    contributors may be used to endorse or promote products derived
////    from this software without specific prior written permission.
////
//// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
//// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
//// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
//// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
//// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
//// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
//// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
//// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
//// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
//// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
//// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
//package com.google.dart.compiler.backend.js;
//
//import com.google.dart.compiler.backend.js.ast.JsName;
//import com.google.dart.compiler.backend.js.ast.JsProgram;
//import com.google.dart.compiler.backend.js.ast.JsScope;
//import com.google.dart.compiler.resolver.ClassElement;
//import com.google.dart.compiler.resolver.ElementKind;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * A helper class for managing global names.
// * @author johnlenz@google.com (John Lenz)
// */
//class JsNameProvider {
//  private final DartMangler mangler;
//  private Map<Symbol, JsName> names = new HashMap<Symbol, JsName>();
//  private JsScope globalScope;
//
//  JsNameProvider(JsProgram program, DartMangler mangler) {
//    this.globalScope = program.getScope();
//    this.mangler = mangler;
//  }
//
//  /**
//   * Returns the JsName for the given element. If the element is global and
//   * hasn't been declared yet, it is done now.
//   */
//  JsName getName(Symbol symbol) {
//    JsName jsName = names.get(symbol);
//    if (jsName != null) {
//      assert !jsName.getShortIdent().equals("Object$Dart");
//      return jsName;
//    }
//    assert ElementKind.of(symbol).equals(ElementKind.CLASS)
//        : "Only classes can be lazily declared. Undeclared: "
//              + symbol.getOriginalSymbolName();
//    ClassElement classElement = (ClassElement) symbol;
//    String name = classElement.getName();
//    String nativeName = classElement.getNativeName();
//    if (nativeName == null) {
//      String mangledClassName = mangler.mangleClassName(classElement);
//      jsName = globalScope.declareName(mangledClassName, mangledClassName, name);
//    } else {
//      jsName = globalScope.declareName(nativeName);
//    }
//    // Class names are globally accessible.
//    jsName.setObfuscatable(false);
//    names.put(symbol, jsName);
//    assert !jsName.getShortIdent().equals("Object$Dart") : "unexpected " + ((ClassElement) symbol).getNode().getSource().getName();
//    return jsName;
//  }
//
//  void setName(Symbol symbol, JsName name) {
//    names.put(symbol, name);
//  }
//}
