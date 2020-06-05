// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.notNullVerification;

import com.intellij.compiler.instrumentation.FailSafeClassReader;
import com.intellij.compiler.instrumentation.FailSafeMethodVisitor;
import org.jetbrains.org.objectweb.asm.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/**
 * @author ven
 */
public class NotNullVerifyingInstrumenter extends ClassVisitor implements Opcodes {
  private static final String IAE_CLASS_NAME = "java/lang/IllegalArgumentException";
  private static final String ISE_CLASS_NAME = "java/lang/IllegalStateException";

  private static final String ANNOTATION_DEFAULT_METHOD = "value";

  @SuppressWarnings("SSBasedInspection")
  private static final String[] EMPTY_STRING_ARRAY = new String[0];

  private final MethodData myMethodData;
  private boolean myIsModification = false;
  private RuntimeException myPostponedError;
  private final AuxiliaryMethodGenerator myAuxGenerator;

  private NotNullVerifyingInstrumenter(ClassVisitor classVisitor, ClassReader reader, String[] notNullAnnotations) {
    super(Opcodes.API_VERSION, classVisitor);
    Set<String> annoSet = new HashSet<String>();
    for (String annotation : notNullAnnotations) {
      annoSet.add('L' + annotation.replace('.', '/') + ';');
    }
    myMethodData = collectMethodData(reader, annoSet);
    myAuxGenerator = new AuxiliaryMethodGenerator(reader);
  }

  /**
   * @deprecated use {@link NotNullVerifyingInstrumenter#processClassFile(ClassReader, ClassVisitor, String[])} instead
   */
  @Deprecated
  public static boolean processClassFile(FailSafeClassReader reader, ClassVisitor writer, String[] notNullAnnotations) {
    return processClassFile((ClassReader)reader, writer, notNullAnnotations);
  }

  public static boolean processClassFile(ClassReader reader, ClassVisitor writer, String[] notNullAnnotations) {
    NotNullVerifyingInstrumenter instrumenter = new NotNullVerifyingInstrumenter(writer, reader, notNullAnnotations);
    reader.accept(instrumenter, 0);
    return instrumenter.myIsModification;
  }

  private static class MethodInfo {
    final NotNullState nullability = new NotNullState();
    final Map<Integer, String> paramNames = new HashMap<Integer, String>();
    final Map<Integer, NotNullState> paramNullability = new LinkedHashMap<Integer, NotNullState>();
    boolean isStable;
    int paramAnnotationOffset;

    NotNullState obtainParameterNullability(int index) {
      NotNullState state = paramNullability.get(index);
      if (state == null) {
        state = new NotNullState();
        paramNullability.put(index, state);
      }
      return state;
    }
  }

  private static final class MethodData {
    private String myClassName;
    private final Map<String, MethodInfo> myMethodInfos = new HashMap<String, MethodInfo>();

    static String key(String methodName, String desc) {
      return methodName + desc;
    }

    String lookupParamName(String methodName, String desc, Integer num) {
      MethodInfo info = myMethodInfos.get(key(methodName, desc));
      Map<Integer, String> names = info == null ? null : info.paramNames;
      return names != null ? names.get(num) : null;
    }

    boolean isAlwaysNotNull(String className, String methodName, String desc) {
      if (myClassName.equals(className)) {
        MethodInfo info = myMethodInfos.get(key(methodName, desc));
        return info != null && info.isStable && info.nullability.isNotNull();
      }
      return false;
    }
  }

