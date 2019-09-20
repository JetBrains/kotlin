// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.uiDesigner.compiler;

import com.intellij.compiler.instrumentation.FailSafeClassReader;
import com.intellij.compiler.instrumentation.FailSafeMethodVisitor;
import com.intellij.compiler.instrumentation.InstrumentationClassFinder;
import com.intellij.uiDesigner.UIFormXmlConstants;
import com.intellij.uiDesigner.lw.*;
import com.intellij.uiDesigner.shared.BorderType;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.*;
import org.jetbrains.org.objectweb.asm.commons.GeneratorAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.*;

/**
 * @author yole
 */
public class AsmCodeGenerator {
  private static final int ASM_API_VERSION = Opcodes.API_VERSION;
  private final LwRootContainer myRootContainer;
  private final InstrumentationClassFinder myFinder;
  private final List<FormErrorInfo> myErrors;
  private final List<FormErrorInfo> myWarnings;

  private final Map<String, Integer> myIdToLocalMap = new HashMap<String, Integer>();

  private static final String CONSTRUCTOR_NAME = "<init>";
  private String myClassToBind;
  private byte[] myPatchedData;

  private Method myGetFontMethod;

  private static final Map<String, LayoutCodeGenerator> myContainerLayoutCodeGenerators = new HashMap<String, LayoutCodeGenerator>();
  private static final Map<Class<? extends LwContainer>, LayoutCodeGenerator> myComponentLayoutCodeGenerators = new HashMap<Class<? extends LwContainer>, LayoutCodeGenerator>();
  private static final Map<String, PropertyCodeGenerator> myPropertyCodeGenerators = new LinkedHashMap<String, PropertyCodeGenerator>();  // need LinkedHashMap for deterministic iteration
  public static final String SETUP_METHOD_NAME = "$$$setupUI$$$";
  public static final String GET_ROOT_COMPONENT_METHOD_NAME = "$$$getRootComponent$$$";
  public static final String CREATE_COMPONENTS_METHOD_NAME = "createUIComponents";
  public static final String LOAD_LABEL_TEXT_METHOD = "$$$loadLabelText$$$";
  public static final String LOAD_BUTTON_TEXT_METHOD = "$$$loadButtonText$$$";
  public static final String GET_FONT_METHOD_NAME = "$$$getFont$$$";

  private static final Type ourButtonGroupType = Type.getType(ButtonGroup.class);
  private static final Type ourBorderFactoryType = Type.getType(BorderFactory.class);
  private static final Type ourBorderType = Type.getType(Border.class);
  private static final Method ourCreateTitledBorderMethod = Method.getMethod(
    "javax.swing.border.TitledBorder createTitledBorder(javax.swing.border.Border,java.lang.String,int,int,java.awt.Font,java.awt.Color)");

  private static final String ourBorderFactoryClientProperty = "BorderFactoryClass";

  private final NestedFormLoader myFormLoader;
  private final boolean myIgnoreCustomCreation;
  private final ClassWriter myClassWriter;

  static {
    myContainerLayoutCodeGenerators.put(UIFormXmlConstants.LAYOUT_INTELLIJ, new GridLayoutCodeGenerator());
    myContainerLayoutCodeGenerators.put(UIFormXmlConstants.LAYOUT_GRIDBAG, new GridBagLayoutCodeGenerator());
    myContainerLayoutCodeGenerators.put(UIFormXmlConstants.LAYOUT_BORDER, new SimpleLayoutCodeGenerator(Type.getType(BorderLayout.class)));
    myContainerLayoutCodeGenerators.put(UIFormXmlConstants.LAYOUT_CARD, new CardLayoutCodeGenerator());
    myContainerLayoutCodeGenerators.put(UIFormXmlConstants.LAYOUT_FLOW, new FlowLayoutCodeGenerator());

    myComponentLayoutCodeGenerators.put(LwSplitPane.class, new SplitPaneLayoutCodeGenerator());
    myComponentLayoutCodeGenerators.put(LwTabbedPane.class, new TabbedPaneLayoutCodeGenerator());
    myComponentLayoutCodeGenerators.put(LwScrollPane.class, new ScrollPaneLayoutCodeGenerator());
    myComponentLayoutCodeGenerators.put(LwToolBar.class, new ToolBarLayoutCodeGenerator());

    myPropertyCodeGenerators.put(String.class.getName(), new StringPropertyCodeGenerator());
    myPropertyCodeGenerators.put(Dimension.class.getName(), new DimensionPropertyCodeGenerator());
    myPropertyCodeGenerators.put(Insets.class.getName(), new InsetsPropertyCodeGenerator());
    myPropertyCodeGenerators.put(Rectangle.class.getName(), new RectanglePropertyCodeGenerator());
    myPropertyCodeGenerators.put(Color.class.getName(), new ColorPropertyCodeGenerator());
    myPropertyCodeGenerators.put(Font.class.getName(), new FontPropertyCodeGenerator());
    myPropertyCodeGenerators.put(Icon.class.getName(), new IconPropertyCodeGenerator());
    myPropertyCodeGenerators.put(ListModel.class.getName(), new ListModelPropertyCodeGenerator(DefaultListModel.class));
    myPropertyCodeGenerators.put(ComboBoxModel.class.getName(), new ListModelPropertyCodeGenerator(DefaultComboBoxModel.class));
    myPropertyCodeGenerators.put("java.lang.Enum", new EnumPropertyCodeGenerator());
  }

