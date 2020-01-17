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
package com.intellij.compiler.instrumentation;

import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;

import java.io.*;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Eugene Zhuravlev
 */
public class InstrumentationClassFinder {
  private static final PseudoClass[] EMPTY_PSEUDOCLASS_ARRAY = new PseudoClass[0];
  private static final String CLASS_RESOURCE_EXTENSION = ".class";
  private static final URL[] URL_EMPTY_ARRAY = new URL[0];
  private final Map<String, PseudoClass> myLoaded = new HashMap<String, PseudoClass>(); // className -> class object
  private final ClassFinderClasspath myPlatformClasspath;
  private final ClassFinderClasspath myClasspath;
  private final URL[] myPlatformUrls;
  private final URL[] myClasspathUrls;
  private ClassLoader myLoader;
  private byte[] myBuffer;

  public InstrumentationClassFinder(final URL[] cp) {
    this(URL_EMPTY_ARRAY, cp);
  }

  public InstrumentationClassFinder(final URL[] platformUrls, final URL[] classpathUrls) {
    myPlatformUrls = platformUrls;
    myClasspathUrls = classpathUrls;
    myPlatformClasspath = new ClassFinderClasspath(platformUrls);
    myClasspath = new ClassFinderClasspath(classpathUrls);
  }

  public static URL createJDKPlatformUrl(String jdkHomePath) throws MalformedURLException {
    return new URL(ClassFinderClasspath.Loader.JRT_PROTOCOL, null, jdkHomePath.replace(File.separatorChar, '/'));
  }

  // compatibility with legacy code requiring ClassLoader
  public ClassLoader getLoader() {
    ClassLoader loader = myLoader;
    if (loader != null) {
      return loader;
    }
    final URLClassLoader platformLoader = myPlatformUrls.length > 0 ? new URLClassLoader(myPlatformUrls, null) : null;
    final ClassLoader cpLoader = new URLClassLoader(myClasspathUrls, platformLoader);
    loader = new ClassLoader(cpLoader) {

      @Override
      public InputStream getResourceAsStream(String name) {
        InputStream is = super.getResourceAsStream(name);
        if (is == null) {
          try {
            is = InstrumentationClassFinder.this.getResourceAsStream(name);
          }
          catch (IOException ignored) {
          }
        }
        return is;
      }

      @Override
      protected Class findClass(String name) throws ClassNotFoundException {
        final InputStream is = lookupClassBeforeClasspath(name.replace('.', '/'));
        if (is == null) {
          throw new ClassNotFoundException("Class not found: " + name.replace('/', '.'));  // ensure presentable class name in error message
        }
        try {
          final byte[] bytes = loadBytes(is);
          return defineClass(name.replace('/', '.'), bytes, 0, bytes.length);
        }
        finally {
          try {
            is.close();
          }
          catch (IOException ignored) {
          }
        }
      }
    };
    myLoader = loader;
    return loader;
  }

  public void releaseResources() {
    myPlatformClasspath.releaseResources();
    myClasspath.releaseResources();
    myLoaded.clear();
    myBuffer = null;
    myLoader = null;
  }

  public PseudoClass loadClass(final String name) throws IOException, ClassNotFoundException{
    final String internalName = name.replace('.', '/'); // normalize
    final PseudoClass aClass = myLoaded.get(internalName);
    if (aClass != null && aClass != PseudoClass.NULL_OBJ) {
      return aClass;
    }

    final InputStream is = aClass == null? getClassBytesStream(internalName) : null;

    if (is == null) {
      if (aClass == null) {
        myLoaded.put(internalName, PseudoClass.NULL_OBJ);
      }
      // ensure presentable class name in error message
      throw new ClassNotFoundException("Class not found: " + name.replace('/', '.')) {
        @Override
        public synchronized Throwable fillInStackTrace() {
          return this;
        }
      };
    }

    try {
      final PseudoClass result = loadPseudoClass(is);
      myLoaded.put(internalName, result);
      return result;
    }
    finally {
      is.close();
    }
  }

  public void cleanCachedData(String className) {
    myLoaded.remove(className.replace('.', '/'));
  }