  private static MethodData collectMethodData(ClassReader reader, final Set<String> notNullAnnotations) {
    final MethodData result = new MethodData();
    reader.accept(new ClassVisitor(Opcodes.API_VERSION) {
      private boolean myEnum, myInner;

      @Override
      public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        result.myClassName = name;
        myEnum = (access & ACC_ENUM) != 0;
      }

      @Override
      public void visitInnerClass(String name, String outerName, String innerName, int access) {
        super.visitInnerClass(name, outerName, innerName, access);
        if (result.myClassName.equals(name)) {
          myInner = (access & ACC_STATIC) == 0;
        }
      }

      @Override
      public MethodVisitor visitMethod(int access, final String name, final String desc, String signature, String[] exceptions) {
        final Type[] args = Type.getArgumentTypes(desc);
        final boolean methodCanHaveNullability = isReferenceType(Type.getReturnType(desc));

        final Map<Integer, Integer> paramSlots = new LinkedHashMap<Integer, Integer>(); // map: localVariableSlot -> methodParameterIndex
        int slotIndex = isStatic(access) ? 0 : 1;
        for (int paramIndex = 0; paramIndex < args.length; paramIndex++) {
          Type arg = args[paramIndex];
          paramSlots.put(slotIndex, paramIndex);
          slotIndex += arg.getSize();
        }

        final MethodInfo methodInfo = new MethodInfo();
        methodInfo.isStable = (access & (Opcodes.ACC_FINAL | Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) != 0;
        methodInfo.paramAnnotationOffset = !"<init>".equals(name) ? 0 : myEnum ? 2 : myInner ? 1 : 0;
        result.myMethodInfos.put(MethodData.key(name, desc), methodInfo);

        return new MethodVisitor(api) {
          private int myParamAnnotationOffset = methodInfo.paramAnnotationOffset;

          @Override
          public void visitAnnotableParameterCount(int parameterCount, boolean visible) {
            if (myParamAnnotationOffset != 0 && parameterCount == args.length) {
              myParamAnnotationOffset = 0;
            }
            super.visitAnnotableParameterCount(parameterCount, visible);
          }

          @Override
          public AnnotationVisitor visitParameterAnnotation(int parameter, String anno, boolean visible) {
            AnnotationVisitor base = super.visitParameterAnnotation(parameter, anno, visible);
            return checkParameterNullability(parameter + myParamAnnotationOffset, anno, base, false);
          }

          @Override
          public AnnotationVisitor visitAnnotation(String anno, boolean isRuntime) {
            AnnotationVisitor base = super.visitAnnotation(anno, isRuntime);
            if (methodCanHaveNullability && notNullAnnotations.contains(anno)) {
              return collectNotNullArgs(base, methodInfo.nullability.withNotNull(anno, ISE_CLASS_NAME));
            }
            return base;
          }

          @Override
          public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String anno, boolean visible) {
            AnnotationVisitor base = super.visitTypeAnnotation(typeRef, typePath, anno, visible);
            if (typePath != null) return base;

            TypeReference ref = new TypeReference(typeRef);
            if (methodCanHaveNullability && ref.getSort() == TypeReference.METHOD_RETURN) {
              if (notNullAnnotations.contains(anno)) {
                return collectNotNullArgs(base, methodInfo.nullability.withNotNull(anno, ISE_CLASS_NAME));
              }
              else if (seemsNullable(anno)) {
                methodInfo.nullability.hasTypeUseNullable = true;
              }
            }
            else if (ref.getSort() == TypeReference.METHOD_FORMAL_PARAMETER) {
              return checkParameterNullability(ref.getFormalParameterIndex() + methodInfo.paramAnnotationOffset, anno, base, true);
            }

            return base;
          }

          private boolean seemsNullable(String anno) {
            String shortName = getAnnoShortName(anno);
            // use hardcoded short names until it causes trouble
            // this is to avoid cumbersome passing of configured nullable names from the IDE
            return shortName.contains("Nullable") || shortName.equals("CheckForNull");
          }

          private AnnotationVisitor collectNotNullArgs(AnnotationVisitor base, final NotNullState state) {
            return new AnnotationVisitor(Opcodes.API_VERSION, base) {
              @Override
              public void visit(String methodName, Object o) {
                if (ANNOTATION_DEFAULT_METHOD.equals(methodName) && !((String) o).isEmpty()) {
                  state.message = (String) o;
                }
                else if ("exception".equals(methodName) && o instanceof Type && !((Type)o).getClassName().equals(Exception.class.getName())) {
                  state.exceptionType = ((Type)o).getInternalName();
                }
                super.visit(methodName, o);
              }
            };
          }

          private AnnotationVisitor checkParameterNullability(int parameter, String anno, AnnotationVisitor av, boolean typeUse) {
            if (parameter >= 0 && parameter < args.length && isReferenceType(args[parameter])) {
              if (notNullAnnotations.contains(anno)) {
                return collectNotNullArgs(av, methodInfo.obtainParameterNullability(parameter).withNotNull(anno, IAE_CLASS_NAME));
              }
              else if (typeUse && seemsNullable(anno)) {
                methodInfo.obtainParameterNullability(parameter).hasTypeUseNullable = true;
              }
            }

            return av;
          }

          @Override
          public void visitLocalVariable(String name2, String desc, String signature, Label start, Label end, int slotIndex) {
            Integer paramIndex = paramSlots.get(slotIndex);
            if (paramIndex != null) {
              methodInfo.paramNames.put(paramIndex, name2);
            }
          }
        };
      }
    }, ClassReader.SKIP_FRAMES);
    return result;
  }

  private static class NotNullState {
    String message;
    String exceptionType;
    String notNullAnno;
    boolean hasTypeUseNullable;

    NotNullState withNotNull(String notNullAnno, String exceptionType) {
      this.notNullAnno = notNullAnno;
      this.exceptionType = exceptionType;
      return this;
    }

    boolean isNotNull() {
      return notNullAnno != null && !hasTypeUseNullable;
    }

    String getNullParamMessage(String paramName) {
      if (message != null) return message;
      String shortName = getAnnoShortName(notNullAnno);
      if (paramName != null) return "Argument for @" + shortName + " parameter '%s' of %s.%s must not be null";
      return "Argument %s for @" + shortName + " parameter of %s.%s must not be null";
    }

    String getNullResultMessage() {
      if (message != null) return message;
      String shortName = getAnnoShortName(notNullAnno);
      return "@" + shortName + " method %s.%s must not return null";
    }
  }

  private static String getAnnoShortName(String anno) {
    String fullName = anno.substring(1, anno.length() - 1); // "Lpk/name;" -> "pk/name"
    return fullName.substring(fullName.lastIndexOf('/') + 1);
  }

  @Override
  public MethodVisitor visitMethod(int access, final String name, final String desc, String signature, String[] exceptions) {
    final MethodInfo info = myMethodData.myMethodInfos.get(MethodData.key(name, desc));
    if ((access & Opcodes.ACC_BRIDGE) != 0 || info == null) {
      return new FailSafeMethodVisitor(Opcodes.API_VERSION, super.visitMethod(access, name, desc, signature, exceptions));
    }

    final boolean isStatic = isStatic(access);
    final Type[] args = Type.getArgumentTypes(desc);
    final NotNullInstructionTracker instrTracker = new NotNullInstructionTracker(cv.visitMethod(access, name, desc, signature, exceptions));
    return new FailSafeMethodVisitor(Opcodes.API_VERSION, instrTracker) {
      private Label myStartGeneratedCodeLabel;

      @Override
      public void visitCode() {
        for (Iterator<NotNullState> iterator = info.paramNullability.values().iterator(); iterator.hasNext(); ) {
          if (!iterator.next().isNotNull()) {
            iterator.remove();
          }
        }
        if (info.paramNullability.size() > 0) {
          myStartGeneratedCodeLabel = new Label();
          mv.visitLabel(myStartGeneratedCodeLabel);
        }
        for (Map.Entry<Integer, NotNullState> entry : info.paramNullability.entrySet()) {
          Integer param = entry.getKey();
          int var = isStatic ? 0 : 1;
          for (int i = 0; i < param; ++i) {
            var += args[i].getSize();
          }
          mv.visitVarInsn(ALOAD, var);

          Label end = new Label();
          mv.visitJumpInsn(IFNONNULL, end);

          NotNullState state = entry.getValue();
          String paramName = myMethodData.lookupParamName(name, desc, param);
          String descrPattern = state.getNullParamMessage(paramName);
          String[] args = state.message != null
                          ? EMPTY_STRING_ARRAY
                          : new String[]{paramName != null ? paramName : String.valueOf(param - info.paramAnnotationOffset), myMethodData.myClassName, name};
          reportError(state.exceptionType, end, descrPattern, args);
        }
      }

      @Override
      public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        boolean isParameterOrThisRef = isStatic ? index < args.length : index <= args.length;
        Label label = (isParameterOrThisRef && myStartGeneratedCodeLabel != null) ? myStartGeneratedCodeLabel : start;
        mv.visitLocalVariable(name, desc, signature, label, end, index);
      }

      @Override
      public void visitInsn(int opcode) {
        if (opcode == ARETURN && instrTracker.canBeNull() && info.nullability.isNotNull()) {
          mv.visitInsn(DUP);
          Label skipLabel = new Label();
          mv.visitJumpInsn(IFNONNULL, skipLabel);
          String descrPattern = info.nullability.getNullResultMessage();
          String[] args = info.nullability.message != null ? EMPTY_STRING_ARRAY : new String[]{myMethodData.myClassName, name};
          reportError(info.nullability.exceptionType, skipLabel, descrPattern, args);
        }

        mv.visitInsn(opcode);
      }

      private void reportError(String exceptionClass, Label end, String descrPattern, String[] args) {
        myAuxGenerator.reportError(mv, myMethodData.myClassName, exceptionClass, descrPattern, args);
        mv.visitLabel(end);
        myIsModification = true;
        processPostponedErrors();
      }

      @Override
      public void visitMaxs(int maxStack, int maxLocals) {
        try {
          super.visitMaxs(maxStack, maxLocals);
        }
        catch (Throwable e) {
          registerError(name, "visitMaxs", e);
        }
      }
    };
  }

  @Override
  public void visitEnd() {
    myAuxGenerator.generateReportingMethod(cv);
    super.visitEnd();
  }

  private static boolean isStatic(int access) {
    return (access & ACC_STATIC) != 0;
  }

  private static boolean isReferenceType(Type type) {
    return type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY;
  }

  private void registerError(String methodName, @SuppressWarnings("SameParameterValue") String operationName, Throwable t) {
    if (myPostponedError == null) {
      // throw the first error that occurred
      Throwable cause = t.getCause();
      if (cause != null) t = cause;

      String message = t.getMessage();

      StringWriter writer = new StringWriter();
      t.printStackTrace(new PrintWriter(writer));

      StringBuilder text = new StringBuilder();
      text.append("Operation '").append(operationName).append("' failed for ").append(myMethodData.myClassName).append(".").append(methodName).append("(): ");
      if (message != null) text.append(message);
      text.append('\n').append(writer.getBuffer());
      myPostponedError = new RuntimeException(text.toString(), cause);
    }
    if (myIsModification) {
      processPostponedErrors();
    }
  }

  private void processPostponedErrors() {
    RuntimeException error = myPostponedError;
    if (error != null) {
      throw error;
    }
  }

  private final class NotNullInstructionTracker extends MethodVisitor {
    private boolean myCanBeNull = true; // initially assume the value can be null

    NotNullInstructionTracker(MethodVisitor delegate) {
      super(Opcodes.API_VERSION, delegate);
    }

    public boolean canBeNull() {
      return myCanBeNull;
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
      myCanBeNull = nextCanBeNullValue(opcode);
      super.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
      myCanBeNull = nextCanBeNullValue(opcode);
      super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
      myCanBeNull = nextCanBeNullValue(opcode);
      super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
      myCanBeNull = nextCanBeNullValue(opcode);
      super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
      myCanBeNull = nextCanBeNullValue(opcode, owner, name, descriptor); /*is not a constructor call*/
      super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
      myCanBeNull = nextCanBeNullValue(Opcodes.INVOKEDYNAMIC);
      super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
      myCanBeNull = nextCanBeNullValue(opcode);
      super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(Object value) {
      myCanBeNull = nextCanBeNullValue(Opcodes.LDC);
      super.visitLdcInsn(value);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
      myCanBeNull = nextCanBeNullValue(Opcodes.IINC);
      super.visitIincInsn(var, increment);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
      myCanBeNull = nextCanBeNullValue(Opcodes.TABLESWITCH);
      super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
      myCanBeNull = nextCanBeNullValue(Opcodes.LOOKUPSWITCH);
      super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
      myCanBeNull = nextCanBeNullValue(Opcodes.MULTIANEWARRAY);
      super.visitMultiANewArrayInsn(descriptor, numDimensions);
    }

    @Override
    public void visitInsn(int opcode) {
      myCanBeNull = nextCanBeNullValue(opcode);
      super.visitInsn(opcode);
    }

    private boolean nextCanBeNullValue(int nextMethodCallOpcode, String owner, String name, String descriptor) {
      if (nextMethodCallOpcode == Opcodes.INVOKESPECIAL && ("<init>".equals(name) || myMethodData.isAlwaysNotNull(owner, name, descriptor))) {
        // a constructor call or a NotNull marked own method
        return false;
      }
      if ((nextMethodCallOpcode == Opcodes.INVOKESTATIC || nextMethodCallOpcode == Opcodes.INVOKEVIRTUAL) &&
          myMethodData.isAlwaysNotNull(owner, name, descriptor)) {
        return false;
      }
      return true;
    }

    private boolean nextCanBeNullValue(int nextOpcode) {
      // if instruction guaranteed produces non-null stack value
      if (nextOpcode == Opcodes.LDC || nextOpcode == NEW ||
          nextOpcode == ANEWARRAY || nextOpcode == Opcodes.NEWARRAY || nextOpcode == Opcodes.MULTIANEWARRAY) {
        return false;
      }
      // for some instructions it is safe not to change previously calculated flag value
      if (nextOpcode == Opcodes.DUP || nextOpcode == Opcodes.DUP_X1 || nextOpcode == Opcodes.DUP_X2 ||
          nextOpcode == Opcodes.DUP2 || nextOpcode == Opcodes.DUP2_X1 || nextOpcode == Opcodes.DUP2_X2 ||
          nextOpcode == Opcodes.JSR || nextOpcode == Opcodes.GOTO || nextOpcode == Opcodes.NOP ||
          nextOpcode == Opcodes.RET || nextOpcode == Opcodes.CHECKCAST) {
        return myCanBeNull;
      }
      // by default assume nullable
      return true;
    }
  }
}