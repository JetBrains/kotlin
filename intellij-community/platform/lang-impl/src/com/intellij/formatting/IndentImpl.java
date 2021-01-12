/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.formatting;

import org.jetbrains.annotations.NonNls;

public class IndentImpl extends Indent {
  private final boolean myIsAbsolute;
  private final boolean myRelativeToDirectParent;

  private final Type myType;
  private final int mySpaces;
  private final boolean myEnforceIndentToChildren;

  public IndentImpl(final Type type, boolean absolute, boolean relativeToDirectParent) {
    this(type, absolute, 0, relativeToDirectParent, false);
  }

  public IndentImpl(final Type type, boolean absolute, final int spaces, boolean relativeToDirectParent, boolean enforceIndentToChildren) {
    myType = type;
    myIsAbsolute = absolute;
    mySpaces = spaces;
    myRelativeToDirectParent = relativeToDirectParent;
    myEnforceIndentToChildren = enforceIndentToChildren;
  }

  @Override
  public Type getType() {
    return myType;
  }

  public int getSpaces() {
    return mySpaces;
  }

  /**
   * @return    {@code 'isAbsolute'} property value as defined during {@link IndentImpl} object construction
   */
  public boolean isAbsolute() {
    return myIsAbsolute;
  }

  /**
   * Allows to answer if current indent object is configured to anchor direct parent that lays on a different line.
   * <p/>
   * Feel free to check {@link Indent} class-level javadoc in order to get more information and examples about expected
   * usage of this property.
   *
   * @return      flag that indicates if this indent should anchor direct parent that lays on a different line
   */
  public boolean isRelativeToDirectParent() {
    return myRelativeToDirectParent;
  }

  /**
   * Allows to answer if current indent object is configured to enforce indent for sub-blocks of composite block that doesn't start
   * new line.
   * <p/>
   * Feel free to check {@link Indent} javadoc for the more detailed explanation of this property usage.
   * 
   * @return      {@code true} if current indent object is configured to enforce indent for sub-blocks of composite block
   *              that doesn't start new line; {@code false} otherwise
   */
  public boolean isEnforceIndentToChildren() {
    return myEnforceIndentToChildren;
  }

  @NonNls
  @Override
  public String toString() {
    if (myType == Type.SPACES) {
      return "<Indent: SPACES(" + mySpaces + ")>";
    }
    return "<Indent: " + myType + (myIsAbsolute ? ":ABSOLUTE " : "") 
           + (myRelativeToDirectParent ? " relative to direct parent " : "")
           + (myEnforceIndentToChildren ? " enforce indent to children" : "") + ">";
  }
}
