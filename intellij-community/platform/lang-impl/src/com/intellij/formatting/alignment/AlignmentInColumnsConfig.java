/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.formatting.alignment;

import com.intellij.psi.tree.TokenSet;

/**
 * Encapsulates information necessary for correct {@code 'align in columns'} processing.
 * <p/>
 * Thread-safe.
 * <p/>
 * <b>Note:</b> this class doesn't provide custom realization for {@link #hashCode()} and {@link #equals(Object)} at the moment.
 * Feel free to add them as necessary.
 *
 * @author Denis Zhdanov
 * @see AlignmentInColumnsHelper
 */
public class AlignmentInColumnsConfig {
  private final TokenSet myStopMultilineCheckElementTypes;
  private final TokenSet myTargetDeclarationTypes;
  private final TokenSet myWhiteSpaceTokenTypes;
  private final TokenSet myCommentTokenTypes;
  private final TokenSet myDistinguishableTypes;

  /**
   * Creates new {@code AlignmentInColumnsConfig} object that is used to tweak {@code 'align in columns'} processing.
   * <p/>
   * 'Alignment in columns' means formatting code as shown below:
   * <p/>
   * <pre>
   *     double start = 1;
   *     int    end   = 2;
   *     private int tmp = 3;
   *     private   int    tmp2;
   *     protected double tmp3;
   * </pre>
   *
   * @param stopMultilineCheckElementTypes {@code 'align in column'} algorithm performs number of checks in order to decide
   *                                       if two variable declarations should be aligned in columns. One of that checks is
   *                                       examination for sub-elements consistency. E.g. {@code 'int end = 2'} statement
   *                                       from example above is not aligned to {@code 'private int tmp = 3;'} because the
   *                                       former doesn't have modifier. Element types given here defines boundary for such
   *                                       a checks, e.g. we can define type of {@code '='} element to be stop check type
   *                                       for example above
   * @param whiteSpaceTokenTypes           defines types of the tokens that should be treated as a white space
   * @param commentTokenTypes              defines types of the tokens that should be treated as comments
   * @param distinguishableTypes           {@code 'align in column'} algorithm doesn't align elements, containing different sets
   *                                       of distinguishable elements.
   *                                       E.g. {@code 'private int tmp = 3'} is not aligned to {@code 'private int tmp2'}
   *                                       at example above, providing that distinguishable types contain {@code '='} token.
   * @param targetDeclarationTypes         defines variable declaration sub-element types to be aligned. Example above
   *                                       shows alignment for {@code 'modifier list'}, {@code 'type reference'},
   *                                       {@code 'identifier'} and {@code '='} types. The general idea of this property
   *                                       is to let avoid alignment of unnecessary types, e.g. variable definition expressions
   */
  public AlignmentInColumnsConfig(TokenSet stopMultilineCheckElementTypes,
                                  TokenSet whiteSpaceTokenTypes,
                                  TokenSet commentTokenTypes,
                                  TokenSet distinguishableTypes,
                                  TokenSet targetDeclarationTypes) {
    myStopMultilineCheckElementTypes = stopMultilineCheckElementTypes;
    myWhiteSpaceTokenTypes = whiteSpaceTokenTypes;
    myCommentTokenTypes = commentTokenTypes;
    myDistinguishableTypes = distinguishableTypes;
    myTargetDeclarationTypes = targetDeclarationTypes;
  }

  public TokenSet getStopMultilineCheckElementTypes() {
    return myStopMultilineCheckElementTypes;
  }

  public TokenSet getWhiteSpaceTokenTypes() {
    return myWhiteSpaceTokenTypes;
  }

  public TokenSet getCommentTokenTypes() {
    return myCommentTokenTypes;
  }

  public TokenSet getDistinguishableTypes() {
    return myDistinguishableTypes;
  }

  public TokenSet getTargetDeclarationTypes() {
    return myTargetDeclarationTypes;
  }
}
