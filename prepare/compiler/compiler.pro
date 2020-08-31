-dontnote **
-dontwarn apple.awt.*
-dontwarn dk.brics.automaton.*
-dontwarn org.fusesource.**
-dontwarn org.imgscalr.Scalr**
-dontwarn org.xerial.snappy.SnappyBundleActivator
-dontwarn com.intellij.util.CompressionUtil
-dontwarn com.intellij.util.SnappyInitializer
-dontwarn com.intellij.util.SVGLoader
-dontwarn com.intellij.util.SVGLoader$MyTranscoder
-dontwarn com.intellij.util.ImageLoader$ImageDesc
-dontwarn com.intellij.util.ui.**
-dontwarn com.intellij.ui.**
-dontwarn com.intellij.util.IconUtil
-dontwarn com.intellij.util.ImageLoader
-dontwarn net.sf.cglib.**
-dontwarn org.objectweb.asm.** # this is ASM3, the old version that we do not use
-dontwarn com.sun.jna.NativeString
-dontwarn com.sun.jna.WString
-dontwarn com.intellij.psi.util.PsiClassUtil
-dontwarn org.apache.hadoop.io.compress.*
-dontwarn com.google.j2objc.annotations.Weak
-dontwarn org.iq80.snappy.HadoopSnappyCodec$SnappyCompressionInputStream
-dontwarn org.iq80.snappy.HadoopSnappyCodec$SnappyCompressionOutputStream
-dontwarn com.google.common.util.concurrent.*
-dontwarn org.apache.xerces.dom.**
-dontwarn org.apache.xerces.util.**
-dontwarn org.w3c.dom.ElementTraversal
-dontwarn javaslang.match.annotation.Unapply
-dontwarn javaslang.match.annotation.Patterns
-dontwarn javaslang.*
-dontwarn kotlinx.collections.immutable.*
-dontwarn kotlinx.collections.immutable.**
-dontwarn com.google.errorprone.**
-dontwarn com.google.j2objc.**
-dontwarn javax.crypto.**
-dontwarn java.lang.invoke.MethodHandle
-dontwarn org.jline.builtins.Nano$Buffer
-dontwarn com.intellij.util.io.TarUtil
-dontwarn com.intellij.util.io.Compressor$Tar

# Some annotations from intellijCore/annotations.jar are not presented in org.jetbrains.annotations
-dontwarn org.jetbrains.annotations.*

# Nullability annotations used in Guava
-dontwarn org.checkerframework.checker.nullness.compatqual.NullableDecl
-dontwarn org.checkerframework.checker.nullness.compatqual.MonotonicNonNullDecl
-dontwarn org.checkerframework.checker.nullness.qual.Nullable
-dontwarn org.checkerframework.checker.nullness.qual.MonotonicNonNull
-dontwarn org.checkerframework.checker.nullness.qual.NonNull

# Depends on apache batick which has lots of dependencies
-dontwarn com.intellij.util.SVGLoader*
-dontwarn org.apache.batik.script.rhino.RhinoInterpreter
-dontwarn org.apache.batik.script.rhino.RhinoInterpreterFactory

# The appropriate jar is either loaded separately or added explicitly to the classpath then needed
-dontwarn org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingCompilerConfigurationComponentRegistrar

-dontwarn org.jdom.xpath.jaxen.*
-dontwarn com.intellij.util.io.Decompressor*
-dontwarn org.w3c.dom.Location
-dontwarn org.w3c.dom.Window
-dontwarn org.slf4j.**

#-libraryjars '<rtjar>'
#-libraryjars '<jssejar>'
#-libraryjars '<bootstrap.runtime>'
#-libraryjars '<bootstrap.reflect>'
#-libraryjars '<bootstrap.script.runtime>'
#-libraryjars '<tools.jar>'

-dontoptimize
-dontobfuscate

-keep class org.fusesource.** { *; }
-keep class com.sun.jna.** { *; }

-keep class org.jetbrains.annotations.** {
    public protected *;
}

-keep class javax.inject.** {
    public protected *;
}

-keep class org.jetbrains.kotlin.** {
    public protected *;
}

-keep class org.jetbrains.kotlin.compiler.plugin.** {
    public protected *;
}

-keep class org.jetbrains.kotlin.extensions.** {
    public protected *;
}

-keep class org.jetbrains.kotlin.protobuf.** {
    public protected *;
}

# temporary workaround for KTI-298
-keepclassmembers class com.google.common.** { *; }

-keep class org.jetbrains.kotlin.container.** { *; }

-keep class org.jetbrains.org.objectweb.asm.Opcodes { *; }

-keep class org.jetbrains.kotlin.codegen.extensions.** {
    public protected *;
}

-keepclassmembers class com.intellij.openapi.vfs.VirtualFile {
    public protected *;
}

-keep class com.intellij.openapi.vfs.StandardFileSystems {
    public static *;
}

# needed for jar cache cleanup in the gradle plugin and compile daemon
-keepclassmembers class com.intellij.openapi.vfs.impl.ZipHandler {
    public static void clearFileAccessorCache();
}

-keep class jet.** {
    public protected *;
}

-keep class com.intellij.psi.** {
    public protected *;
}

