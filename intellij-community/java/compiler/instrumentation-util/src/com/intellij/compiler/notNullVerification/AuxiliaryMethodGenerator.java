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
package com.intellij.compiler.notNullVerification;

import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.MethodVisitor;

import java.util.*;

import static org.jetbrains.org.objectweb.asm.Opcodes.*;

/**
 * @author peter
 */
class AuxiliaryMethodGenerator {
  private static final String STRING_CLASS_NAME = "java/lang/String";
  private static final String OBJECT_CLASS_NAME = "java/lang/Object";
  private static final String CONSTRUCTOR_NAME = "<init>";
  private static final String EXCEPTION_INIT_SIGNATURE = "(L" + STRING_CLASS_NAME + ";)V";
  private static final String REPORTING_METHOD_DESC = "(I)V";

  private final ClassReader myOriginalClass;
  private final List<ReportingPlace> myReportingPlaces = new ArrayList<ReportingPlace>();
  private String myReportingMethod;
  private int myMaxArgCount;

  AuxiliaryMethodGenerator(ClassReader originalClass) {
    myOriginalClass = originalClass;
  }

  private String getReportingMethodName() {
    if (myReportingMethod == null) {
      myReportingMethod = suggestUniqueName();
    }
    return myReportingMethod;
  }

  private String suggestUniqueName() {
    Set<String> existingMethods = populateExistingMethods();
    for (int i = 0;; i++) {
      String name = "$$$reportNull$$$" + i;
      if (!existingMethods.contains(name)) {
        return name;
      }
    }
  }

  private Set<String> populateExistingMethods() {
    final Set<String> existingMethods = new HashSet<String>();
    myOriginalClass.accept(new ClassVisitor(API_VERSION) {
      @Override
      public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        existingMethods.add(name);
        return null;
      }
    }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    return existingMethods;
  }

  void reportError(MethodVisitor mv, String className, String exceptionClass, String descrPattern, String[] args) {
    myMaxArgCount = Math.max(myMaxArgCount, args.length);

    int index = myReportingPlaces.size();
    myReportingPlaces.add(new ReportingPlace(exceptionClass, descrPattern, args));
    pushIntConstant(mv, index);

    mv.visitMethodInsn(INVOKESTATIC, className, getReportingMethodName(), REPORTING_METHOD_DESC, false);
  }

  private static void pushIntConstant(MethodVisitor mv, int i) {
    if (i <= 5) {
      mv.visitInsn(getSmallIntConstantInstruction(i));
    }
    else if (i <= Byte.MAX_VALUE) {
      mv.visitIntInsn(BIPUSH, i);
    }
    else {
      mv.visitIntInsn(SIPUSH, i);
    }
  }

  private static int getSmallIntConstantInstruction(int i) {
    switch (i) {
      case 0: return ICONST_0;
      case 1: return ICONST_1;
      case 2: return ICONST_2;
      case 3: return ICONST_3;
      case 4: return ICONST_4;
      case 5: return ICONST_5;
      default: throw new AssertionError(i);
    }
  }

  void generateReportingMethod(ClassVisitor cw) {
    if (myReportingPlaces.isEmpty()) return;

    MethodVisitor mv = cw.visitMethod(ACC_PRIVATE | ACC_SYNTHETIC | ACC_STATIC, getReportingMethodName(), REPORTING_METHOD_DESC, null, null);
    pushExceptionMessage(mv);
    createExceptionObject(mv);
    mv.visitInsn(ATHROW);
    mv.visitMaxs(0, 0);
  }

  private void createExceptionObject(final MethodVisitor mv) {
    new SwitchGenerator<String>() {
      @Override
      void generateCaseBody(String exceptionClass) {
        mv.visitTypeInsn(NEW, exceptionClass);
        mv.visitInsn(DUP_X1);
        mv.visitInsn(SWAP);

        mv.visitMethodInsn(INVOKESPECIAL, exceptionClass, CONSTRUCTOR_NAME, EXCEPTION_INIT_SIGNATURE, false);
      }

      @Override
      String getSwitchedValue(ReportingPlace place) {
        return place.exceptionClass;
      }
    }.generateSwitch(mv);
  }

