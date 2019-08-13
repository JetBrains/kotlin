/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.formatting.engine;

public abstract class State {

  private boolean myDone;
  private Runnable myOnDoneAction;

  public void iteration() {
    if (!isDone()) {
      doIteration();
    }
  }

  public final boolean isDone() {
    return myDone;
  }

  protected void setDone(boolean done) {
    if (!myDone && done && myOnDoneAction != null) {
      myOnDoneAction.run();
    }
    myDone = done;
  }

  public void stop() {}

  protected abstract void doIteration();

  public void prepare() {}

  public void setOnDone(Runnable onDoneAction) {
    myOnDoneAction = onDoneAction;
  }
  
}