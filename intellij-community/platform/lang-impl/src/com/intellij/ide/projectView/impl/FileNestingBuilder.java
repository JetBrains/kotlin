// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.MultiMap;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Helper component that stores nesting rules and apply them to files
 *
 * @see NestingTreeStructureProvider
 */
public class FileNestingBuilder {

  public static FileNestingBuilder getInstance() {
    return ServiceManager.getService(FileNestingBuilder.class);
  }

  private long myBaseListModCount = -1;
  private Set<ProjectViewFileNestingService.NestingRule> myNestingRules;

  /**
   * Returns all possible nesting rules, including transitive rules
   */
  @NotNull
  public synchronized Collection<ProjectViewFileNestingService.NestingRule> getNestingRules() {
    final ProjectViewFileNestingService fileNestingService = ProjectViewFileNestingService.getInstance();
    final List<ProjectViewFileNestingService.NestingRule> baseRules = fileNestingService.getRules();
    final long modCount = fileNestingService.getModificationCount();

    if (myNestingRules == null || myBaseListModCount != modCount) {
      myNestingRules = new THashSet<>();
      myBaseListModCount = modCount;

      final MultiMap<String, String> childToParentSuffix = new MultiMap<>();
      final MultiMap<String, String> parentToChildSuffix = new MultiMap<>();

      for (ProjectViewFileNestingService.NestingRule rule : baseRules) {
        final String parentFileSuffix = rule.getParentFileSuffix();
        final String childFileSuffix = rule.getChildFileSuffix();
        if (parentFileSuffix.isEmpty() || childFileSuffix.isEmpty()) continue; // shouldn't happen, checked on component loading and in UI
        if (parentFileSuffix.equals(childFileSuffix)) continue; // shouldn't happen, checked on component loading and in UI

        myNestingRules.add(rule);
        childToParentSuffix.putValue(childFileSuffix, parentFileSuffix);
        parentToChildSuffix.putValue(parentFileSuffix, childFileSuffix);

        // for all cases like A -> B -> C we also add a rule A -> C
        for (String s : parentToChildSuffix.get(childFileSuffix)) {
          myNestingRules.add(new ProjectViewFileNestingService.NestingRule(parentFileSuffix, s));
          parentToChildSuffix.putValue(parentFileSuffix, s);
          childToParentSuffix.putValue(s, parentFileSuffix);
        }

        for (String s : childToParentSuffix.get(parentFileSuffix)) {
          myNestingRules.add(new ProjectViewFileNestingService.NestingRule(s, childFileSuffix));
          parentToChildSuffix.putValue(s, childFileSuffix);
          childToParentSuffix.putValue(childFileSuffix, s);
        }
      }
    }

    return myNestingRules;
  }