  public InputStream getClassBytesAsStream(String className) throws IOException {
    final String internalName = className.replace('.', '/'); // normalize
    final PseudoClass aClass = myLoaded.get(internalName);
    if (aClass == PseudoClass.NULL_OBJ) {
      return null;
    }
    InputStream bytes = null;
    try {
      bytes = getClassBytesStream(internalName);
    }
    finally {
      if (aClass == null && bytes == null) {
        myLoaded.put(internalName, PseudoClass.NULL_OBJ);
      }
    }
    return bytes;
  }

  private InputStream getClassBytesStream(String internalName) throws IOException {
    InputStream is = null;
    // first look into platformCp
    final String resourceName = internalName + CLASS_RESOURCE_EXTENSION;
    Resource resource = myPlatformClasspath.getResource(resourceName);
    if (resource != null) {
      is = resource.getInputStream();
    }
    // second look into memory and classpath
    if (is == null) {
      is = lookupClassBeforeClasspath(internalName);
    }

    if (is == null) {
      resource = myClasspath.getResource(resourceName);
      if (resource != null) {
        is = resource.getInputStream();
      }
    }

    if (is == null) {
      is = lookupClassAfterClasspath(internalName);
    }
    return is;
  }

  public InputStream getResourceAsStream(String resourceName) throws IOException {
    InputStream is = null;

    Resource resource = myPlatformClasspath.getResource(resourceName);
    if (resource != null) {
      is = resource.getInputStream();
    }

    if (is == null) {
      resource = myClasspath.getResource(resourceName);
      if (resource != null) {
        is = resource.getInputStream();
      }
    }

    return is;
  }

  protected InputStream lookupClassBeforeClasspath(final String internalClassName) {
    return null;
  }

  protected InputStream lookupClassAfterClasspath(final String internalClassName) {
    return null;
  }

  private PseudoClass loadPseudoClass(InputStream is) throws IOException {
    final ClassReader reader = new ClassReader(is);
    final V visitor = new V();

    reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

    return new PseudoClass(this, visitor.myName, visitor.mySuperclassName, visitor.myInterfaces, visitor.myModifiers, visitor.myMethods);
  }

  public static class PseudoClass {
    static final PseudoClass NULL_OBJ = new PseudoClass(null, null, null, null, 0, null);
    private final String myName;
    private final String mySuperClass;
    private final String[] myInterfaces;
    private final int myModifiers;
    private final List<PseudoMethod> myMethods;
    private final InstrumentationClassFinder myFinder;

    private PseudoClass(InstrumentationClassFinder finder,
                        final String name,
                        final String superClass,
                        final String[] interfaces,
                        final int modifiers,
                        List<PseudoMethod> methods) {
      myName = name;
      mySuperClass = superClass;
      myInterfaces = interfaces;
      myModifiers = modifiers;
      myMethods = methods;
      myFinder = finder;
    }

    public int getModifiers() {
      return myModifiers;
    }

    public boolean isInterface() {
      return (myModifiers & Opcodes.ACC_INTERFACE) > 0;
    }

    public String getName() {
      return myName;
    }

    public List<PseudoMethod> getMethods() {
      return myMethods;
    }

    public List<PseudoMethod> findMethods(String name) {
      final List<PseudoMethod> result = new ArrayList<PseudoMethod>();
      for (PseudoMethod method : myMethods) {
        if (method.getName().equals(name)){
          result.add(method);
        }
      }
      return result;
    }

    public PseudoMethod findMethod(String name, String descriptor) {
      for (PseudoMethod method : myMethods) {
        if (method.getName().equals(name) && method.getSignature().equals(descriptor)){
          return method;
        }
      }
      return null;
    }

    public PseudoMethod findMethodInHierarchy(final String name, final String descriptor) throws IOException, ClassNotFoundException {
      // first find in superclasses
      for (PseudoClass c = this; c != null; c = c.getSuperClass()) {
        final PseudoMethod method = c.findMethod(name, descriptor);
        if (method != null) {
          return method;
        }
      }
      // second, check interfaces
      for (PseudoClass iface : getInterfaces()) {
        final PseudoMethod method = findInterfaceMethodRecursively(iface, name, descriptor);
        if (method != null) {
          return method;
        }
      }
      return null;
    }

