/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package com.intellij.internal;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.ManagingFS;
import com.intellij.openapi.vfs.newvfs.impl.VirtualFileSystemEntry;
import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntProcedure;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ComputeVirtualFileNameStatAction extends AnAction implements DumbAware {
  public ComputeVirtualFileNameStatAction() {
    super("Compute VF Name Statistics");
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    long start = System.currentTimeMillis();

    suffixes.clear();
    nameCount.clear();
    VirtualFile[] roots = ManagingFS.getInstance().getRoots(LocalFileSystem.getInstance());
    for (VirtualFile root : roots) {
      compute(root);
    }

    final List<Pair<String,Integer>> names = new ArrayList<>(nameCount.size());
    nameCount.forEachEntry(new TObjectIntProcedure<String>() {
      @Override
      public boolean execute(String name, int count) {
        names.add(Pair.create(name, count));
        return true;
      }
    });
    Collections.sort(names, (o1, o2) -> o2.second - o1.second);

    System.out.println("Most frequent names ("+names.size()+" total):");
    int saveByIntern = 0;
    for (Pair<String, Integer> pair : names) {
      int count = pair.second;
      String name = pair.first;
      System.out.println(name + " -> " + count);
      saveByIntern += count * name.length();
      if (count == 1) break;
    }
    System.out.println("Total save if names were interned: "+saveByIntern+"; ------------");

    //System.out.println("Prefixes: ("+prefixes.size()+" total)");
    //show(prefixes);
    System.out.println("Suffix counts:("+suffixes.size()+" total)");
    show(suffixes);


    final TObjectIntHashMap<String> save = new TObjectIntHashMap<>();
    // compute economy
    suffixes.forEachEntry(new TObjectIntProcedure<String>() {
      @Override
      public boolean execute(String s, int count) {
        save.put(s, count * s.length());
        return true;
      }
    });

    System.out.println("Supposed save by stripping suffixes: ("+save.size()+" total)");
    final List<Pair<String, Integer>> saveSorted = show(save);


    final List<String> picked = new ArrayList<>();
    //List<String> candidates = new ArrayList<String>();
    //int i =0;
    //for (Pair<String, Integer> pair : sorted) {
    //  if (i++>1000) break;
    //  candidates.add(pair.first);
    //}

    //final TObjectIntHashMap<String> counts = new TObjectIntHashMap<String>();
    //suffixes.forEachEntry(new TObjectIntProcedure<String>() {
    //  @Override
    //  public boolean execute(String a, int b) {
    //    counts.put(a, b);
    //    return true;
    //  }
    //});

    while (picked.size() != 15) {
      Pair<String, Integer> cp = saveSorted.get(0);
      final String candidate = cp.first;
      picked.add(candidate);
      System.out.println("Candidate: '"+candidate+"', save = "+cp.second);
      Collections.sort(picked, (o1, o2) -> {
        return o2.length() - o1.length(); // longer first
      });
      saveSorted.clear();

      // adjust
      suffixes.forEachEntry(new TObjectIntProcedure<String>() {
        @Override
        public boolean execute(String s, int count) {
          for (int i = picked.size() - 1; i >= 0; i--) {
            String pick = picked.get(i);
            if (pick.endsWith(s)) {
              count -= suffixes.get(pick);
              break;
            }
          }
          saveSorted.add(Pair.create(s, s.length() * count));
          return true;
        }
      });
      Collections.sort(saveSorted, (o1, o2) -> o2.second.compareTo(o1.second));
    }

    System.out.println("Picked: "+ StringUtil.join(picked, s -> "\"" + s + "\"", ","));
    Collections.sort(picked, (o1, o2) -> {
      return o2.length() - o1.length(); // longer first
    });

    int saved = 0;
    for (int i = 0; i < picked.size(); i++) {
      String s = picked.get(i);
      int count = suffixes.get(s);
      for (int k=0; k<i;k++) {
        String prev = picked.get(k);
        if (prev.endsWith(s)) {
          count -= suffixes.get(prev);
          break;
        }
      }
      saved += count * s.length();
    }
    System.out.println("total saved = " + saved);
    System.out.println("Time spent: " + (System.currentTimeMillis() - start));
  }

  private static List<Pair<String,Integer>> show(final TObjectIntHashMap<String> prefixes) {
    final List<Pair<String,Integer>> prefs = new ArrayList<>(prefixes.size());
    prefixes.forEachEntry(new TObjectIntProcedure<String>() {
      @Override
      public boolean execute(String s, int count) {
        prefs.add(Pair.create(s, count));
        return true;
      }
    });
    Collections.sort(prefs, (o1, o2) -> o2.second.compareTo(o1.second));
    int i =0;
    for (Pair<String, Integer> pref : prefs) {
      Integer count = pref.second;
      System.out.printf("%60.60s : %d\n", pref.first, count);
      if (/*count<500 || */i++ > 100) {
        System.out.println("\n.......<" + count + "...\n");
        break;
      }
    }
    return prefs;
  }

  //TObjectIntHashMap<String> prefixes = new TObjectIntHashMap<String>();
  TObjectIntHashMap<String> suffixes = new TObjectIntHashMap<>();
  TObjectIntHashMap<String> nameCount = new TObjectIntHashMap<>();
  private void compute(VirtualFile root) {
    String name = root.getName();
    if (!nameCount.increment(name)) nameCount.put(name, 1);
    for (int i=1; i<=name.length(); i++) {
      //String prefix = name.substring(0, i);
      //if (!prefixes.increment(prefix)) prefixes.put(prefix, 1);

      String suffix = name.substring(name.length()-i);
      if (!suffixes.increment(suffix)) suffixes.put(suffix, 1);
    }
    Collection<VirtualFile> cachedChildren = ((VirtualFileSystemEntry)root).getCachedChildren();
    //VirtualFile[] cachedChildren = ((VirtualFileSystemEntry)root).getChildren();
    for (VirtualFile cachedChild : cachedChildren) {
      compute(cachedChild);
    }
  }
}