  private void pushExceptionMessage(MethodVisitor mv) {
    pushFormatPattern(mv);

    createFormatArgArray(mv);
    for (int i = 0; i < myMaxArgCount; i++) {
      pushFormatArg(mv, i);
    }

    //noinspection SpellCheckingInspection
    mv.visitMethodInsn(INVOKESTATIC, STRING_CLASS_NAME, "format", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", false);
  }

  private void createFormatArgArray(final MethodVisitor mv) {
    new SwitchGenerator<Integer>(){
      @Override
      void generateCaseBody(Integer argCount) {
        pushIntConstant(mv, argCount);
      }

      @Override
      Integer getSwitchedValue(ReportingPlace place) {
        return place.args.length;
      }
    }.generateSwitch(mv);
    mv.visitTypeInsn(ANEWARRAY, OBJECT_CLASS_NAME);
  }

  private void pushFormatArg(final MethodVisitor mv, final int index) {
    new SwitchGenerator<String>() {
      @Override
      protected String getSwitchedValue(ReportingPlace place) {
        return index < place.args.length ? place.args[index] : null;
      }

      @Override
      void generateCaseBody(String value) {
        if (value != null) {
          mv.visitInsn(DUP);
          pushIntConstant(mv, index);
          mv.visitLdcInsn(value);
          mv.visitInsn(AASTORE);
        }
      }
    }.generateSwitch(mv);
  }

  private void pushFormatPattern(final MethodVisitor mv) {
    new SwitchGenerator<String>() {
      @Override
      protected String getSwitchedValue(ReportingPlace place) {
        return place.descrPattern;
      }

      @Override
      void generateCaseBody(String descrPattern) {
        mv.visitLdcInsn(descrPattern);
      }
    }.generateSwitch(mv);
  }

  private static class ReportingPlace {
    final String exceptionClass;
    final String descrPattern;
    final String[] args;

    ReportingPlace(String exceptionClass, String descrPattern, String[] args) {
      this.exceptionClass = exceptionClass;
      this.descrPattern = descrPattern;
      this.args = args;
    }
  }

  private abstract class SwitchGenerator<T> {

    void generateSwitch(MethodVisitor mv) {
      Label[] labels = getCaseLabels();
      if (labels == null) {
        // all places behave in a same way, don't bother with switch
        generateCaseBody(getSwitchedValue(myReportingPlaces.get(0)));
      } else {
        reallyGenerateSwitch(mv, labels, deduplicateLabels(labels));
      }
    }

    private void reallyGenerateSwitch(MethodVisitor mv, Label[] labels, Map<Label, ReportingPlace> label2Place) {
      Label afterSwitch = new Label();
      mv.visitVarInsn(ILOAD, 0);
      mv.visitTableSwitchInsn(0, labels.length - 1 , labels[0], labels);

      for (Map.Entry<Label, ReportingPlace> entry : label2Place.entrySet()) {
        mv.visitLabel(entry.getKey());
        generateCaseBody(getSwitchedValue(entry.getValue()));
        mv.visitJumpInsn(GOTO, afterSwitch);
      }

      mv.visitLabel(afterSwitch);
    }

    private Map<Label, ReportingPlace> deduplicateLabels(Label[] labels) {
      Map<Label, ReportingPlace> label2Place = new LinkedHashMap<Label, ReportingPlace>();
      for (int i = 0; i < labels.length; i++) {
        if (!label2Place.containsKey(labels[i])) {
          label2Place.put(labels[i], myReportingPlaces.get(i));
        }
      }
      return label2Place;
    }

    private Label[] getCaseLabels() {
      Map<T, Label> labelsByValue = new HashMap<T, Label>();
      Label[] labels = new Label[myReportingPlaces.size()];
      for (int i = 0; i < myReportingPlaces.size(); i++) {
        labels[i] = getOrCreateLabel(labelsByValue, getSwitchedValue(myReportingPlaces.get(i)));
      }
      return labelsByValue.size() == 1 ? null : labels;
    }

    private Label getOrCreateLabel(Map<T, Label> labelsByValue, T key) {
      Label label = labelsByValue.get(key);
      if (label == null) {
        labelsByValue.put(key, label = new Label());
      }
      return label;
    }

    abstract void generateCaseBody(T switchedValue);

    abstract T getSwitchedValue(ReportingPlace place);
  }
}
