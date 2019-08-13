/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package com.intellij.psi.search.scope.packageSet;

import com.intellij.analysis.AnalysisScopeBundle;
import com.intellij.lexer.Lexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.search.scope.packageSet.lexer.ScopeTokenTypes;
import org.jetbrains.annotations.Nullable;

public class FilePackageSetParserExtension implements PackageSetParserExtension {

  @Override
  @Nullable
  public String parseScope(Lexer lexer) {
    if (lexer.getTokenType() != ScopeTokenTypes.IDENTIFIER) return null;
    String id = getTokenText(lexer);
    if (FilePatternPackageSet.SCOPE_FILE.equals(id)) {

      final CharSequence buf = lexer.getBufferSequence();
      final int end = lexer.getTokenEnd();
      final int bufferEnd = lexer.getBufferEnd();

      if (end >= bufferEnd || buf.charAt(end) != ':' && buf.charAt(end) != '[') {
        return null;
      }

      lexer.advance();
      return FilePatternPackageSet.SCOPE_FILE;
    }

    return null;
  }

  @Override
  @Nullable
  public PackageSet parsePackageSet(final Lexer lexer, final String scope, final String modulePattern) throws ParsingException {
    if (scope != FilePatternPackageSet.SCOPE_FILE) return null;
    return new FilePatternPackageSet(modulePattern, parseFilePattern(lexer));
  }

  private static String parseFilePattern(Lexer lexer) throws ParsingException {
    StringBuilder pattern = new StringBuilder();
    boolean wasIdentifier = false;
    while (true) {
      if (lexer.getTokenType() == ScopeTokenTypes.DIV) {
        wasIdentifier = false;
        pattern.append("/");
      }
      else if (lexer.getTokenType() == ScopeTokenTypes.IDENTIFIER || lexer.getTokenType() == ScopeTokenTypes.INTEGER_LITERAL) {
        if (wasIdentifier) error(lexer, AnalysisScopeBundle.message("error.package.set.token.expectations", getTokenText(lexer)));
        wasIdentifier = lexer.getTokenType() == ScopeTokenTypes.IDENTIFIER;
        pattern.append(getTokenText(lexer));
      }
      else if (lexer.getTokenType() == ScopeTokenTypes.ASTERISK) {
        wasIdentifier = false;
        pattern.append("*");
      }
      else if (lexer.getTokenType() == ScopeTokenTypes.DOT) {
        wasIdentifier = false;
        pattern.append(".");
      }
      else if (lexer.getTokenType() == TokenType.WHITE_SPACE) {
        wasIdentifier = false;
        pattern.append(" ");
      }
      else if (lexer.getTokenType() == ScopeTokenTypes.MINUS) {
        wasIdentifier = false;
        pattern.append("-");
      }
      else if (lexer.getTokenType() == ScopeTokenTypes.TILDE) {
        wasIdentifier = false;
        pattern.append("~");
      }
      else if (lexer.getTokenType() == ScopeTokenTypes.SHARP) {
        wasIdentifier = false;
        pattern.append("#");
      }
      else {
        break;
      }
      lexer.advance();
    }

    if (pattern.length() == 0) {
      error(lexer, AnalysisScopeBundle.message("error.package.set.pattern.expectations"));
    }

    return pattern.toString();
  }

  private static String getTokenText(Lexer lexer) {
    int start = lexer.getTokenStart();
    int end = lexer.getTokenEnd();
    return lexer.getBufferSequence().subSequence(start, end).toString();
  }

  private static void error(Lexer lexer, String message) throws ParsingException {
    throw new ParsingException(
      AnalysisScopeBundle.message("error.package.set.position.parsing.error", message, (lexer.getTokenStart() + 1)));
  }
}