  public AsmCodeGenerator(LwRootContainer rootContainer,
                          InstrumentationClassFinder finder,
                          NestedFormLoader formLoader,
                          final boolean ignoreCustomCreation,
                          final ClassWriter classWriter) {
    myFormLoader = formLoader;
    myIgnoreCustomCreation = ignoreCustomCreation;
    if (finder == null){
      throw new IllegalArgumentException("loader cannot be null");
    }
    if (rootContainer == null){
      throw new IllegalArgumentException("rootContainer cannot be null");
    }
    myRootContainer = rootContainer;
    myFinder = finder;

    myErrors = new ArrayList<FormErrorInfo>();
    myWarnings = new ArrayList<FormErrorInfo>();
    myClassWriter = classWriter;
  }

  public void patchFile(final File classFile) {
    if (!classFile.exists()) {
      myErrors.add(new FormErrorInfo(null, "Class to bind does not exist: " + myRootContainer.getClassToBind()));
      return;
    }

    FileInputStream fis;
    try {
      byte[] patchedData;
      fis = new FileInputStream(classFile);
      try {
        patchedData = patchClass(fis);
        if (patchedData == null) {
          return;
        }
      }
      finally {
        fis.close();
      }

      FileOutputStream fos = new FileOutputStream(classFile);
      try {
        fos.write(patchedData);
      }
      finally {
        fos.close();
      }
    }
    catch (IOException e) {
      myErrors.add(new FormErrorInfo(null, "Cannot read or write class file " + classFile.getPath() + ": " + e.toString()));
    }
    catch(IllegalStateException e) {
      myErrors.add(new FormErrorInfo(null, "Unexpected data in form file when patching class " + classFile.getPath() + ": " + e.toString()));
    }
  }

  public byte[] patchClass(InputStream classStream) {
    try {
      final ClassReader reader = new FailSafeClassReader(classStream);
      return patchClass(reader);
    }
    catch (IOException e) {
      myErrors.add(new FormErrorInfo(null, "Error reading class data stream"));
      return null;
    }
  }

  public byte[] patchClass(ClassReader reader) {
    myClassToBind = myRootContainer.getClassToBind();
    if (myClassToBind == null){
      myWarnings.add(new FormErrorInfo(null, "No class to bind specified"));
      return null;
    }

    if (myRootContainer.getComponentCount() != 1) {
      myErrors.add(new FormErrorInfo(null, "There should be only one component at the top level"));
      return null;
    }

    String nonEmptyPanel = Utils.findNotEmptyPanelWithXYLayout(myRootContainer.getComponent(0));
    if (nonEmptyPanel != null) {
      myErrors.add(new FormErrorInfo(nonEmptyPanel,
                                     "There are non empty panels with XY layout. Please lay them out in a grid."));
      return null;
    }

    FirstPassClassVisitor visitor = new FirstPassClassVisitor();
    reader.accept(visitor, 0);

    reader.accept(new FormClassVisitor(myClassWriter, visitor.isExplicitSetupCall()), 0);
    myPatchedData = myClassWriter.toByteArray();
    return myPatchedData;
  }

  public FormErrorInfo[] getErrors() {
    return myErrors.toArray(new FormErrorInfo[0]);
  }

  public FormErrorInfo[] getWarnings() {
    return myWarnings.toArray(new FormErrorInfo[0]);
  }

  public byte[] getPatchedData() {
    return myPatchedData;
  }

  static void pushPropValue(GeneratorAdapter generator, String propertyClass, Object value) {
    PropertyCodeGenerator codeGen = myPropertyCodeGenerators.get(propertyClass);
    if (codeGen == null) {
      throw new RuntimeException("Unknown property class " + propertyClass);
    }
    codeGen.generatePushValue(generator, value);
  }

  static InstrumentationClassFinder.PseudoClass getComponentClass(String className, final InstrumentationClassFinder finder) throws CodeGenerationException {
    try {
      return finder.loadClass(className);
    }
    catch (ClassNotFoundException e) {
      throw new CodeGenerationException(null, "Class not found: " + className);
    }
    catch(UnsupportedClassVersionError e) {
      throw new CodeGenerationException(null, "Unsupported class version error: " + className);
    }
    catch (IOException e) {
      throw new CodeGenerationException(null, e.getMessage(), e);
    }
  }

  public static Type typeFromClassName(final String className) {
    return Type.getType("L" + className.replace('.', '/') + ";");
  }