    private static PseudoMethod findInterfaceMethodRecursively(PseudoClass fromIface, final String name, final String descriptor) throws IOException, ClassNotFoundException {
      PseudoMethod method = fromIface.findMethod(name, descriptor);
      if (method != null) {
        return method;
      }
      for (PseudoClass superIface : fromIface.getInterfaces()) {
        method = findInterfaceMethodRecursively(superIface, name, descriptor);
        if (method != null) {
          return method;
        }
      }
      return null;
    }

    public InstrumentationClassFinder getFinder() {
      return myFinder;
    }

    public PseudoClass getSuperClass() throws IOException, ClassNotFoundException {
      final String superClass = mySuperClass;
      return superClass != null? myFinder.loadClass(superClass) : null;
    }

    public PseudoClass[] getInterfaces() throws IOException, ClassNotFoundException {
      if (myInterfaces == null) {
        return EMPTY_PSEUDOCLASS_ARRAY;
      }

      final PseudoClass[] result = new PseudoClass[myInterfaces.length];

      for (int i = 0; i < result.length; i++) {
        result[i] = myFinder.loadClass(myInterfaces[i]);
      }

      return result;
    }

    public boolean equals (final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      return getName().equals(((PseudoClass)o).getName());
    }

    private boolean isSubclassOf(final PseudoClass x) throws IOException, ClassNotFoundException {
      for (PseudoClass c = this; c != null; c = c.getSuperClass()) {
        final PseudoClass superClass = c.getSuperClass();

        if (superClass != null && superClass.equals(x)) {
          return true;
        }
      }

      return false;
    }

    private boolean implementsInterface(final PseudoClass x) throws IOException, ClassNotFoundException {
      for (PseudoClass c = this; c != null; c = c.getSuperClass()) {
        final PseudoClass[] tis = c.getInterfaces();
        for (final PseudoClass ti : tis) {
          if (ti.equals(x) || ti.implementsInterface(x)) {
            return true;
          }
        }
      }
      return false;
    }

    public boolean isAssignableFrom(final PseudoClass x) throws IOException, ClassNotFoundException {
      if (this.equals(x)) {
        return true;
      }
      if (x.isSubclassOf(this)) {
        return true;
      }
      if (x.implementsInterface(this)) {
        return true;
      }
      if (x.isInterface() && "java/lang/Object".equals(getName())) {
        return true;
      }
      return false;
    }

    public boolean hasDefaultPublicConstructor() {
      for (PseudoMethod method : myMethods) {
        if ("<init>".equals(method.getName()) && "()V".equals(method.getSignature())) {
          return true;
        }
      }
      return false;
    }

    public String getDescriptor() {
      return "L" + myName + ";";
    }
  }

  public static final class PseudoMethod {
    private final int myAccess;
    private final String myName;
    private final String mySignature;

    public PseudoMethod(int access, String name, String signature) {
      myAccess = access;
      myName = name;
      mySignature = signature;
    }

    public int getModifiers() {
      return myAccess;
    }

    public String getName() {
      return myName;
    }

    public String getSignature() {
      return mySignature;
    }
  }

  private static class V extends ClassVisitor {
    public String mySuperclassName = null;
    public String[] myInterfaces = null;
    public String myName = null;
    public int myModifiers;
    private final List<PseudoMethod> myMethods = new ArrayList<PseudoMethod>();

    private V() {
      super(Opcodes.API_VERSION);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      if ((access & Opcodes.ACC_PUBLIC) > 0) {
        myMethods.add(new PseudoMethod(access, name, desc));
      }
      return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public void visit(int version, int access, String pName, String signature, String pSuperName, String[] pInterfaces) {
      mySuperclassName = pSuperName;
      myInterfaces = pInterfaces;
      myName = pName;
      myModifiers = access;
    }
  }

  public interface Resource {
    InputStream getInputStream() throws IOException;
  }

  static class ClassFinderClasspath {

    private final Stack<URL> myUrls = new Stack<URL>();
    private final List<Loader> myLoaders = new ArrayList<Loader>();
    private final Map<URL,Loader> myLoadersMap = new HashMap<URL, Loader>();

    ClassFinderClasspath(URL[] urls) {
      if (urls.length > 0) {
        for (int i = urls.length - 1; i >= 0; i--) {
          myUrls.push(urls[i]);
        }
      }
    }

