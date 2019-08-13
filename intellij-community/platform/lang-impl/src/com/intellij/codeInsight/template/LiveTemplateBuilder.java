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
package com.intellij.codeInsight.template;

import com.intellij.codeInsight.template.impl.TemplateImpl;
import com.intellij.codeInsight.template.impl.Variable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Eugene.Kudelevsky
 */
public class LiveTemplateBuilder {
  @NonNls private static final String END_PREFIX = "____END";
  private static final Logger LOGGER = Logger.getInstance(LiveTemplateBuilder.class);

  private final StringBuilder myText = new StringBuilder();
  private final List<Variable> myVariables = new ArrayList<>();
  private final Set<String> myVarNames = new HashSet<>();
  private final List<VarOccurence> myVariableOccurrences = new ArrayList<>();
  private final List<Marker> myMarkers = new ArrayList<>();
  private final int mySegmentLimit;
  private final boolean myAddEndVariableAtTheEndOfTemplate;
  private String myLastEndVarName;
  private boolean myIsToReformat;


  @SuppressWarnings("UnusedDeclaration")
  public LiveTemplateBuilder() {
    this(false, Registry.intValue("emmet.segments.limit"));
  }
  
  public LiveTemplateBuilder(boolean addEndVariableAtTheEndOfTemplate, int segmentLimit) {
    mySegmentLimit = segmentLimit;
    myAddEndVariableAtTheEndOfTemplate = addEndVariableAtTheEndOfTemplate;
  }

  public void setIsToReformat(boolean isToReformat) {
    myIsToReformat = isToReformat;
  }

  public CharSequence getText() {
    return myText;
  }

  public static boolean isEndVariable(@NotNull String name) {
    return name.startsWith(END_PREFIX);
  }

  private static class VarOccurence {
    String myName;
    int myOffset;

    private VarOccurence(String name, int offset) {
      myName = name;
      myOffset = offset;
    }
  }

  public boolean findVarOccurence(String name) {
    for (VarOccurence occurence : myVariableOccurrences) {
      if (occurence.myName.equals(name)) {
        return true;
      }
    }
    return false;
  }

  @NotNull
  public TemplateImpl buildTemplate() {
    List<Variable> variables = getListWithLimit(myVariables);
    if (!findVarOccurence(TemplateImpl.END)) {
      if (myLastEndVarName == null) {
        for (Variable variable : variables) {
          if (isEndVariable(variable.getName())) {
            myLastEndVarName = variable.getName();
            break;
          }
        }
      }
      if (myLastEndVarName != null) {
        int endOffset = -1;
        if (myAddEndVariableAtTheEndOfTemplate) {
          endOffset = myText.length();
        }
        else {
          Iterator<VarOccurence> it = myVariableOccurrences.iterator();
          while (it.hasNext()) {
            VarOccurence occurence = it.next();
            if (occurence.myName.equals(myLastEndVarName)) {
              endOffset = occurence.myOffset;
              break;
            }
          }
          if (endOffset >= 0) {
            for (Iterator<Variable> it1 = variables.iterator(); it1.hasNext(); ) {
              Variable variable = it1.next();
              if (myLastEndVarName.equals(variable.getName()) && variable.isAlwaysStopAt()) {
                it.remove();
                it1.remove();
              }
            }
          }
        }
        if (endOffset >= 0) {
          myVariableOccurrences.add(new VarOccurence(TemplateImpl.END, endOffset));
        }
      }
    }
    TemplateImpl template = new TemplateImpl("", "");
    for (Variable variable : variables) {
      template.addVariable(variable.getName(), variable.getExpressionString(), variable.getDefaultValueString(), variable.isAlwaysStopAt());
    }

    List<VarOccurence> variableOccurrences = getListWithLimit(myVariableOccurrences);
    Collections.sort(variableOccurrences, Comparator.comparingInt(o -> o.myOffset));
    int last = 0;
    for (VarOccurence occurence : variableOccurrences) {
      template.addTextSegment(myText.substring(last, occurence.myOffset));
      template.addVariableSegment(occurence.myName);
      last = occurence.myOffset;
    }
    template.addTextSegment(myText.substring(last));
    template.setToReformat(myIsToReformat);
    return template;
  }