  class FormClassVisitor extends ClassVisitor implements GetFontMethodProvider {
    private String myClassName;
    private String mySuperName;
    private final Map<String, String> myFieldDescMap = new HashMap<String, String>();
    private final Map<String, Integer> myFieldAccessMap = new HashMap<String, Integer>();
    private boolean myHaveCreateComponentsMethod = false;
    private int myCreateComponentsAccess;
    private final boolean myExplicitSetupCall;

    FormClassVisitor(final ClassVisitor cv, final boolean explicitSetupCall) {
      super(ASM_API_VERSION, cv);
      myExplicitSetupCall = explicitSetupCall;
    }

    @Override
    public void visit(final int version,
                      final int access,
                      final String name,
                      final String signature,
                      final String superName,
                      final String[] interfaces) {
      super.visit(version, access, name, signature, superName, interfaces);
      myClassName = name;
      mySuperName = superName;

      for (PropertyCodeGenerator propertyCodeGenerator : myPropertyCodeGenerators.values()) {
        propertyCodeGenerator.generateClassStart(this, name, myFinder);
      }
    }

    public String getClassName() {
      return myClassName;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {

      if (name.equals(SETUP_METHOD_NAME) || name.equals(GET_ROOT_COMPONENT_METHOD_NAME) ||
          name.equals(LOAD_BUTTON_TEXT_METHOD) || name.equals(LOAD_LABEL_TEXT_METHOD)) {
        return null;
      }
      if (name.equals(CREATE_COMPONENTS_METHOD_NAME) && desc.equals("()V")) {
        myHaveCreateComponentsMethod = true;
        myCreateComponentsAccess = access;
      }

      final MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
      if (name.equals(CONSTRUCTOR_NAME) && !myExplicitSetupCall) {
        return new FormConstructorVisitor(methodVisitor, myClassName, mySuperName);
      }
      return methodVisitor != null? new FailSafeMethodVisitor(ASM_API_VERSION, methodVisitor) : null;
    }

    MethodVisitor visitNewMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
      final MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
      return methodVisitor != null? new FailSafeMethodVisitor(ASM_API_VERSION, methodVisitor) : null;
    }