    public Resource getResource(String s) {
      int i = 0;
      for (Loader loader; (loader = getLoader(i)) != null; i++) {
        Resource resource = loader.getResource(s);
        if (resource != null) {
          return resource;
        }
      }

      return null;
    }

    public void releaseResources() {
      for (Loader loader : myLoaders) {
        loader.releaseResources();
      }
      myLoaders.clear();
      myLoadersMap.clear();
    }

    private synchronized Loader getLoader(int i) {
      while (myLoaders.size() < i + 1) {
        URL url;
        synchronized (myUrls) {
          if (myUrls.empty()) {
            return null;
          }
          url = myUrls.pop();
        }

        if (myLoadersMap.containsKey(url)) {
          continue;
        }

        Loader loader;
        try {
          loader = getLoader(url, myLoaders.size());
          if (loader == null) {
            continue;
          }
        }
        catch (IOException ioexception) {
          continue;
        }

        myLoaders.add(loader);
        myLoadersMap.put(url, loader);
      }

      return myLoaders.get(i);
    }

    private static Loader getLoader(final URL url, int index) throws IOException {
      String s;
      final String protocol = url.getProtocol();
      try {
        s = Loader.JRT_PROTOCOL.equals(protocol)? url.getFile() : url.toURI().getSchemeSpecificPart();
      }
      catch (URISyntaxException thisShouldNotHappen) {
        thisShouldNotHappen.printStackTrace();
        s = url.getFile();
      }

      if (s != null && s.length() > 0) {
        if (Loader.JRT_PROTOCOL.equals(protocol)) {
          final Loader jrtLoader = JrtClassHolder.create(url, index);
          if (jrtLoader != null) {
            return jrtLoader;
          }
        }
        if (new File(s).isDirectory()) {
          return Loader.FILE_PROTOCOL.equals(protocol) ? new FileLoader(url, index) : null;
        }
      }

      // by default treat the url as a jar archive
      return new JarLoader(url, index);
    }


    abstract static class Loader {
      protected static final String JAR_PROTOCOL = "jar";
      protected static final String FILE_PROTOCOL = "file";
      protected static final String JRT_PROTOCOL = "jrt";

      private final URL myURL;
      private final int myIndex;

      protected Loader(URL url, int index) {
        myURL = url;
        myIndex = index;
      }


      protected URL getBaseURL() {
        return myURL;
      }

      public abstract Resource getResource(final String name);

      public abstract void releaseResources();

      public int getIndex() {
        return myIndex;
      }
    }

    private static class FileLoader extends Loader {
      private final File myRootDir;

      FileLoader(URL url, int index) {
        super(url, index);
        if (!FILE_PROTOCOL.equals(url.getProtocol())) {
          throw new IllegalArgumentException("url");
        }
        else {
          final String s = unescapePercentSequences(url.getFile().replace('/', File.separatorChar));
          myRootDir = new File(s);
        }
      }

      @Override
      public void releaseResources() {
      }

      @Override
      public Resource getResource(final String name) {
        try {
          final URL url = new URL(getBaseURL(), name);
          if (!url.getFile().startsWith(getBaseURL().getFile())) {
            return null;
          }

          final File file = new File(myRootDir, name.replace('/', File.separatorChar));
          if (file.exists()) {
            return new Resource() {
              @Override
              public InputStream getInputStream() throws IOException {
                return new BufferedInputStream(new FileInputStream(file));
              }

              public String toString() {
                return file.getAbsolutePath();
              }
            };
          }
        }
        catch (Exception ignored) {
        }
        return null;
      }

      public String toString() {
        return "FileLoader [" + myRootDir + "]";
      }
    }

    private static class JarLoader extends Loader {
      private final URL myURL;
      private ZipFile myZipFile;

      JarLoader(URL url, int index) throws IOException {
        super(new URL(JAR_PROTOCOL, "", -1, url + "!/"), index);
        myURL = url;
      }

      @Override
      public void releaseResources() {
        final ZipFile zipFile = myZipFile;
        if (zipFile != null) {
          myZipFile = null;
          try {
            zipFile.close();
          }
          catch (IOException e) {
            throw new RuntimeException();
          }
        }
      }

      private ZipFile acquireZipFile() throws IOException {
        ZipFile zipFile = myZipFile;
        if (zipFile == null) {
          zipFile = doGetZipFile();
          myZipFile = zipFile;
        }
        return zipFile;
      }

