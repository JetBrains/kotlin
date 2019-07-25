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

package com.intellij.refactoring.classMembers;

import com.intellij.psi.PsiElement;

import java.util.*;

/**
 * @author Dennis.Ushakov
 */
public abstract class AbstractMemberInfoStorage<T extends PsiElement, C extends PsiElement, M extends MemberInfoBase<T>> {
  protected final HashMap<C, LinkedHashSet<C>> myClassToSubclassesMap = new HashMap<>();
  private final HashMap<C, Set<C>> myTargetClassToExtendingMap = new HashMap<>();
  private final HashMap<C, List<M>> myClassToMemberInfoMap = new HashMap<>();
  protected final C myClass;
  protected final MemberInfoBase.Filter<T> myFilter;
  private final HashMap<C, List<M>> myTargetClassToIntermediateMemberInfosMap = new HashMap<>();
  private final HashMap<C, LinkedHashSet<M>> myTargetClassToMemberInfosListMap = new HashMap<>();
  private final HashMap<C, HashSet<M>> myTargetClassToDuplicatedMemberInfosMap = new HashMap<>();

  public AbstractMemberInfoStorage(C aClass, MemberInfoBase.Filter<T> memberInfoFilter) {
    myClass = aClass;
    buildSubClassesMap(aClass);
    myFilter = memberInfoFilter;
  }

  private Set<C> getAllClasses() {
    return myClassToSubclassesMap.keySet();
  }

  public Set<C> getExtending(C baseClass) {
    Set<C> result = myTargetClassToExtendingMap.get(baseClass);
    if(result == null) {
      result = new HashSet<>();
      result.add(baseClass);
      final Set<C> allClasses = getAllClasses();
      for (C aClass : allClasses) {
        if (isInheritor(baseClass, aClass)) {
          result.add(aClass);
        }
      }
      myTargetClassToExtendingMap.put(baseClass, result);
    }

    return result;
  }

  protected abstract boolean isInheritor(C baseClass, C aClass);

  protected abstract void buildSubClassesMap(C aClass);

  public List<M> getClassMemberInfos(C aClass) {
    List<M> result = myClassToMemberInfoMap.get(aClass);
    if(result == null) {
      ArrayList<M> temp = new ArrayList<>();
      extractClassMembers(aClass, temp);
      result = Collections.unmodifiableList(temp);
      myClassToMemberInfoMap.put(aClass, result);
    }
    return result;
  }

  protected abstract void extractClassMembers(C aClass, ArrayList<M> temp);

  public List<M> getIntermediateMemberInfosList(C baseClass) {
    List<M> result = myTargetClassToIntermediateMemberInfosMap.get(baseClass);

    if (result == null) {
      Set<M> list = getIntermediateClassesMemberInfosList(baseClass, new HashSet<>());
      result = Collections.unmodifiableList(new ArrayList<>(list));
      myTargetClassToIntermediateMemberInfosMap.put(baseClass, result);
    }

    return result;
  }

  private Set<M> getIntermediateClassesMemberInfosList(C targetClass, Set<? super C> visited) {
    LinkedHashSet<M> result = myTargetClassToMemberInfosListMap.get(targetClass);
    if(result == null) {
      result = new LinkedHashSet<>();
      Set<C> subclasses = getSubclasses(targetClass);
      for (C subclass : subclasses) {
        List<M> memberInfos = getClassMemberInfos(subclass);
        result.addAll(memberInfos);
      }
      for (C subclass : subclasses) {
        if (visited.add(subclass)) {
          result.addAll(getIntermediateClassesMemberInfosList(subclass, visited));
        }
      }
      myTargetClassToMemberInfosListMap.put(targetClass, result);
    }
    return result;
  }

  protected LinkedHashSet<C> getSubclasses(C aClass) {
    return myClassToSubclassesMap.computeIfAbsent(aClass, k -> new LinkedHashSet<>());
  }

  public Set<M> getDuplicatedMemberInfos(C baseClass) {
    return myTargetClassToDuplicatedMemberInfosMap.computeIfAbsent(baseClass, k -> buildDuplicatedMemberInfos(baseClass));
  }

  private HashSet<M> buildDuplicatedMemberInfos(C baseClass) {
    HashSet<M> result = new HashSet<>();
    List<M> memberInfos = getIntermediateMemberInfosList(baseClass);

    for (int i = 0; i < memberInfos.size(); i++) {
      final M memberInfo = memberInfos.get(i);
      final T member = memberInfo.getMember();

      for(int j = 0; j < i; j++) {
        final M memberInfo1 = memberInfos.get(j);
        final T member1 = memberInfo1.getMember();
        if(memberConflict(member1,  member)) {
          result.add(memberInfo);
//        We let the first one be...
//          result.add(memberInfo1);
        }
      }
    }
    return result;
  }

  protected abstract boolean memberConflict(T member1, T member);
}
