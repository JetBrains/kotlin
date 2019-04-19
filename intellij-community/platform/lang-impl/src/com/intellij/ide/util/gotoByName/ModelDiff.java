/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.ide.util.gotoByName;

import com.intellij.util.diff.Diff;
import com.intellij.util.diff.FilesTooBigForDiffException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModelDiff {
  @Nullable
  public static List<Cmd> createDiffCmds(@NotNull Model<Object> listModel, @NotNull Object[] oldElements, @NotNull Object[] newElements) {
    Diff.Change change = null;
    try {
      change = Diff.buildChanges(oldElements, newElements);
    }
    catch (FilesTooBigForDiffException e) {
      // should not occur
    }

    if (change == null) {
      return null;
    }

    List<Cmd> commands = new ArrayList<>();
    int inserted = 0;
    int deleted = 0;
    while (change != null) {
      if (change.deleted > 0) {
        final int start = change.line0 + inserted - deleted;
        commands.add(new RemoveCmd<>(listModel, start, start + change.deleted - 1));
      }

      if (change.inserted > 0) {
        List<Object> elements = new ArrayList<>(Arrays.asList(newElements).subList(change.line1, change.line1 + change.inserted));
        commands.add(new InsertCmd<>(listModel, change.line0 + inserted - deleted, elements));
      }

      deleted += change.deleted;
      inserted += change.inserted;
      change = change.link;
    }
    return commands;
  }

  public interface Cmd {
    void apply();
    int translateSelection(int row);
  }

  public interface Model<T> {
    void addToModel(int index, T element);

    default void addAllToModel(int index, List<T> elements) {
      for (int i = 0; i < elements.size(); i++) {
        addToModel(index + i, elements.get(i));
      }
    }

    void removeRangeFromModel(int start, int end);
  }

  private static class RemoveCmd<T> implements Cmd {
    private final Model<T> myListModel;
    private final int start;
    private final int end;

    private RemoveCmd(@NotNull Model<T> model, final int start, final int end) {
      myListModel = model;
      this.start = start;
      this.end = end;
    }

    @Override
    public void apply() {
      myListModel.removeRangeFromModel(start, end);
    }

    @Override
    public int translateSelection(int row) {
      if (row < start) return row;
      if (row >= end) return row - (end-start);
      return start-1;
    }

    @Override
    public String toString() {
      return "-["+start+", "+end+"]";
    }
  }

  private static class InsertCmd<T> implements Cmd {
    private final Model<T> myListModel;
    private final int idx;
    private final List<T> elements;

    private InsertCmd(@NotNull Model<T> model, final int idx, @NotNull List<T> elements) {
      myListModel = model;
      this.idx = idx;
      this.elements = elements;
    }

    @Override
    public void apply() {
      //System.out.println("Adding: "+this+"-> "+element);
      myListModel.addAllToModel(idx, elements);
    }

    @Override
    public int translateSelection(int row) {
      return idx > row ? row : row + elements.size();
    }
    @Override
    public String toString() {
      return "+["+idx+"]";
    }
  }
}