  private <T> List<T> getListWithLimit(List<T> list) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return list;
    }
    if (mySegmentLimit == 0) {
      return Collections.emptyList();
    }
    if (mySegmentLimit > 0 && list.size() > mySegmentLimit) {
      warnTooManySegments(list.size());
      return ContainerUtil.getFirstItems(list, mySegmentLimit);
    }
    return list;
  }

  public void insertText(int offset, String text, boolean disableEndVariable) {
    if (disableEndVariable) {
      String varName = null;
      for (VarOccurence occurence : myVariableOccurrences) {
        if (!isEndVariable(occurence.myName)) {
          continue;
        }
        if (occurence.myOffset == offset) {
          varName = occurence.myName;
          break;
        }
      }
      if (varName != null) {
        for (Variable variable : myVariables) {
          if (varName.equals(variable.getName())) {
            variable.setAlwaysStopAt(false);
            variable.setDefaultValueString("\"\"");
            break;
          }
        }
      }
    }
    int delta = text.length();
    for (VarOccurence occurence : myVariableOccurrences) {
      if (occurence.myOffset > offset || !disableEndVariable && occurence.myOffset == offset) {
        occurence.myOffset += delta;
      }
    }
    myText.insert(offset, text);
    updateMarkers(offset, text);
  }

  public int length() {
    return myText.length();
  }

  private void updateMarkers(int offset, String text) {
    for (Marker marker : myMarkers) {
      if (offset < marker.getStartOffset()) {
        marker.myStartOffset += text.length();
      }
      else if (offset <= marker.getEndOffset()) {
        marker.myEndOffset += text.length();
      }
    }
  }

  private String generateUniqueVarName(Set<String> existingNames, boolean end) {
    String prefix = end ? END_PREFIX : "VAR";
    int i = 0;
    while (myVarNames.contains(prefix + i) || existingNames.contains(prefix + i)) {
      i++;
    }
    return prefix + i;
  }

  public int insertTemplate(int offset, TemplateImpl template, Map<String, String> predefinedVarValues) {
    myIsToReformat = myText.length() > 0 || template.isToReformat();
    removeEndVarAtOffset(offset);

    String text = template.getTemplateText();
    insertText(offset, text, false);
    Set<String> oldVarNames = new HashSet<>();
    for (int i = 0; i < template.getVariableCount(); i++) {
      String varName = template.getVariableNameAt(i);
      oldVarNames.add(varName);
    }
    Map<String, String> newVarNames = new HashMap<>();
    for (int i = 0; i < template.getVariableCount(); i++) {
      String varName = template.getVariableNameAt(i);
      if (!TemplateImpl.INTERNAL_VARS_SET.contains(varName)) {
        if (predefinedVarValues != null && predefinedVarValues.containsKey(varName)) {
          continue;
        }
        String newVarName;
        if (myVarNames.contains(varName)) {
          oldVarNames.remove(varName);
          newVarName = generateUniqueVarName(oldVarNames, isEndVariable(varName));
          newVarNames.put(varName, newVarName);
          if (varName.equals(myLastEndVarName)) {
            myLastEndVarName = newVarName;
          }
        }
        else {
          newVarName = varName;
        }
        Variable var = new Variable(newVarName, template.getExpressionStringAt(i), template.getDefaultValueStringAt(i), template.isAlwaysStopAt(i));
        if (mySegmentLimit >= 0 && myVariables.size() >= mySegmentLimit) {
          if (mySegmentLimit > 0) {
            warnTooManySegments(myVariables.size());
          }
          break;
        }
        myVariables.add(var);
        myVarNames.add(newVarName);
      }
    }
    int end = -1;

    for (int i = 0; i < template.getSegmentsCount(); i++) {
      String segmentName = template.getSegmentName(i);
      int localOffset = template.getSegmentOffset(i);
      if (TemplateImpl.END.equals(segmentName)) {
        end = offset + localOffset;
      } 
      else {
        if (predefinedVarValues != null && predefinedVarValues.containsKey(segmentName)) {
          String value = predefinedVarValues.get(segmentName);
          insertText(offset + localOffset, value, false);
          offset += value.length();
          continue;
        }
        if (newVarNames.containsKey(segmentName)) {
          segmentName = newVarNames.get(segmentName);
        }
        myVariableOccurrences.add(new VarOccurence(segmentName, offset + localOffset));
      }
    }
    int endOffset = end >= 0 ? end : offset + text.length();
    if (endOffset > 0 &&
        endOffset != offset + text.length() &&
        endOffset < myText.length() &&
        !hasVarAtOffset(endOffset)) {
      myLastEndVarName = generateUniqueVarName(myVarNames, true);
      myVariables.add(new Variable(myLastEndVarName, "", "", true));
      myVarNames.add(myLastEndVarName);
      myVariableOccurrences.add(new VarOccurence(myLastEndVarName, endOffset));
    }
    return endOffset;
  }

  private void warnTooManySegments(int size) {
    LOGGER.warn("Too many (" + size + " with the limit of " + mySegmentLimit + ") segments were requested" +
                " for the template with the text: " + myText);
  }

  private void removeEndVarAtOffset(int offset) {
    for (Iterator<VarOccurence> it = myVariableOccurrences.iterator(); it.hasNext();) {
      VarOccurence occurence = it.next();
      if (!isEndVariable(occurence.myName)) {
        continue;
      }
      if (occurence.myOffset == offset) {
        it.remove();
        myVariables.removeIf(variable -> occurence.myName.equals(variable.getName()));
      }
    }
  }

  private boolean hasVarAtOffset(int offset) {
    boolean flag = false;
    for (VarOccurence occurence : myVariableOccurrences) {
      if (occurence.myOffset == offset) {
        flag = true;
        break;
      }
    }
    return flag;
  }

  public Marker createMarker(int offset) {
    Marker marker = new Marker(offset, offset);
    myMarkers.add(marker);
    return marker;
  }

  public static class Marker {
    int myStartOffset;
    int myEndOffset;

    private Marker(int startOffset, int endOffset) {
      myStartOffset = startOffset;
      myEndOffset = endOffset;
    }

    public int getStartOffset() {
      return myStartOffset;
    }

    public int getEndOffset() {
      return myEndOffset;
    }
  }
}
