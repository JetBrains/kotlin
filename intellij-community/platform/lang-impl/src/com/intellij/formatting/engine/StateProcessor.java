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

import com.intellij.util.containers.ContainerUtil;

import java.util.List;

public class StateProcessor {
  
  private final List<State> myStates = ContainerUtil.newArrayList();
  private State myCurrentState;
  
  public StateProcessor(State initial) {
    myCurrentState = initial;
  }
  
  public void setNextState(State state) {
    myStates.add(state);
  }
  
  public boolean isDone() {
    return myStates.isEmpty() && myCurrentState.isDone();
  }
  
  public void iteration() {
    if (!myCurrentState.isDone()) {
      myCurrentState.iteration();
    }
    shiftStateIfNecessary();
  }

  private void shiftStateIfNecessary() {
    if (myCurrentState.isDone() && !myStates.isEmpty()) {
      myCurrentState = myStates.get(0);
      myStates.remove(0);
      myCurrentState.prepare();
    }
  }

  public void stop() {
    myCurrentState.stop();
  }
}
