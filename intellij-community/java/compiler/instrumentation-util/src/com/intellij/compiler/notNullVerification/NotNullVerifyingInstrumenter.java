// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.notNullVerification;

import com.intellij.compiler.instrumentation.FailSafeClassReader;
import com.intellij.compiler.instrumentation.FailSafeMethodVisitor;
import org.jetbrains.org.objectweb.asm.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
  private String myClassName;
  private boolean myIsModification = false;
  private RuntimeException myPostponedError;
  private final AuxiliaryMethodGenerator myAuxGenerator;
  private final Set<String> myNotNullAnnotations = new HashSet<String>();
  private boolean myEnum;
  private boolean myInner;

  private NotNullVerifyingInstrumenter(ClassVisitor classVisitor, ClassReader reader, String[] notNullAnnotations) {
    super(Opcodes.API_VERSION, classVisitor);
    for (String annotation : notNullAnnotations) {
      myNotNullAnnotations.add('L' + annotation.replace('.', '/') + ';');
    }
    myMethodData = collectMethodData(reader, myNotNullAnnotations);
    myAuxGenerator = new AuxiliaryMethodGenerator(reader);
  }

  public static boolean processClassFile(FailSafeClassReader reader, ClassVisitor writer, String[] notNullAnnotations) {
    NotNullVerifyingInstrumenter instrumenter = new NotNullVerifyingInstrumenter(writer, reader, notNullAnnotations);
    reader.accept(instrumenter, 0);
    return instrumenter.myIsModification;
  }

  private static final class MethodData {
    private String myClassName;
    final Map<String, Map<Integer, String>> paramNames = new LinkedHashMap<String, Map<Integer, String>>();
    final Set<String> alwaysNotNullMethods = new HashSet<String>(); // methods we are 100% sure return a non-null value

    public void setClassName(String className) {
      myClassName = className;
    }

    static String key(String methodName, String desc) {
      return methodName + desc;
    }

    String lookupParamName(String methodName, String desc, Integer num) {
      final Map<Integer, String> names = paramNames.get(key(methodName, desc));
      return names != null? names.get(num) : null;
    }

    void markNotNull(String methodName, String desc) {
      alwaysNotNullMethods.add(key(methodName, desc));
    }

    boolean isAlwaysNotNull(String className, String methodName, String desc) {
      return myClassName.equals(className) && alwaysNotNullMethods.contains(key(methodName, desc));
    }
  }

  private static MethodData collectMethodData(ClassReader reader, final Set<String> notNullAnnotations) {
    final MethodData result = new MethodData();
    reader.accept(new ClassVisitor(Opcodes.API_VERSION) {

      @Override
      public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        result.setClassName(name);
      }

      @Override
      public MethodVisitor visitMethod(int access, final String name, final String desc, String signature, String[] exceptions) {
        final Map<Integer, String> names = new LinkedHashMap<Integer, String>();
        result.paramNames.put(MethodData.key(name, desc), names);
        Type[] args = Type.getArgumentTypes(desc);
        final boolean shouldRegisterNotNull = isReferenceType(Type.getReturnType(desc)) &&
                                              (access & (Opcodes.ACC_FINAL | Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) != 0;

        final Map<Integer, Integer> paramSlots = new LinkedHashMap<Integer, Integer>(); // map: localVariableSlot -> methodParameterIndex
        int slotIndex = isStatic(access) ? 0 : 1;
        for (int paramIndex = 0; paramIndex < args.length; paramIndex++) {
          Type arg = args[paramIndex];
          paramSlots.put(slotIndex, paramIndex);
          slotIndex += arg.getSize();
        }

        return new MethodVisitor(api) {
          @Override
          public AnnotationVisitor visitAnnotation(String anno, boolean isRuntime) {
            if (shouldRegisterNotNull && notNullAnnotations.contains(anno)) {
              result.markNotNull(name, desc);
            }
            return super.visitAnnotation(anno, isRuntime);
          }

          @Override
          public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String anno, boolean visible) {
            if (shouldRegisterNotNull && new TypeReference(typeRef).getSort() == TypeReference.METHOD_RETURN && notNullAnnotations.contains(anno)) {
              result.markNotNull(name, desc);
            }
            return super.visitTypeAnnotation(typeRef, typePath, anno, visible);
          }

          @Override
          public void visitLocalVariable(String name2, String desc, String signature, Label start, Label end, int slotIndex) {
            Integer paramIndex = paramSlots.get(slotIndex);
            if (paramIndex != null) {
              names.put(paramIndex, name2);
            }
          }
        };
      }
    }, ClassReader.SKIP_FRAMES);
    return result;
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    myClassName = name;
    myEnum = (access & ACC_ENUM) != 0;
  }

  @Override
  public void visitInnerClass(String name, String outerName, String innerName, int access) {
    super.visitInnerClass(name, outerName, innerName, access);
    if (myClassName.equals(name)) {
      myInner = (access & ACC_STATIC) == 0;
    }
  }

  private static class NotNullState {
    String message;
    String exceptionType;
    final String notNullAnno;

    NotNullState(String notNullAnno, String exceptionType) {
      this.notNullAnno = notNullAnno;
      this.exceptionType = exceptionType;
    }

    String getNullParamMessage(String paramName) {
      if (message != null) return message;
      String shortName = getAnnoShortName();
      if (paramName != null) return "Argument for @" + shortName + " parameter '%s' of %s.%s must not be null";
      return "Argument %s for @" + shortName + " parameter of %s.%s must not be null";
    }

    String getNullResultMessage() {
      if (message != null) return message;
      String shortName = getAnnoShortName();
      return "@" + shortName + " method %s.%s must not return null";
    }

    private String getAnnoShortName() {
      String fullName = notNullAnno.substring(1, notNullAnno.length() - 1); // "Lpk/name;" -> "pk/name"
      return fullName.substring(fullName.lastIndexOf('/') + 1);
    }
  }

  @Override
  public MethodVisitor visitMethod(int access, final String name, final String desc, String signature, String[] exceptions) {
    if ((access & Opcodes.ACC_BRIDGE) != 0) {
      return new FailSafeMethodVisitor(Opcodes.API_VERSION, super.visitMethod(access, name, desc, signature, exceptions));
    }

    final boolean isStatic = isStatic(access);
    final Type[] args = Type.getArgumentTypes(desc);
    final int paramAnnotationOffset = !"<init>".equals(name) ? 0 : myEnum ? 2 : myInner ? 1 : 0;

    final Type returnType = Type.getReturnType(desc);
    final NotNullInstructionTracker instrTracker = new NotNullInstructionTracker(cv.visitMethod(access, name, desc, signature, exceptions));
    return new FailSafeMethodVisitor(Opcodes.API_VERSION, instrTracker) {
      private final Map<Integer, NotNullState> myNotNullParams = new LinkedHashMap<Integer, NotNullState>();
      private int myParamAnnotationOffset = paramAnnotationOffset;
      private NotNullState myMethodNotNull;
      private Label myStartGeneratedCodeLabel;

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

      @Override
      public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        AnnotationVisitor base = mv.visitTypeAnnotation(typeRef, typePath, desc, visible);
        if (typePath != null) return base;

        TypeReference ref = new TypeReference(typeRef);
        if (ref.getSort() == TypeReference.METHOD_RETURN) {
          return checkNotNullMethod(desc, base);
        }
        if (ref.getSort() == TypeReference.METHOD_FORMAL_PARAMETER) {
          return checkNotNullParameter(ref.getFormalParameterIndex() + paramAnnotationOffset, desc, base);
        }
        return base;
      }

      @Override
      public void visitAnnotableParameterCount(int parameterCount, boolean visible) {
        if (myParamAnnotationOffset != 0 && parameterCount == args.length) {
          myParamAnnotationOffset = 0;
        }
        super.visitAnnotableParameterCount(parameterCount, visible);
      }

      @Override
      public AnnotationVisitor visitParameterAnnotation(int parameter, String anno, boolean visible) {
        AnnotationVisitor base = mv.visitParameterAnnotation(parameter, anno, visible);
        return checkNotNullParameter(parameter + myParamAnnotationOffset, anno, base);
      }

      private AnnotationVisitor checkNotNullParameter(int parameter, String anno, AnnotationVisitor av) {
        if (parameter >= 0 && parameter < args.length && isReferenceType(args[parameter]) && myNotNullAnnotations.contains(anno)) {
          NotNullState state = new NotNullState(anno, IAE_CLASS_NAME);
          myNotNullParams.put(parameter, state);
          return collectNotNullArgs(av, state);
        }

        return av;
      }

      @Override
      public AnnotationVisitor visitAnnotation(String anno, boolean isRuntime) {
        return checkNotNullMethod(anno, mv.visitAnnotation(anno, isRuntime));
      }

      private AnnotationVisitor checkNotNullMethod(String anno, AnnotationVisitor base) {
        if (isReferenceType(returnType) && myNotNullAnnotations.contains(anno)) {
          myMethodNotNull = new NotNullState(anno, ISE_CLASS_NAME);
          return collectNotNullArgs(base, myMethodNotNull);
        }
        return base;
      }

      @Override
      public void visitCode() {
        if (myNotNullParams.size() > 0) {
          myStartGeneratedCodeLabel = new Label();
          mv.visitLabel(myStartGeneratedCodeLabel);
        }
        for (Map.Entry<Integer, NotNullState> entry : myNotNullParams.entrySet()) {
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
                          : new String[]{paramName != null ? paramName : String.valueOf(param - paramAnnotationOffset), myClassName, name};
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
        if (opcode == ARETURN && myMethodNotNull != null && instrTracker.canBeNull()) {
          mv.visitInsn(DUP);
          Label skipLabel = new Label();
          mv.visitJumpInsn(IFNONNULL, skipLabel);
          String descrPattern = myMethodNotNull.getNullResultMessage();
          String[] args = myMethodNotNull.message != null ? EMPTY_STRING_ARRAY : new String[]{myClassName, name};
          reportError(myMethodNotNull.exceptionType, skipLabel, descrPattern, args);
        }

        mv.visitInsn(opcode);
      }

      private void reportError(String exceptionClass, Label end, String descrPattern, String[] args) {
        myAuxGenerator.reportError(mv, myClassName, exceptionClass, descrPattern, args);
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
      text.append("Operation '").append(operationName).append("' failed for ").append(myClassName).append(".").append(methodName).append("(): ");
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