# This is needed so that the platform code which parses XML wouldn't fail, see KT-16968
# This API is used from org.jdom.input.SAXBuilder via reflection.
-keep class org.jdom.input.JAXPParserFactory { public ** createParser(...); }
# Without this class PluginManagerCore.loadDescriptorFromJar fails
-keep class org.jdom.output.XMLOutputter { *; }

# for kdoc & dokka
-keep class com.intellij.openapi.util.TextRange { *; }
-keep class com.intellij.lang.impl.PsiBuilderImpl* {
    public protected *;
}
-keep class com.intellij.openapi.util.text.StringHash { *; }

# for j2k
-keep class com.intellij.codeInsight.NullableNotNullManager { public protected *; }

# for gradle (see KT-12549)
-keep class com.intellij.lang.properties.charset.Native2AsciiCharsetProvider { *; }

# for kotlin-build-common (consider repacking compiler together with kotlin-build-common and remove this part afterwards)
-keep class com.intellij.util.io.IOUtil { public *; }
-keep class com.intellij.openapi.util.io.FileUtil { public *; }
-keep class com.intellij.util.SystemProperties { public *; }
-keep class com.intellij.util.containers.hash.LinkedHashMap { *; }
-keep class com.intellij.util.containers.ConcurrentIntObjectMap { *; }
-keep class com.intellij.util.containers.ComparatorUtil { *; }
-keep class com.intellij.util.io.PersistentHashMapValueStorage { *; }
-keep class com.intellij.util.io.PersistentHashMap { *; }
-keep class com.intellij.util.io.BooleanDataDescriptor { *; }
-keep class com.intellij.util.io.EnumeratorStringDescriptor { *; }
-keep class com.intellij.util.io.ExternalIntegerKeyDescriptor { *; }
-keep class com.intellij.util.containers.hash.EqualityPolicy { *; }
-keep class com.intellij.util.containers.hash.EqualityPolicy.* { *; }
-keep class com.intellij.util.containers.Interner { *; }
-keep class gnu.trove.TIntHashSet { *; }
-keep class gnu.trove.TIntIterator { *; }
-keep class org.iq80.snappy.SlowMemory { *; }
-keep class javaslang.match.PatternsProcessor { *; }

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * {
    ** toString();
    ** hashCode();
    void start();
    void stop();
    void dispose();
}

-keep class org.jetbrains.org.objectweb.asm.tree.AnnotationNode { *; }
-keep class org.jetbrains.org.objectweb.asm.tree.ClassNode { *; }
-keep class org.jetbrains.org.objectweb.asm.tree.LocalVariableNode { *; }
-keep class org.jetbrains.org.objectweb.asm.tree.MethodNode { *; }
-keep class org.jetbrains.org.objectweb.asm.tree.FieldNode { *; }
-keep class org.jetbrains.org.objectweb.asm.tree.ParameterNode { *; }
-keep class org.jetbrains.org.objectweb.asm.tree.TypeAnnotationNode { *; }
-keep class org.jetbrains.org.objectweb.asm.tree.InsnList { *; }

-keep class org.jetbrains.org.objectweb.asm.signature.SignatureReader { *; }
-keep class org.jetbrains.org.objectweb.asm.signature.SignatureVisitor { *; }

-keep class org.jetbrains.org.objectweb.asm.Type {
    public protected *;
}

-keepclassmembers class org.jetbrains.org.objectweb.asm.ClassReader {
    *** SKIP_CODE;
    *** SKIP_DEBUG;
    *** SKIP_FRAMES;
}

-keepclassmembers class com.intellij.openapi.project.Project {
    ** getBasePath();
}

# for kotlin-android-extensions in maven
-keep class com.intellij.openapi.module.ModuleServiceManager { public *; }

# for building kotlin-build-common-test
-keep class org.jetbrains.kotlin.build.SerializationUtilsKt { *; }

# for tools.jar
-keep class com.sun.tools.javac.** { *; }
-keep class com.sun.source.** { *; }

# for webdemo
-keep class com.intellij.openapi.progress.ProgressManager { *; }

# for kapt
-keep class com.intellij.openapi.project.Project { *; }
-keepclassmembers class com.intellij.util.PathUtil {
    public static java.lang.String getJarPathForClass(java.lang.Class);
}

-keepclassmembers class com.intellij.util.PathUtil {
    public static java.lang.String getJarPathForClass(java.lang.Class);
}

# remove when KT-18563 would be fixed
-keep class org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt { *; }

-keep class net.jpountz.lz4.* { *; }

# used in LazyScriptDescriptor
-keep class org.jetbrains.kotlin.utils.addToStdlib.AddToStdlibKt { *; }

-keep class com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem { *; }

# Serialization plugin

-keep class com.intellij.openapi.util.io.JarUtil {
    public static java.lang.String getJarAttribute(java.io.File, java.util.jar.Attributes$Name);
}

# used in REPL
# TODO: pack jline directly to scripting-compiler jars instead
-keep class org.jline.reader.LineReaderBuilder { *; }
-keep class org.jline.reader.LineReader { *; }
-keep class org.jline.reader.History { *; }
-keep class org.jline.reader.EndOfFileException { *; }
-keep class org.jline.reader.UserInterruptException { *; }
-keep class org.jline.terminal.impl.jna.JnaSupportImpl  { *; }
-keep class org.jline.terminal.impl.jansi.JansiSupportImpl  { *; }

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
}

-dontwarn org.jetbrains.kotlin.fir.**