  /*
    This is a graph theory problem. T is a graph node that represents a file. fileNameFunc should return appropriate file name for a T node.
    Edges go from parent file to child file according to NestingRules, for example foo.js->foo.min.js.
    Parent may have several children. Child may have several parents.
    There may be cycles with 3 or more nodes, but cycle with 2 nodes (A->B and B->A) is impossible because parentFileSuffix != childFileSuffix
    For each child its outbound edges are removed. For example in case of a cycle all edges that form it are removed. In case of A->B->C only A->B remains.
    As a result we get a number of separated parent-to-many-children sub-graphs, and use them to nest child files under parent file in Project View.
    One child still may have more than one parent. For real use cases it is not expected to happen, but anyway it's not a big problem, it will be shown as a subnode more than once.
   */
  @NotNull
  public <T> MultiMap<T, T> mapParentToChildren(@NotNull final Collection<? extends T> nodes,
                                                @NotNull final Function<? super T, String> fileNameFunc) {

    final Collection<ProjectViewFileNestingService.NestingRule> rules = getNestingRules();
    if (rules.isEmpty()) return MultiMap.empty();

    // result that will contain number of separated parent-to-many-children sub-graphs
    MultiMap<T, T> parentToChildren = null;

    Set<T> allChildNodes = null; // helps to remove all outbound edges of a node that has inbound edge itself
    Map<Pair<String, ProjectViewFileNestingService.NestingRule>, Edge<T>> baseNameAndRuleToEdge = null; // temporary map for building edges

    for (T node : nodes) {
      final String fileName = fileNameFunc.apply(node);
      if (fileName == null) continue;

      for (ProjectViewFileNestingService.NestingRule rule : rules) {
        final Couple<Boolean> c = checkMatchingAsParentOrChild(rule, fileName);
        final boolean matchesParent = c.first;
        final boolean matchesChild = c.second;

        if (!matchesChild && !matchesParent) continue;

        if (baseNameAndRuleToEdge == null) {
          baseNameAndRuleToEdge = new THashMap<>();
          parentToChildren = new MultiMap<>();
          allChildNodes = new THashSet<>();
        }

        if (matchesParent) {
          final String baseName = fileName.substring(0, fileName.length() - rule.getParentFileSuffix().length());
          final Edge<T> edge = getOrCreateEdge(baseNameAndRuleToEdge, baseName, rule);
          edge.from = node;
          updateInfoIfEdgeComplete(parentToChildren, allChildNodes, edge);
        }

        if (matchesChild) {
          final String baseName = fileName.substring(0, fileName.length() - rule.getChildFileSuffix().length());
          final Edge<T> edge = getOrCreateEdge(baseNameAndRuleToEdge, baseName, rule);
          edge.to = node;
          updateInfoIfEdgeComplete(parentToChildren, allChildNodes, edge);
        }
      }
    }

    return parentToChildren == null ? MultiMap.empty() : parentToChildren;
  }

  /**
   * Returns true if the rule applies to the file [as parent; as child] pair
   */
  public static Couple<Boolean> checkMatchingAsParentOrChild(@NotNull final ProjectViewFileNestingService.NestingRule rule,
                                                             @NotNull final String fileName) {
    String parentFileSuffix = rule.getParentFileSuffix();
    String childFileSuffix = rule.getChildFileSuffix();

    boolean matchesParent = /*!fileName.equalsIgnoreCase(parentFileSuffix) &&*/ StringUtil.endsWithIgnoreCase(fileName, parentFileSuffix);
    boolean matchesChild = /*!fileName.equalsIgnoreCase(childFileSuffix) &&*/ StringUtil.endsWithIgnoreCase(fileName, childFileSuffix);

    if (matchesParent && matchesChild) {
      if (parentFileSuffix.length() > childFileSuffix.length()) {
        matchesChild = false;
      }
      else {
        matchesParent = false;
      }
    }

    return Couple.of(matchesParent, matchesChild);
  }

  @NotNull
  private static <T> Edge<T> getOrCreateEdge(@NotNull final Map<Pair<String, ProjectViewFileNestingService.NestingRule>, Edge<T>> baseNameAndRuleToEdge,
                                             @NotNull final String baseName,
                                             @NotNull final ProjectViewFileNestingService.NestingRule rule) {
    final Pair<String, ProjectViewFileNestingService.NestingRule> baseNameAndRule = Pair.create(baseName, rule);

    Edge<T> edge = baseNameAndRuleToEdge.get(baseNameAndRule);
    if (edge == null) {
      edge = new Edge<>();
      baseNameAndRuleToEdge.put(baseNameAndRule, edge);
    }
    return edge;
  }

  private static <T> void updateInfoIfEdgeComplete(@NotNull final MultiMap<T, T> parentToChildren,
                                                   @NotNull final Set<? super T> allChildNodes,
                                                   @NotNull final Edge<? extends T> edge) {
    if (edge.from != null && edge.to != null) { // if edge complete
      allChildNodes.add(edge.to);
      parentToChildren.remove(edge.to); // nodes that appear as a child shouldn't be a parent of another edge, corresponding edges removed
      if (!allChildNodes.contains(edge.from)) {
        parentToChildren.putValue(edge.from, edge.to);
      }
    }
  }

  private static class Edge<T> {
    @Nullable
    private T from;
    @Nullable
    private T to;
  }
}