    @Override
    public FieldVisitor visitField(final int access, final String name, final String desc, final String signature, final Object value) {
      myFieldDescMap.put(name, desc);
      myFieldAccessMap.put(name, new Integer(access));
      return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public void visitEnd() {
      final boolean haveCustomCreateComponents = Utils.getCustomCreateComponentCount(myRootContainer) > 0 &&
                                                 !myIgnoreCustomCreation;
      if (haveCustomCreateComponents && !myHaveCreateComponentsMethod) {
        myErrors.add(new FormErrorInfo(null, "Form contains components with Custom Create option but no createUIComponents() method"));
      }

      Method method = Method.getMethod("void " + SETUP_METHOD_NAME + " ()");
      GeneratorAdapter generator = new GeneratorAdapter(Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC, method, null, null, cv);
      if (haveCustomCreateComponents && myHaveCreateComponentsMethod) {
        generator.visitVarInsn(Opcodes.ALOAD, 0);
        int opcode = myCreateComponentsAccess == Opcodes.ACC_PRIVATE ? Opcodes.INVOKESPECIAL : Opcodes.INVOKEVIRTUAL;
        generator.visitMethodInsn(opcode, myClassName, CREATE_COMPONENTS_METHOD_NAME, "()V", false);
      }
      buildSetupMethod(generator);

      final String rootBinding = myRootContainer.getComponent(0).getBinding();
      if (rootBinding != null && myFieldDescMap.containsKey(rootBinding)) {
        buildGetRootComponenMethod();
      }

      if (myGetFontMethod != null) {
        FontPropertyCodeGenerator
          .buildGetFontMethod(new GeneratorAdapter(Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC, myGetFontMethod, null, null, cv));
      }

      for (PropertyCodeGenerator propertyCodeGenerator : myPropertyCodeGenerators.values()) {
        propertyCodeGenerator.generateClassEnd(this);
      }

      super.visitEnd();
    }

    private void buildGetRootComponenMethod() {
      final Type componentType = Type.getType(JComponent.class);
      final Method method = new Method(GET_ROOT_COMPONENT_METHOD_NAME, componentType, new Type[0]);
      GeneratorAdapter generator = new GeneratorAdapter(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, method, null, null, cv);

      final LwComponent topComponent = (LwComponent)myRootContainer.getComponent(0);
      final String binding = topComponent.getBinding();

      generator.loadThis();
      generator.getField(typeFromClassName(myClassName), binding,
                         Type.getType(myFieldDescMap.get(binding)));
      generator.returnValue();
      generator.endMethod();
    }

    private void buildSetupMethod(final GeneratorAdapter generator) {
      try {
        final LwComponent topComponent = (LwComponent)myRootContainer.getComponent(0);
        generateSetupCodeForComponent(topComponent, generator, -1);
        generateComponentReferenceProperties(topComponent, generator);
        generateButtonGroups(myRootContainer, generator);
      }
      catch (CodeGenerationException e) {
        myErrors.add(new FormErrorInfo(e.getComponentId(), e.getMessage()));
      }
      generator.returnValue();
      generator.endMethod();
    }

    private void generateSetupCodeForComponent(final LwComponent lwComponent,
                                               final GeneratorAdapter generator,
                                               final int parentLocal) throws CodeGenerationException {

      String className;
      if (lwComponent instanceof LwNestedForm) {
        LwRootContainer nestedFormContainer;
        LwNestedForm nestedForm = (LwNestedForm) lwComponent;
        if (myFormLoader == null) {
          throw new CodeGenerationException(null, "Attempt to compile nested form with no nested form loader specified");
        }
        try {
          nestedFormContainer = myFormLoader.loadForm(nestedForm.getFormFileName());
        }
        catch (Exception e) {
          throw new CodeGenerationException(lwComponent.getId(), e.getMessage());
        }
        // if nested form is empty, ignore
        if (nestedFormContainer.getComponentCount() == 0) {
          return;
        }
        if (nestedFormContainer.getComponent(0).getBinding() == null) {
          throw new CodeGenerationException(lwComponent.getId(), "No binding on root component of nested form " + nestedForm.getFormFileName());
        }
        try {
          Utils.validateNestedFormLoop(nestedForm.getFormFileName(), myFormLoader);
        }
        catch (RecursiveFormNestingException e) {
          throw new CodeGenerationException(lwComponent.getId(), "Recursive form nesting is not allowed");
        }
        className = myFormLoader.getClassToBindName(nestedFormContainer);
      }
      else {
        className = getComponentCodeGenerator(lwComponent.getParent()).mapComponentClass(lwComponent.getComponentClassName());
      }
      Type componentType = typeFromClassName(className);
      int componentLocal = generator.newLocal(componentType);

      myIdToLocalMap.put(lwComponent.getId(), new Integer(componentLocal));

      InstrumentationClassFinder.PseudoClass componentClass = getComponentClass(className, myFinder);
      validateFieldBinding(lwComponent, componentClass);

      if (myIgnoreCustomCreation) {
        try {
          boolean creatable = true;
          if ((componentClass.getModifiers() & (Modifier.PRIVATE | Modifier.ABSTRACT)) != 0) {
            creatable = false;
          }
          else {
            if (!componentClass.hasDefaultPublicConstructor()) {
              creatable = false;
            }
          }
          if (!creatable) {
            componentClass = Utils.suggestReplacementClass(componentClass);
            componentType = Type.getType(componentClass.getDescriptor());
          }
        }
        catch (ClassNotFoundException e) {
          throw new CodeGenerationException(lwComponent.getId(), e.getMessage(), e);
        }
        catch (IOException e) {
          throw new CodeGenerationException(lwComponent.getId(), e.getMessage(), e);
        }
      }

      if (!lwComponent.isCustomCreate() || myIgnoreCustomCreation) {
        generator.newInstance(componentType);
        generator.dup();
        generator.invokeConstructor(componentType, Method.getMethod("void <init>()"));
        generator.storeLocal(componentLocal);

        generateFieldBinding(lwComponent, generator, componentLocal);
      }
      else {
        final String binding = lwComponent.getBinding();
        if (binding == null) {
          throw new CodeGenerationException(lwComponent.getId(),
                                            "Only components bound to fields can have custom creation code");
        }
        generator.loadThis();
        generator.getField(getMainClassType(), binding, Type.getType(myFieldDescMap.get(binding)));
        generator.storeLocal(componentLocal);
      }

      if (lwComponent instanceof LwContainer) {
        LwContainer lwContainer = (LwContainer) lwComponent;
        if (!lwContainer.isCustomCreate() || lwContainer.getComponentCount() > 0) {
          getComponentCodeGenerator(lwContainer).generateContainerLayout(lwContainer, generator, componentLocal);
        }
      }

      generateComponentProperties(lwComponent, componentClass, generator, componentLocal);

      // add component to parent
      if (!(lwComponent.getParent() instanceof LwRootContainer)) {
        final LayoutCodeGenerator parentCodeGenerator = getComponentCodeGenerator(lwComponent.getParent());
        if (lwComponent instanceof LwNestedForm) {
          componentLocal = getNestedFormComponent(generator, componentClass, componentLocal);
        }
        parentCodeGenerator.generateComponentLayout(lwComponent, generator, componentLocal, parentLocal);
      }

      if (lwComponent instanceof LwContainer) {
        LwContainer container = (LwContainer) lwComponent;

        generateBorder(container, generator, componentLocal);

        for(int i=0; i<container.getComponentCount(); i++) {
          generateSetupCodeForComponent((LwComponent) container.getComponent(i), generator, componentLocal);
        }
      }
    }

    private int getNestedFormComponent(GeneratorAdapter generator, InstrumentationClassFinder.PseudoClass componentClass, int formLocal) {
      final Type componentType = Type.getType(JComponent.class);
      int componentLocal = generator.newLocal(componentType);
      generator.loadLocal(formLocal);
      generator.invokeVirtual(Type.getType(componentClass.getDescriptor()), new Method(GET_ROOT_COMPONENT_METHOD_NAME, componentType, new Type[0]));
      generator.storeLocal(componentLocal);
      return componentLocal;
    }

    private LayoutCodeGenerator getComponentCodeGenerator(final LwContainer container) {
      LayoutCodeGenerator generator = myComponentLayoutCodeGenerators.get(container.getClass());
      if (generator != null) {
        return generator;
      }
      LwContainer parent = container;
      while(parent != null) {
        final String layoutManager = parent.getLayoutManager();
        if (layoutManager != null && !layoutManager.isEmpty()) {
          if (layoutManager.equals(UIFormXmlConstants.LAYOUT_FORM) &&
              !myContainerLayoutCodeGenerators.containsKey(UIFormXmlConstants.LAYOUT_FORM)) {
            myContainerLayoutCodeGenerators.put(UIFormXmlConstants.LAYOUT_FORM, new FormLayoutCodeGenerator());
          }
          generator = myContainerLayoutCodeGenerators.get(layoutManager);
          if (generator != null) {
            return generator;
          }
        }
        parent = parent.getParent();
      }
      return GridLayoutCodeGenerator.INSTANCE;
    }

    private void generateComponentProperties(final LwComponent lwComponent,
                                             final InstrumentationClassFinder.PseudoClass componentClass,
                                             final GeneratorAdapter generator,
                                             final int componentLocal) throws CodeGenerationException {
      // introspected properties
      final LwIntrospectedProperty[] introspectedProperties = lwComponent.getAssignedIntrospectedProperties();
      for (final LwIntrospectedProperty property : introspectedProperties) {
        if (property instanceof LwIntroComponentProperty) {
          continue;
        }
        final String propertyClass = property.getCodeGenPropertyClassName();
        if (myIgnoreCustomCreation) {
          try {
            final String descriptor;
            // convert wrapper classes to primitive
            if (propertyClass.equals(Integer.class.getName())) {
              descriptor = "(I)V";
            }
            else if (propertyClass.equals(Boolean.class.getName())) {
              descriptor = "(Z)V";
            }
            else if (propertyClass.equals(Double.class.getName())) {
              descriptor = "(D)V";
            }
            else if (propertyClass.equals(Float.class.getName())) {
              descriptor = "(F)V";
            }
            else if (propertyClass.equals(Long.class.getName())) {
              descriptor = "(L)V";
            }
            else if (propertyClass.equals(Byte.class.getName())) {
              descriptor = "(B)V";
            }
            else if (propertyClass.equals(Short.class.getName())) {
              descriptor = "(S)V";
            }
            else if (propertyClass.equals(Character.class.getName())) {
              descriptor = "(C)V";
            }
            else {
              descriptor = "(L" + Class.forName(propertyClass).getName().replace('.', '/') + ";)V";
            }
            final InstrumentationClassFinder.PseudoMethod setter = componentClass.findMethodInHierarchy(property.getWriteMethodName(),
                                                                                                        descriptor);
            if (setter == null) {
              continue;
            }
          }
          catch (Exception e) {
            continue;
          }
        }
        final PropertyCodeGenerator propGen = myPropertyCodeGenerators.get(propertyClass);

        try {
          if (propGen != null &&
              propGen.generateCustomSetValue(lwComponent, componentClass, property, generator, this, componentLocal, myClassName)) {
            continue;
          }
        }
        catch (IOException e) {
          throw new CodeGenerationException(lwComponent.getId(), e.getMessage(), e);
        }
        catch (ClassNotFoundException e) {
          throw new CodeGenerationException(lwComponent.getId(), e.getMessage(), e);
        }

        generator.loadLocal(componentLocal);

        Object value = lwComponent.getPropertyValue(property);
        Type setterArgType;
        if (propertyClass.equals(Integer.class.getName())) {
          generator.push(((Integer)value).intValue());
          setterArgType = Type.INT_TYPE;
        }
        else if (propertyClass.equals(Boolean.class.getName())) {
          generator.push(((Boolean)value).booleanValue());
          setterArgType = Type.BOOLEAN_TYPE;
        }
        else if (propertyClass.equals(Double.class.getName())) {
          generator.push(((Double)value).doubleValue());
          setterArgType = Type.DOUBLE_TYPE;
        }
        else if (propertyClass.equals(Float.class.getName())) {
          generator.push(((Float)value).floatValue());
          setterArgType = Type.FLOAT_TYPE;
        }
        else if (propertyClass.equals(Long.class.getName())) {
          generator.push(((Long)value).longValue());
          setterArgType = Type.LONG_TYPE;
        }
        else if (propertyClass.equals(Short.class.getName())) {
          generator.push(((Short)value).intValue());
          setterArgType = Type.SHORT_TYPE;
        }
        else if (propertyClass.equals(Byte.class.getName())) {
          generator.push(((Byte)value).intValue());
          setterArgType = Type.BYTE_TYPE;
        }
        else if (propertyClass.equals(Character.class.getName())) {
          generator.push(((Character)value).charValue());
          setterArgType = Type.CHAR_TYPE;
        }
        else {
          if (propGen == null) {
            continue;
          }
          propGen.generatePushValue(generator, value);
          setterArgType = typeFromClassName(property.getPropertyClassName());
        }

        Type declaringType = (property.getDeclaringClassName() != null)
                             ? typeFromClassName(property.getDeclaringClassName())
                             : Type.getType(componentClass.getDescriptor());
        generator.invokeVirtual(declaringType, new Method(property.getWriteMethodName(),
                                                          Type.VOID_TYPE, new Type[]{setterArgType}));
      }

      generateClientProperties(lwComponent, componentClass, generator, componentLocal);
    }

    private void generateClientProperties(final LwComponent lwComponent,
                                          final InstrumentationClassFinder.PseudoClass componentClass,
                                          final GeneratorAdapter generator,
                                          final int componentLocal) throws CodeGenerationException {
      for (Object o : lwComponent.getDelegeeClientProperties().entrySet()) {
        Map.Entry e = (Map.Entry)o;
        generator.loadLocal(componentLocal);

        generator.push((String)e.getKey());

        Object value = e.getValue();
        if (value instanceof StringDescriptor) {
          generator.push(((StringDescriptor)value).getValue());
        }
        else if (value instanceof Boolean) {
          boolean boolValue = ((Boolean)value).booleanValue();
          Type booleanType = Type.getType(Boolean.class);
          if (boolValue) {
            generator.getStatic(booleanType, "TRUE", booleanType);
          }
          else {
            generator.getStatic(booleanType, "FALSE", booleanType);
          }
        }
        else {
          Type valueType = Type.getType(value.getClass());
          generator.newInstance(valueType);
          generator.dup();
          if (value instanceof Integer) {
            generator.push(((Integer)value).intValue());
            generator.invokeConstructor(valueType, Method.getMethod("void <init>(int)"));
          }
          else if (value instanceof Double) {
            generator.push(((Double)value).doubleValue());
            generator.invokeConstructor(valueType, Method.getMethod("void <init>(double)"));
          }
          else {
            throw new CodeGenerationException(lwComponent.getId(), "Unknown client property value type");
          }
        }

        Type componentType = Type.getType(componentClass.getDescriptor());
        Type objectType = Type.getType(Object.class);
        generator.invokeVirtual(componentType, new Method("putClientProperty",
                                                          Type.VOID_TYPE, new Type[]{objectType, objectType}));
      }
    }

    private void generateComponentReferenceProperties(final LwComponent component,
                                                      final GeneratorAdapter generator) throws CodeGenerationException {
      if (component instanceof LwNestedForm) return;
      int componentLocal = myIdToLocalMap.get(component.getId()).intValue();
      final LayoutCodeGenerator layoutCodeGenerator = getComponentCodeGenerator(component.getParent());
      InstrumentationClassFinder.PseudoClass componentClass = getComponentClass(layoutCodeGenerator.mapComponentClass(component.getComponentClassName()), myFinder);

      final LwIntrospectedProperty[] introspectedProperties = component.getAssignedIntrospectedProperties();
      for (final LwIntrospectedProperty property : introspectedProperties) {
        if (property instanceof LwIntroComponentProperty) {
          String targetId = (String)component.getPropertyValue(property);
          if (targetId != null && !targetId.isEmpty()) {
            // we may have a reference property pointing to a component which is
            // no longer valid
            final Integer targetLocalInt = myIdToLocalMap.get(targetId);
            if (targetLocalInt != null) {
              int targetLocal = targetLocalInt.intValue();
              generator.loadLocal(componentLocal);
              generator.loadLocal(targetLocal);
              Type declaringType = (property.getDeclaringClassName() != null)
                                   ? typeFromClassName(property.getDeclaringClassName())
                                   : Type.getType(componentClass.getDescriptor());
              generator.invokeVirtual(declaringType,
                                      new Method(property.getWriteMethodName(),
                                                 Type.VOID_TYPE, new Type[]{typeFromClassName(property.getPropertyClassName())}));
            }
          }
        }
      }

      if (component instanceof LwContainer) {
        LwContainer container = (LwContainer) component;

        for(int i=0; i<container.getComponentCount(); i++) {
          generateComponentReferenceProperties((LwComponent) container.getComponent(i), generator);
        }
      }
    }

    private void generateButtonGroups(final LwRootContainer rootContainer, final GeneratorAdapter generator) throws CodeGenerationException {
      IButtonGroup[] groups = rootContainer.getButtonGroups();
      if (groups.length > 0) {
        try {
          InstrumentationClassFinder.PseudoClass buttonGroupClass = null; // cached
          int groupLocal = generator.newLocal(ourButtonGroupType);
          for (IButtonGroup group : groups) {
            String[] ids = group.getComponentIds();

            if (ids.length > 0) {
              generator.newInstance(ourButtonGroupType);
              generator.dup();
              generator.invokeConstructor(ourButtonGroupType, Method.getMethod("void <init>()"));
              generator.storeLocal(groupLocal);

              if (group.isBound() && !myIgnoreCustomCreation) {
                if (buttonGroupClass == null) {
                  buttonGroupClass = myFinder.loadClass(ButtonGroup.class.getName());
                }
                validateFieldClass(group.getName(), buttonGroupClass, null);
                generator.loadThis();
                generator.loadLocal(groupLocal);
                generator.putField(getMainClassType(), group.getName(), ourButtonGroupType);
              }

              for (String id : ids) {
                Integer localInt = myIdToLocalMap.get(id);
                if (localInt != null) {
                  generator.loadLocal(groupLocal);
                  generator.loadLocal(localInt.intValue());
                  generator.invokeVirtual(ourButtonGroupType, Method.getMethod("void add(javax.swing.AbstractButton)"));
                }
              }
            }
          }
        }
        catch (IOException e) {
          throw new CodeGenerationException(rootContainer.getId(), e.getMessage(), e);
        }
        catch (ClassNotFoundException e) {
          throw new CodeGenerationException(rootContainer.getId(), e.getMessage(), e);
        }
      }
    }

    private void generateFieldBinding(final LwComponent lwComponent,
                                      final GeneratorAdapter generator,
                                      final int componentLocal) throws CodeGenerationException {
      final String binding = lwComponent.getBinding();
      if (binding != null) {
        Integer access = myFieldAccessMap.get(binding);
        if ((access.intValue() & Opcodes.ACC_STATIC) != 0) {
          throw new CodeGenerationException(lwComponent.getId(), "Cannot bind: field is static: " + myClassToBind + "." + binding);
        }
        if ((access.intValue() & Opcodes.ACC_FINAL) != 0) {
          throw new CodeGenerationException(lwComponent.getId(), "Cannot bind: field is final: " + myClassToBind + "." + binding);
        }

        generator.loadThis();
        generator.loadLocal(componentLocal);
        generator.putField(getMainClassType(), binding,
                           Type.getType(myFieldDescMap.get(binding)));
      }
    }

    @Override
    public Method getFontMethod() {
      if (myGetFontMethod == null) {
        myGetFontMethod = FontPropertyCodeGenerator.createGetFontMethod();
      }
      return myGetFontMethod;
    }

    @Override
    public Type getMainClassType() {
      return Type.getType("L" + myClassName + ";");
    }

    private void validateFieldBinding(LwComponent component, final InstrumentationClassFinder.PseudoClass componentClass) throws CodeGenerationException {
      String binding = component.getBinding();
      if (binding == null) return;

      validateFieldClass(binding, componentClass, component.getId());
    }

    private void validateFieldClass(String binding, InstrumentationClassFinder.PseudoClass componentClass, String componentId) throws CodeGenerationException {
      if (!myFieldDescMap.containsKey(binding)) {
        throw new CodeGenerationException(componentId, "Cannot bind: field does not exist: " + myClassToBind + "." + binding);
      }

      final Type fieldType = Type.getType(myFieldDescMap.get(binding));
      if (fieldType.getSort() != Type.OBJECT) {
        throw new CodeGenerationException(componentId, "Cannot bind: field is of primitive type: " + myClassToBind + "." + binding);
      }

      try {
        final InstrumentationClassFinder.PseudoClass fieldClass = myFinder.loadClass(fieldType.getClassName());
        if (!fieldClass.isAssignableFrom(componentClass)) {
          throw new CodeGenerationException(componentId, "Cannot bind: Incompatible types. Cannot assign " + componentClass.getName().replace('/', '.') + " to field " + myClassToBind + "." + binding);
        }
      }
      catch (ClassNotFoundException e) {
        throw new CodeGenerationException(componentId, "Class not found: " + fieldType.getClassName());
      }
      catch (IOException e) {
        throw new CodeGenerationException(componentId, e.getMessage(), e);
      }
    }

    private void generateBorder(final LwContainer container, final GeneratorAdapter generator, final int componentLocal) {
      final BorderType borderType = container.getBorderType();
      final StringDescriptor borderTitle = container.getBorderTitle();
      final String borderFactoryMethodName = borderType.getBorderFactoryMethodName();

      final boolean borderNone = borderType.equals(BorderType.NONE);
      if (!borderNone || borderTitle != null) {
        // object to invoke setBorder
        generator.loadLocal(componentLocal);

        if (!borderNone) {
          if (borderType.equals(BorderType.LINE)) {
            if (container.getBorderColor() == null) {
              Type colorType = Type.getType(Color.class);
              generator.getStatic(colorType, "black", colorType);
            }
            else {
              pushPropValue(generator, Color.class.getName(), container.getBorderColor());
            }
            generator.invokeStatic(ourBorderFactoryType,
                                   new Method(borderFactoryMethodName, ourBorderType,
                                              new Type[] { Type.getType(Color.class) } ));
          }
          else if (borderType.equals(BorderType.EMPTY) && container.getBorderSize() != null) {
            Insets size = container.getBorderSize();
            generator.push(size.top);
            generator.push(size.left);
            generator.push(size.bottom);
            generator.push(size.right);
            generator.invokeStatic(ourBorderFactoryType,
                                   new Method(borderFactoryMethodName, ourBorderType,
                                              new Type[] { Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE }));
          }
          else {
            generator.invokeStatic(ourBorderFactoryType,
                                   new Method(borderFactoryMethodName, ourBorderType, new Type[0]));
          }
        }
        else {
          generator.push((String) null);
        }
        pushBorderProperties(container, generator, borderTitle, componentLocal);


        Type borderFactoryType = ourBorderFactoryType;
        StringDescriptor borderFactoryValue = (StringDescriptor)container.getDelegeeClientProperties().get(ourBorderFactoryClientProperty);
        if (borderFactoryValue == null && borderTitle != null && Boolean.valueOf(System.getProperty("idea.is.internal")).booleanValue()) {
          borderFactoryValue = StringDescriptor.create("com.intellij.ui.IdeBorderFactory$PlainSmallWithIndent");
          container.getDelegeeClientProperties().put(ourBorderFactoryClientProperty, borderFactoryValue);
        }
        if (borderFactoryValue != null && !borderFactoryValue.getValue().isEmpty()) {
          borderFactoryType = typeFromClassName(borderFactoryValue.getValue());
        }

        generator.invokeStatic(borderFactoryType, ourCreateTitledBorderMethod);

        // set border
        generator.invokeVirtual(Type.getType(JComponent.class),
                                Method.getMethod("void setBorder(javax.swing.border.Border)"));
      }
    }

    private void pushBorderProperties(final LwContainer container, final GeneratorAdapter generator, final StringDescriptor borderTitle,
                                      final int componentLocal) {
      pushPropValue(generator, "java.lang.String", borderTitle);
      generator.push(container.getBorderTitleJustification());
      generator.push(container.getBorderTitlePosition());
      final FontDescriptor font = container.getBorderTitleFont();
      if (font == null) {
        generator.push((String) null);
      }
      else {
        FontPropertyCodeGenerator.generatePushFont(generator, this, componentLocal, container, font, "getFont", null);
      }
      if (container.getBorderTitleColor() == null) {
        generator.push((String) null);
      }
      else {
        pushPropValue(generator, Color.class.getName(), container.getBorderTitleColor());
      }
    }
  }

