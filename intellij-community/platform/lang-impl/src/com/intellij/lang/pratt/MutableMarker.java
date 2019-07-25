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
package com.intellij.lang.pratt;

import com.intellij.lang.LighterASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

/**
 * @author peter
 */
public class MutableMarker {
  enum Mode { READY, DROPPED, COMMITTED, ERROR }

  private final PsiBuilder.Marker myStartMarker;
  private IElementType myResultType;
  private final int myInitialPathLength;
  private final LinkedList<IElementType> myPath;
  private Mode myMode;

  public MutableMarker(final LinkedList<IElementType> path, final PsiBuilder.Marker startMarker, final int initialPathLength) {
    myPath = path;
    myStartMarker = startMarker;
    myInitialPathLength = initialPathLength;
    myMode = startMarker instanceof LighterASTNode && ((LighterASTNode)startMarker).getTokenType() != null? Mode.COMMITTED : Mode.READY;
  }

  // for easier transition only
  public MutableMarker(final LinkedList<IElementType> path, final PsiBuilder builder) {
    myPath = path;
    myStartMarker = (PsiBuilder.Marker)builder.getLatestDoneMarker();
    myInitialPathLength = path.size();
    myResultType = myStartMarker instanceof LighterASTNode? ((LighterASTNode)myStartMarker).getTokenType() : null;
    myMode = myResultType != null ? Mode.COMMITTED : Mode.READY;
  }

  public boolean isCommitted() {
    return myMode == Mode.COMMITTED;
  }

  public boolean isDropped() {
    return myMode == Mode.DROPPED;
  }

  public boolean isError() {
    return myMode == Mode.ERROR;
  }

  public boolean isReady() {
    return myMode == Mode.READY;
  }

  public MutableMarker setResultType(final IElementType resultType) {
    myResultType = resultType;
    return this;
  }

  public IElementType getResultType() {
    return myResultType;
  }

  public void finish() {
    if (myMode == Mode.READY) {
      if (myResultType == null) {
        myMode = Mode.DROPPED;
        myStartMarker.drop();
      }
      else {
        myMode = Mode.COMMITTED;
        myStartMarker.done(myResultType);
        restorePath();
        myPath.addLast(myResultType);
      }
    }
  }

  private void restorePath() {
    while (myPath.size() > myInitialPathLength) {
      myPath.removeLast();
    }
  }

  public MutableMarker precede() {
    return new MutableMarker(myPath, myStartMarker.precede(), myInitialPathLength);
  }

  public void finish(final IElementType type) {
    setResultType(type);
    finish();
  }

  public void drop() {
    assert myMode == Mode.READY : myMode;
    myMode = Mode.DROPPED;
    myStartMarker.drop();
  }

  public void rollback() {
    assert myMode == Mode.READY : myMode;
    myMode = Mode.DROPPED;
    restorePath();
    myStartMarker.rollbackTo();
  }

  public void error(@NotNull String message) {
    assert myMode == Mode.READY : myMode;
    myMode = Mode.ERROR;
    myStartMarker.error(message);
  }
}