      private ZipFile doGetZipFile() throws IOException {
        if (FILE_PROTOCOL.equals(myURL.getProtocol())) {
          String s = unescapePercentSequences(myURL.getFile().replace('/', File.separatorChar));
          if (new File(s).exists()) {
            return new ZipFile(s);
          }
        }

        return null;
      }

      @Override
      public Resource getResource(String name) {
        try {
          final ZipFile file = acquireZipFile();
          if (file != null) {
            final ZipEntry entry = file.getEntry(name);
            if (entry != null) {
              return new Resource() {
                @Override
                public InputStream getInputStream() {
                  try {
                    final ZipFile file = acquireZipFile();
                    if (file != null) {
                      final InputStream inputStream = file.getInputStream(entry);
                      if (inputStream != null) {
                        return new FilterInputStream(inputStream) {};
                      }
                    }
                  }
                  catch (IOException e) {
                    e.printStackTrace();
                  }
                  return null;
                }

                public String toString() {
                  return "JarLoader [" + myURL + "!/" + entry.getName() + "]";
                }
              };
            }
          }
        }
        catch (Exception e) {
          return null;
        }
        return null;
      }

    }
  }


  private static String unescapePercentSequences(String s) {
    if (s.indexOf('%') == -1) {
      return s;
    }
    StringBuilder decoded = new StringBuilder();
    final int len = s.length();
    int i = 0;
    while (i < len) {
      char c = s.charAt(i);
      if (c == '%') {
        List<Integer> bytes = new ArrayList<Integer>();
        while (i + 2 < len && s.charAt(i) == '%') {
          final int d1 = decode(s.charAt(i + 1));
          final int d2 = decode(s.charAt(i + 2));
          if (d1 != -1 && d2 != -1) {
            bytes.add(((d1 & 0xf) << 4 | d2 & 0xf));
            i += 3;
          }
          else {
            break;
          }
        }
        if (!bytes.isEmpty()) {
          final byte[] bytesArray = new byte[bytes.size()];
          for (int j = 0; j < bytes.size(); j++) {
            bytesArray[j] = (byte)bytes.get(j).intValue();
          }
          try {
            decoded.append(new String(bytesArray, "UTF-8"));
            continue;
          }
          catch (UnsupportedEncodingException ignored) {
          }
        }
      }

      decoded.append(c);
      i++;
    }
    return decoded.toString();
  }

  private static int decode(char c) {
    if ((c >= '0') && (c <= '9')){
      return c - '0';
    }
    if ((c >= 'a') && (c <= 'f')){
      return c - 'a' + 10;
    }
    if ((c >= 'A') && (c <= 'F')){
      return c - 'A' + 10;
    }
    return -1;
  }

  public byte[] loadBytes(InputStream stream) {
    byte[] buf = myBuffer;
    if (buf == null) {
      buf = new byte[512];
      myBuffer = buf;
    }

    final ByteArrayOutputStream result = new ByteArrayOutputStream();
    try {
      while (true) {
        int n = stream.read(buf, 0, buf.length);
        if (n <= 0) {
          break;
        }
        result.write(buf, 0, n);
      }
      result.close();
    }
    catch (IOException ignored) {
    }
    return result.toByteArray();
  }

  private static final class JrtClassHolder {
    public static final Class<? extends ClassFinderClasspath.Loader> ourClass;
    public static final Constructor<? extends ClassFinderClasspath.Loader> ourConstructor;

    static {
      Class<? extends ClassFinderClasspath.Loader> aClass = null;
      Constructor<? extends ClassFinderClasspath.Loader> constructor = null;
      try {
        aClass = Class.forName("com.intellij.compiler.instrumentation.JrtLoader").asSubclass(ClassFinderClasspath.Loader.class);
        constructor = aClass.getDeclaredConstructor(URL.class, int.class);
        constructor.setAccessible(true);
      }
      catch (Throwable ignored) {
      }
      ourClass = aClass;
      ourConstructor = constructor;
    }

    public static ClassFinderClasspath.Loader create(URL url, int index) {
      if (ourConstructor != null) {
        try {
          return ourConstructor.newInstance(url, index);
        }
        catch (Throwable ignored) {
        }
      }
      return null;
    }
  }

}
