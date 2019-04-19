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
package com.intellij.compiler.impl;

import com.intellij.util.containers.EmptyIterator;
import com.intellij.util.containers.StringInterner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;

/**
 * @author Eugene Zhuravlev
 */
public class TreeBasedMap<T> {
  private Node<T> myRoot = new Node<T>();
  private final StringInterner myInterner;
  private final char mySeparator;
  private int mySize = 0;

  public TreeBasedMap(StringInterner table, final char separator) {
    myInterner = table;
    mySeparator = separator;
  }

  private class Node<T> {
    private boolean myMappingExists = false;
    private T myValue = null;
    private @Nullable HashMap<String, Node<T>> myChildren = null;

    public void setValue(T value) {
      myValue = value;
      myMappingExists = true;
    }

    public T getValue() {
      return myValue;
    }

    public void removeValue() {
      myValue = null;
      myMappingExists = false;
    }

    public boolean mappingExists() {
      return myMappingExists;
    }

    @Nullable
    public Node<T> findRelative(String text, boolean create, final StringInterner table) {
      return findRelative(text, 0, create, table);
    }

    @Nullable
    private Node<T> findRelative(final String text, final int nameStartIndex, final boolean create, final StringInterner table) {
      if (myChildren == null && !create) {
        return null;
      }

      final int textLen = text.length();
      final int separatorIdx = text.indexOf(mySeparator, nameStartIndex);
      final int childNameEnd = separatorIdx >= 0 ? separatorIdx : textLen;

      if (myChildren != null) {
        final Node<T> child = myChildren.get(text.substring(nameStartIndex, childNameEnd));
        if (child != null) {
          if (separatorIdx < 0) {
            return child;
          }
          return child.findRelative(text, childNameEnd + 1, create, table);
        }
      }

      if (create) {
        return addChild(table, text, nameStartIndex, childNameEnd);
      }

      return null;
    }

    @NotNull
    private Node<T> addChild(final StringInterner table, final String text, final int nameStartIndex, final int nameEndIndex) {
      if (myChildren == null) {
        myChildren = new HashMap<String, Node<T>>(3, 0.95f);
      }

      Node<T> newChild = new Node<T>();
      final String key = table.intern(text.substring(nameStartIndex, nameEndIndex));
      myChildren.put(key, newChild);

      if (nameEndIndex == text.length()) {
        return newChild;
      }

      Node<T> restNodes = newChild.findRelative(text, nameEndIndex + 1, true, table);
      assert restNodes != null;
      return restNodes;
    }
  }

  public void put(String key, T value) {
    final Node<T> node = myRoot.findRelative(key, true, myInterner);
    assert node != null;
    final boolean mappingExisted = node.mappingExists();
    node.setValue(value);
    if (!mappingExisted) {
      mySize++;
    }
  }

  public void remove(String key) {
    final Node node = myRoot.findRelative(key, false, myInterner);
    if (node != null && node.mappingExists()) {
      node.removeValue();
      mySize--;
    }
  }

  public int size() {
    return mySize;
  }

  public T get(String key) {
    final Node<T> node = myRoot.findRelative(key, false, myInterner);
    return (node != null && node.mappingExists()) ? node.getValue() : null;
  }

  public boolean containsKey(String key) {
    final Node<T> node = myRoot.findRelative(key, false, myInterner);
    return node != null && node.mappingExists();
  }

  public void removeAll() {
    myRoot = new Node<T>();
  }

  public Iterator<String> getKeysIterator() {
    return new KeysIterator();
  }


  private class KeysIterator implements Iterator<String> {
    private final Stack<PathElement<T>> myCurrentNodePath = new Stack<PathElement<T>>();
    private final StringBuilder myCurrentName = new StringBuilder();

    KeysIterator() {
      pushNode("", myRoot);
      findNextNode();
    }

    @Override
    public boolean hasNext() {
      return myCurrentNodePath.size() > 0;
    }

    @Override
    public String next() {
      final String key = myCurrentName.toString();
      popNode();
      findNextNode();
      return key;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Remove not supported");
    }

    private boolean pushNode(final @NotNull String name, @NotNull Node<T> node) {
      final HashMap<String, Node<T>> childrenMap = node.myChildren;
      final boolean hasChildren = childrenMap != null && childrenMap.size() > 0;
      if (hasChildren || node.mappingExists()) {
        myCurrentNodePath.push(new PathElement<>(node, hasChildren ? childrenMap.keySet().iterator() : EmptyIterator.getInstance()));
        if (myCurrentNodePath.size() > 2) {
          // do not add separator before the Root and its direct child nodes
          myCurrentName.append(mySeparator);
        }
        myCurrentName.append(name);
        return true;
      }
      return false;
    }

    private void popNode() {
      myCurrentNodePath.pop();
      final int separatorIndex = myCurrentName.lastIndexOf(String.valueOf(mySeparator));
      if (separatorIndex >= 0) {
        myCurrentName.replace(separatorIndex, myCurrentName.length(), "");
      }
      else {
        myCurrentName.setLength(0);
      }
    }

    private void findNextNode() {
      MAIN_LOOP: while (!myCurrentNodePath.isEmpty()) {
        final PathElement<T> element = myCurrentNodePath.peek();
        final Iterator<String> childrenIterator = element.iterator;
        final Node<T> currentNode = element.node;
        while (childrenIterator.hasNext()) {
          final String name = childrenIterator.next();
          final Node<T> childNode = currentNode.myChildren.get(name);
          if (pushNode(name, childNode)) {
            continue MAIN_LOOP;
          }
        }
        if (!currentNode.mappingExists()) {
          popNode();
        }
        else {
          break;
        }
      }
    }
  }

  private class PathElement<T> {
    final @NotNull Iterator<String> iterator;
    final @NotNull Node<T> node;
    PathElement(@NotNull final Node<T> node, Iterator<String> iterator) {
      this.node = node;
      this.iterator = iterator;
    }
  }

}
