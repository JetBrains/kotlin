// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ant;

import com.intellij.compiler.instrumentation.FailSafeClassReader;
import com.intellij.compiler.instrumentation.InstrumentationClassFinder;
import com.intellij.compiler.instrumentation.InstrumenterClassWriter;
import com.intellij.compiler.notNullVerification.NotNullVerifyingInstrumenter;
import com.intellij.uiDesigner.compiler.*;
import com.intellij.uiDesigner.lw.CompiledClassPropertiesProvider;
import com.intellij.uiDesigner.lw.LwRootContainer;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.regexp.Regexp;
import org.jetbrains.org.objectweb.asm.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class Javac2 extends Javac {
  public static final String PROPERTY_INSTRUMENTATION_INCLUDE_JAVA_RUNTIME = "javac2.instrumentation.includeJavaRuntime";
  private ArrayList<File> myFormFiles;
  private List<PrefixedPath> myNestedFormPathList;
  private boolean instrumentNotNull = true;
  private String myNotNullAnnotations = "org.jetbrains.annotations.NotNull";
  private final List<Regexp> myClassFilterAnnotationRegexpList = new ArrayList<Regexp>(0);

  public Javac2() {
  }

  /**
   * Check if Java classes should be actually compiled by the task. This method is overridden by
   * {@link com.intellij.ant.InstrumentIdeaExtensions} task in order to suppress actual compilation
   * of the java sources.
   *
   * @return true if the java classes are compiled, false if just instrumentation is performed.
   */
  protected boolean areJavaClassesCompiled() {
    return true;
  }

  /**
   * This method is called when option that supported only for the case when java sources are compiled
   * and it is not supported for the case when only instrumentation is performed.
   *
   * @param optionName the option name to warn about.
   */
  private void unsupportedOptionMessage(final String optionName) {
    if (!areJavaClassesCompiled()) {
      log("The option " + optionName + " is not supported by InstrumentIdeaExtensions task", Project.MSG_ERR);
    }
  }

  public boolean getInstrumentNotNull() {
    return instrumentNotNull;
  }

  public void setInstrumentNotNull(boolean instrumentNotNull) {
    this.instrumentNotNull = instrumentNotNull;
  }

  /**
   * @return semicolon-separated names of not-null annotations to be instrumented. Example: {@code "org.jetbrains.annotations.NotNull;javax.annotation.Nonnull"}
   */
  public String getNotNullAnnotations() {
    return myNotNullAnnotations;
  }

  /**
   * @param notNullAnnotations semicolon-separated names of not-null annotations to be instrumented. Example: {@code "org.jetbrains.annotations.NotNull;javax.annotation.Nonnull"}
   */
  public void setNotNullAnnotations(String notNullAnnotations) {
    myNotNullAnnotations = notNullAnnotations;
  }

  /**
   * Allows to specify patterns of annotation class names to skip NotNull instrumentation on classes which have at least one
   * annotation matching at least one of the given patterns
   *
   * @param regexp the regular expression for JVM internal name (slash-separated) of annotations
   */
  public void add(final ClassFilterAnnotationRegexp regexp) {
    myClassFilterAnnotationRegexpList.add(regexp.getRegexp(getProject()));
  }

  /**
   * The overridden setter method that warns about unsupported option.
   *
   * @param v the option value
   */
  @Override
  public void setDebugLevel(String v) {
    unsupportedOptionMessage("debugLevel");
    super.setDebugLevel(v);
  }

  /**
   * The overridden setter method that warns about unsupported option.
   *
   * @param list the option value
   */
  @Override
  public void setListfiles(boolean list) {
    unsupportedOptionMessage("listFiles");
    super.setListfiles(list);
  }

  /**
   * The overridden setter method that warns about unsupported option.
   *
   * @param memoryInitialSize the option value
   */
  @Override
  public void setMemoryInitialSize(String memoryInitialSize) {
    unsupportedOptionMessage("memoryInitialSize");
    super.setMemoryInitialSize(memoryInitialSize);
  }

  /**
   * The overridden setter method that warns about unsupported option.
   *
   * @param memoryMaximumSize the option value
   */
  @Override
  public void setMemoryMaximumSize(String memoryMaximumSize) {
    unsupportedOptionMessage("memoryMaximumSize");
    super.setMemoryMaximumSize(memoryMaximumSize);
  }

  /**
   * The overridden setter method that warns about unsupported option.
   *
   * @param encoding the option value
   */
  @Override
  public void setEncoding(String encoding) {
    unsupportedOptionMessage("encoding");
    super.setEncoding(encoding);
  }

  /**
   * The overridden setter method that warns about unsupported option.
   *
   * @param optimize the option value
   */
  @Override
  public void setOptimize(boolean optimize) {
    unsupportedOptionMessage("optimize");
    super.setOptimize(optimize);
  }

  /**
   * The overridden setter method that warns about unsupported option.
   *
   * @param depend the option value
   */
  @Override
  public void setDepend(boolean depend) {
    unsupportedOptionMessage("depend");
    super.setDepend(depend);
  }

  /**
   * The overridden setter method that warns about unsupported option.
   *
   * @param f the option value
   */
  @Override
  public void setFork(boolean f) {
    unsupportedOptionMessage("fork");
    super.setFork(f);
  }

  /**
   * The overridden setter method that warns about unsupported option.
   *
   * @param forkExec the option value
   */
  @Override
  public void setExecutable(String forkExec) {
    unsupportedOptionMessage("executable");
    super.setExecutable(forkExec);
  }

  /**
   * The overridden setter method that warns about unsupported option.
   *
   * @param compiler the option value
   */
  @Override
  public void setCompiler(String compiler) {
    unsupportedOptionMessage("compiler");
    super.setCompiler(compiler);
  }

  /**
   * Sets the nested form directories that will be used during the
   * compilation.
   * @param nestedformdirs a list of {@link PrefixedPath}
   */
  public void setNestedformdirs(List nestedformdirs) {
    myNestedFormPathList = nestedformdirs;
  }

  /**
   * Gets the nested form directories that will be used during the
   * compilation.
   * @return the extension directories as a list of {@link PrefixedPath}
   */
  public List getNestedformdirs() {
    return myNestedFormPathList;
  }

  /**
   * Adds a path to nested form directories.
   * @return a path to be configured
   */
  public PrefixedPath createNestedformdirs() {
    PrefixedPath p = new PrefixedPath(getProject());
    if (myNestedFormPathList == null) {
      myNestedFormPathList = new ArrayList<PrefixedPath>();
    }
    myNestedFormPathList.add(p);
    return p;
  }



  /**
   * The overridden compile method that does not actually compiles java sources but only instruments
   * class files.
   */
  @Override
  protected void compile() {
    // compile java
    if (areJavaClassesCompiled()) {
      super.compile();
    }

    InstrumentationClassFinder finder = buildClasspathClassLoader();
    if (finder == null) {
      return;
    }
    try {
      instrumentForms(finder);

      if (getInstrumentNotNull()) {
        //NotNull instrumentation
        final int instrumented = instrumentNotNull(getDestdir(), finder);
        log("Added @NotNull assertions to " + instrumented + " files", Project.MSG_INFO);
      }
    }
    finally {
      finder.releaseResources();
    }
  }

  /**
   * Instrument forms
   *
   * @param finder a classloader to use
   */
  private void instrumentForms(final InstrumentationClassFinder finder) {
    // we instrument every file, because we cannot find which files should not be instrumented without dependency storage
    final ArrayList<File> formsToInstrument = myFormFiles;

    if (formsToInstrument.isEmpty()) {
      log("No forms to instrument found", Project.MSG_VERBOSE);
      return;
    }

    final HashMap<String, File> class2form = new HashMap<String, File>();

    for (File formFile : formsToInstrument) {

      log("compiling form " + formFile.getAbsolutePath(), Project.MSG_VERBOSE);
      final LwRootContainer rootContainer;
      try {
        rootContainer = Utils.getRootContainer(formFile.toURI().toURL(), new CompiledClassPropertiesProvider(finder.getLoader()));
      }
      catch (AlienFormFileException e) {
        // ignore non-IDEA forms
        continue;
      }
      catch (Exception e) {
        fireError("Cannot process form file " + formFile.getAbsolutePath() + ". Reason: " + e);
        continue;
      }

      final String classToBind = rootContainer.getClassToBind();
      if (classToBind == null) {
        continue;
      }

      String name = classToBind.replace('.', '/');
      File classFile = getClassFile(name);
      if (classFile == null) {
        log(formFile.getAbsolutePath() + ": Class to bind does not exist: " + classToBind, Project.MSG_WARN);
        continue;
      }

      final File alreadyProcessedForm = class2form.get(classToBind);
      if (alreadyProcessedForm != null) {
        fireError(formFile.getAbsolutePath() +
                  ": " +
                  "The form is bound to the class " +
                  classToBind +
                  ".\n" +
                  "Another form " +
                  alreadyProcessedForm.getAbsolutePath() +
                  " is also bound to this class.");
        continue;
      }
      class2form.put(classToBind, formFile);

      try {
        int version;
        InputStream stream = new FileInputStream(classFile);
        try {
          version = InstrumenterClassWriter.getClassFileVersion(new ClassReader(stream));
        }
        finally {
          stream.close();
        }
        AntNestedFormLoader formLoader = new AntNestedFormLoader(finder.getLoader(), myNestedFormPathList);
        InstrumenterClassWriter classWriter = new InstrumenterClassWriter(InstrumenterClassWriter.getAsmClassWriterFlags(version), finder);
        final AsmCodeGenerator codeGenerator = new AsmCodeGenerator(rootContainer, finder, formLoader, false, classWriter);
        codeGenerator.patchFile(classFile);
        final FormErrorInfo[] warnings = codeGenerator.getWarnings();

        for (FormErrorInfo warning : warnings) {
          log(formFile.getAbsolutePath() + ": " + warning.getErrorMessage(), Project.MSG_WARN);
        }
        final FormErrorInfo[] errors = codeGenerator.getErrors();
        if (errors.length > 0) {
          StringBuilder message = new StringBuilder();
          for (FormErrorInfo error : errors) {
            if (message.length() > 0) {
              message.append("\n");
            }
            message.append(formFile.getAbsolutePath()).append(": ").append(error.getErrorMessage());
          }
          fireError(message.toString());
        }
      }
      catch (Exception e) {
        fireError("Forms instrumentation failed for " + formFile.getAbsolutePath() + ": " + e.toString());
      }
    }
  }

  /**
   * Create class loader based on classpath, bootclasspath, and sourcepath.
   *
   * @return a URL classloader
   */
  private InstrumentationClassFinder buildClasspathClassLoader() {
    final StringBuilder classPathBuffer = new StringBuilder();
    final Project project = getProject();
    final Path cp = new Path(project);
    appendPath(cp, getBootclasspath());
    cp.setLocation(getDestdir().getAbsoluteFile());
    appendPath(cp, getClasspath());
    appendPath(cp, getSourcepath());
    appendPath(cp, getSrcdir());
    if (getIncludeantruntime()) {
      cp.addExisting(cp.concatSystemClasspath("last"));
    }
    boolean shouldIncludeJavaRuntime = getIncludejavaruntime();
    if (!shouldIncludeJavaRuntime) {
      if (project != null) {
        final String propValue = project.getProperty(PROPERTY_INSTRUMENTATION_INCLUDE_JAVA_RUNTIME);
        shouldIncludeJavaRuntime = !("false".equalsIgnoreCase(propValue) || "no".equalsIgnoreCase(propValue));
      }
      else {
        shouldIncludeJavaRuntime = true;
      }
    }
    if (shouldIncludeJavaRuntime) {
      cp.addJavaRuntime();
    }

    cp.addExtdirs(getExtdirs());

    final String[] pathElements = cp.list();
    for (final String pathElement : pathElements) {
      classPathBuffer.append(File.pathSeparator);
      classPathBuffer.append(pathElement);
    }

    final String classPath = classPathBuffer.toString();
    log("classpath=" + classPath, Project.MSG_VERBOSE);

    try {
      return createInstrumentationClassFinder(classPath, shouldIncludeJavaRuntime);
    }
    catch (MalformedURLException e) {
      fireError(e.getMessage());
      return null;
    }
  }

  private static URL tryGetJrtURL() {
    final String home = System.getProperty("java.home");
    if (new File(home, "lib/jrt-fs.jar").isFile()) {
      // this is a modular jdk where platform classes are stored in a jrt-fs image
      try {
        return InstrumentationClassFinder.createJDKPlatformUrl(home);
      }
      catch (MalformedURLException ignored) {
      }
    }
    return null;
  }


  /**
   * Append path to class path if the appened path is not empty and is not null
   *
   * @param cp the path to modify
   * @param p  the path to append
   */
  private void appendPath(Path cp, final Path p) {
    if (p != null && p.size() > 0) {
      cp.append(p);
    }
  }

  /**
   * Instrument classes with NotNull annotations
   *
   * @param dir    the directory with classes to instrument (the directory is processed recursively)
   * @param finder the classloader to use
   * @return the amount of classes actually affected by instrumentation
   */
  private int instrumentNotNull(File dir, final InstrumentationClassFinder finder) {
    int instrumented = 0;
    final File[] files = dir.listFiles();
    for (File file : files) {
      final String name = file.getName();
      if (name.endsWith(".class")) {
        final String path = file.getPath();
        log("Adding @NotNull assertions to " + path, Project.MSG_VERBOSE);
        try {
          final FileInputStream inputStream = new FileInputStream(file);
          try {
            FailSafeClassReader reader = new FailSafeClassReader(inputStream);

            int version = InstrumenterClassWriter.getClassFileVersion(reader);

            if ((version & 0xFFFF) >= Opcodes.V1_5 && !shouldBeSkippedByAnnotationPattern(reader)) {
              ClassWriter writer = new InstrumenterClassWriter(reader, InstrumenterClassWriter.getAsmClassWriterFlags(version), finder);

              if (NotNullVerifyingInstrumenter.processClassFile(reader, writer, myNotNullAnnotations.split(";"))) {
                final FileOutputStream fileOutputStream = new FileOutputStream(path);
                try {
                  fileOutputStream.write(writer.toByteArray());
                  instrumented++;
                }
                finally {
                  fileOutputStream.close();
                }
              }
            }
          }
          finally {
            inputStream.close();
          }
        }
        catch (IOException e) {
          log("Failed to instrument @NotNull assertion for " + path + ": " + e.getMessage(), Project.MSG_WARN);
        }
        catch (Exception e) {
          fireError("@NotNull instrumentation failed for " + path + ": " + e.toString());
        }
      }
      else if (file.isDirectory()) {
        instrumented += instrumentNotNull(file, finder);
      }
    }

    return instrumented;
  }

  private boolean shouldBeSkippedByAnnotationPattern(ClassReader reader) {
    if (myClassFilterAnnotationRegexpList.isEmpty()) {
      return false;
    }

    final boolean[] result = {false};
    reader.accept(new ClassVisitor(Opcodes.API_VERSION) {
      @Override
      public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (!result[0]) {
          String internalName = Type.getType(desc).getInternalName();
          for (Regexp regexp : myClassFilterAnnotationRegexpList) {
            if (regexp.matches(internalName)) {
              result[0] = true;
              break;
            }
          }
        }
        return null;
      }
    }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

    return result[0];
  }

  private void fireError(final String message) {
    if (failOnError) {
      throw new BuildException(message, getLocation());
    }
    else {
      log(message, Project.MSG_ERR);
    }
  }

  private File getClassFile(String className) {
    final String classOrInnerName = getClassOrInnerName(className);
    if (classOrInnerName == null) return null;
    return new File(getDestdir().getAbsolutePath(), classOrInnerName + ".class");
  }

  private String getClassOrInnerName(String className) {
    File classFile = new File(getDestdir().getAbsolutePath(), className + ".class");
    if (classFile.exists()) return className;
    int position = className.lastIndexOf('/');
    if (position == -1) return null;
    return getClassOrInnerName(className.substring(0, position) + '$' + className.substring(position + 1));
  }

  @Override
  protected void resetFileLists() {
    super.resetFileLists();
    myFormFiles = new ArrayList<File>();
  }

  @Override
  protected void scanDir(final File srcDir, final File destDir, final String[] files) {
    super.scanDir(srcDir, destDir, files);
    for (final String file : files) {
      if (file.endsWith(".form")) {
        log("Found form file " + file, Project.MSG_VERBOSE);
        myFormFiles.add(new File(srcDir, file));
      }
    }
  }

  private static InstrumentationClassFinder createInstrumentationClassFinder(final String classPath, boolean shouldIncludeJavaRuntime) throws MalformedURLException {
    final ArrayList<URL> urls = new ArrayList<URL>();
    if (shouldIncludeJavaRuntime) {
      final URL jrt = tryGetJrtURL();
      if (jrt != null) {
        urls.add(jrt);
      }
    }
    for (StringTokenizer tokenizer = new StringTokenizer(classPath, File.pathSeparator); tokenizer.hasMoreTokens();) {
      final String s = tokenizer.nextToken();
      urls.add(new File(s).toURI().toURL());
    }
    final URL[] urlsArr = urls.toArray(new URL[0]);
    return new InstrumentationClassFinder(urlsArr);
  }

  private class AntNestedFormLoader implements NestedFormLoader {
    private final ClassLoader myLoader;
    private final List<PrefixedPath> myNestedFormPathList;
    private final HashMap<String, LwRootContainer> myFormCache = new HashMap<String, LwRootContainer>();

    AntNestedFormLoader(final ClassLoader loader, List nestedFormPathList) {
      myLoader = loader;
      myNestedFormPathList = nestedFormPathList;
    }

    @Override
    public LwRootContainer loadForm(String formFilePath) throws Exception {
      if (myFormCache.containsKey(formFilePath)) {
        return myFormCache.get(formFilePath);
      }

      String lowerFormFilePath = formFilePath.toLowerCase(Locale.ENGLISH);
      log("Searching for form " + lowerFormFilePath, Project.MSG_VERBOSE);
      for (File file : myFormFiles) {
        String name = file.getAbsolutePath().replace(File.separatorChar, '/').toLowerCase(Locale.ENGLISH);
        log("Comparing with " + name, Project.MSG_VERBOSE);
        if (name.endsWith(lowerFormFilePath)) {
          return loadForm(formFilePath, new FileInputStream(file));
        }
      }

      if (myNestedFormPathList != null) {
        for (PrefixedPath path : myNestedFormPathList) {
          File formFile = path.findFile(formFilePath);
          if (formFile != null) {
            return loadForm(formFilePath, new FileInputStream(formFile));
          }
        }
      }
      InputStream resourceStream = myLoader.getResourceAsStream(formFilePath);
      if (resourceStream != null) {
        return loadForm(formFilePath, resourceStream);
      }
      throw new Exception("Cannot find nested form file " + formFilePath);
    }

    private LwRootContainer loadForm(String formFileName, InputStream resourceStream) throws Exception {
      final LwRootContainer container = Utils.getRootContainer(resourceStream, null);
      myFormCache.put(formFileName, container);
      return container;
    }

    @Override
    public String getClassToBindName(LwRootContainer container) {
      final String className = container.getClassToBind();
      String result = getClassOrInnerName(className.replace('.', '/'));
      if (result != null) return result.replace('/', '.');
      return className;
    }
  }
}
