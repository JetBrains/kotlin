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

package com.intellij.psi.filters.position;

import com.intellij.psi.PsiElement;
import com.intellij.psi.filters.ElementFilter;

public class ParentElementFilter extends PositionElementFilter{
  private PsiElement myParent = null;
  private int myLevel = 1;
  public ParentElementFilter(ElementFilter filter){
    setFilter(filter);
  }

  public ParentElementFilter(ElementFilter filter, int level) {
    setFilter(filter);
    myLevel = level;
  }

  public ParentElementFilter(PsiElement parent){
    myParent = parent;
  }


  public ParentElementFilter(){}

  @Override
  public boolean isAcceptable(Object element, PsiElement scope){
    if (!(element instanceof PsiElement)) return false;
    PsiElement context = (PsiElement)element;
    for(int i = 0; i < myLevel && context != null; i++){
       context = context.getContext();
    }
    if(context != null){
      if(myParent == null){
        return getFilter().isAcceptable(context, scope);
      }
      return myParent == context;
    }
    return false;
  }


  public String toString(){
    return "parent(" +getFilter()+")";
  }

}