  private class FormConstructorVisitor extends FailSafeMethodVisitor {
    private final String myClassName;
    private final String mySuperName;
    private boolean callsSelfConstructor = false;
    private boolean mySetupCalled = false;
    private boolean mySuperCalled = false;

    FormConstructorVisitor(final MethodVisitor mv, final String className, final String superName) {
      super(ASM_API_VERSION, mv);
      myClassName = className;
      mySuperName = superName;
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
      if (opcode == Opcodes.GETFIELD && !mySetupCalled && !callsSelfConstructor && Utils.isBoundField(myRootContainer, name)) {
        callSetupUI();
      }
      super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc, boolean itf) {
      if (opcode == Opcodes.INVOKESPECIAL && name.equals(CONSTRUCTOR_NAME)) {
        if (owner.equals(myClassName)) {
          callsSelfConstructor = true;
        }
        else if (owner.equals(mySuperName)) {
          mySuperCalled = true;
        }
        else if (mySuperCalled) {
          callSetupUI();
        }
      }
      else if (mySuperCalled) {
        callSetupUI();
      }
      super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
      if (mySuperCalled) {
        callSetupUI();
      }
      super.visitJumpInsn(opcode, label);
    }

    private void callSetupUI() {
      if (!mySetupCalled) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, myClassName, SETUP_METHOD_NAME, "()V", false);
        mySetupCalled = true;
      }
    }

    @Override
    public void visitInsn(final int opcode) {
      if (opcode == Opcodes.RETURN && !mySetupCalled && !callsSelfConstructor) {
        callSetupUI();
      }
      super.visitInsn(opcode);
    }
  }

  private static class FirstPassClassVisitor extends ClassVisitor {
    private boolean myExplicitSetupCall = false;

    FirstPassClassVisitor() {
      super(ASM_API_VERSION, new ClassVisitor(ASM_API_VERSION) {});
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      if (name.equals(CONSTRUCTOR_NAME)) {
        return new FirstPassConstructorVisitor();
      }
      return null;
    }

    public boolean isExplicitSetupCall() {
      return myExplicitSetupCall;
    }

    private class FirstPassConstructorVisitor extends FailSafeMethodVisitor {
      FirstPassConstructorVisitor() {
        super(ASM_API_VERSION, new MethodVisitor(ASM_API_VERSION){});
      }

      @Override
      public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (name.equals(SETUP_METHOD_NAME)) {
          myExplicitSetupCall = true;
        }
      }
    }
  }
}
