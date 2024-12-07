-dontnote **
-dontwarn apple.awt.*
-dontwarn com.google.common.util.concurrent.*
-dontwarn com.google.errorprone.**
-dontwarn com.google.j2objc.**
-dontwarn com.google.j2objc.annotations.Weak
# True warning, should be removed later: https://github.com/JetBrains/intellij-community/commit/e84b32f0620126b0e2b3a3f477cda8c1c9b5b4d2
-dontwarn com.intellij.openapi.vfs.VirtualFileUtil
-dontwarn com.intellij.platform.util.progress.ProgressReporterKt
-dontwarn com.intellij.platform.util.progress.RawProgressReporter
-dontwarn com.intellij.psi.util.PsiClassUtil
-dontwarn com.intellij.ui.**
-dontwarn com.intellij.util.CompressionUtil
-dontwarn com.intellij.util.IconUtil
-dontwarn com.intellij.util.ImageLoader
-dontwarn com.intellij.util.ImageLoader$ImageDesc
-dontwarn com.intellij.util.SVGLoader
-dontwarn com.intellij.util.SVGLoader$MyTranscoder
-dontwarn com.intellij.util.SnappyInitializer
-dontwarn com.intellij.util.diff.*
-dontwarn com.intellij.util.io.Compressor$Tar
-dontwarn com.intellij.util.io.TarUtil
-dontwarn com.intellij.util.lang.*
-dontwarn com.intellij.util.ui.**
-dontwarn com.sun.jna.NativeString
-dontwarn com.sun.jna.WString
-dontwarn dk.brics.automaton.*
-dontwarn java.lang.invoke.MethodHandle
-dontwarn io.vavr.*
-dontwarn javax.crypto.**
-dontwarn kotlinx.collections.immutable.*
-dontwarn kotlinx.collections.immutable.**
-dontwarn kotlinx.coroutines.debug.DebugProbes
-dontwarn kotlinx.coroutines.future.FutureKt
-dontwarn kotlinx.serialization.**
-dontwarn net.sf.cglib.**
-dontwarn org.apache.hadoop.io.compress.*
-dontwarn org.apache.xerces.dom.**
-dontwarn org.apache.xerces.util.**
-dontwarn org.fusesource.**
-dontwarn org.imgscalr.Scalr**
-dontwarn org.iq80.snappy.HadoopSnappyCodec$SnappyCompressionInputStream
-dontwarn org.iq80.snappy.HadoopSnappyCodec$SnappyCompressionOutputStream
-dontwarn org.jline.builtins.Nano$Buffer
-dontwarn org.objectweb.asm.** # this is ASM3, the old version that we do not use
-dontwarn org.w3c.dom.ElementTraversal
-dontwarn org.xerial.snappy.SnappyBundleActivator

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

# Ignore generated Gradle DSL types
# They will be added separately on generating Gradle DSL for compiler options
-dontwarn org.jetbrains.kotlin.cli.common.arguments.DefaultValues$*

-dontwarn org.jdom.xpath.jaxen.*
-dontwarn com.intellij.util.io.Decompressor*
-dontwarn org.w3c.dom.Location
-dontwarn org.w3c.dom.Window
-dontwarn org.slf4j.**

# This class in com.intellij.platform.utils has accidental dependency on Java 11,
# but it is not used in the production code, so it should be fine to ignore this.
# The fix commit in platform: cbf405263b98ef2ad0ecb0d5a47dc18e1b325c9f
-dontwarn com.intellij.util.io.WalRecord$Companion

#-libraryjars '<rtjar>'
#-libraryjars '<jssejar>'
#-libraryjars '<bootstrap.runtime>'
#-libraryjars '<bootstrap.reflect>'
#-libraryjars '<bootstrap.script.runtime>'
#-libraryjars '<tools.jar>'

-dontprocesskotlinmetadata
-keep class kotlin.Metadata
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
-keep class org.jetbrains.org.objectweb.asm.tree.analysis.SimpleVerifier { *; }
-keep class org.jetbrains.org.objectweb.asm.tree.analysis.Analyzer { *; }
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
-keep class com.intellij.util.containers.OpenTHashSet { *; }
-keep class gnu.trove.TIntHashSet { *; }
-keep class gnu.trove.TIntIterator { *; }
-keep class org.iq80.snappy.SlowMemory { *; }

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

-keep class com.intellij.openapi.roots.ContentIterator  { *; }

-keepclassmembers class com.intellij.openapi.vfs.VfsUtilCore {
    public static boolean iterateChildrenRecursively(com.intellij.openapi.vfs.VirtualFile,com.intellij.openapi.vfs.VirtualFileFilter,com.intellij.openapi.roots.ContentIterator);
}

-keep class com.intellij.openapi.extensions.DefaultPluginDescriptor {
    public DefaultPluginDescriptor(java.lang.String);
}

-keep class com.intellij.ide.plugins.ContainerDescriptor {
    public java.util.List getServices();
}

-keep class com.intellij.util.messages.impl.MessageBusEx {
    void setLazyListeners(java.util.concurrent.ConcurrentMap);
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

# For Anvil https://youtrack.jetbrains.com/issue/KT-42103
-keepclassmembers class com.intellij.openapi.extensions.ExtensionPoint {
    public void registerExtension(...);
}

# Temporary for klint https://github.com/pinterest/ktlint/blob/c5a81e0d4198fa5cb2cac69967080e01e365b837/ktlint-rule-engine/src/main/kotlin/com/pinterest/ktlint/rule/engine/internal/KotlinPsiFileFactory.kt#L121
# Should be removed after after 26.04.2024
-keepclassmembers class com.intellij.openapi.extensions.ExtensionsArea {
    public void registerExtensionPoint(java.lang.String, java.lang.String, com.intellij.openapi.extensions.ExtensionPoint$Kind);
}

# used in REPL
# TODO: pack jline directly to scripting-compiler jars instead
-keep class org.jline.reader.LineReaderBuilder { *; }
-keep class org.jline.reader.LineReader { *; }
-keep class org.jline.reader.History { *; }
-keep class org.jline.reader.EndOfFileException { *; }
-keep class org.jline.reader.UserInterruptException { *; }
-keep class org.jline.terminal.TerminalBuilder { *; }
-keep class org.jline.terminal.impl.jna.JnaSupportImpl  { *; }
-keep class org.jline.terminal.impl.jansi.JansiSupportImpl  { *; }

# Keep rules for serializable classes (see https://www.guardsquare.com/manual/configuration/examples#serializable)
-keepclassmembers class * implements java.io.Serializable {
        static final long serialVersionUID;
        private static final java.io.ObjectStreamField[] serialPersistentFields;
        !static !transient <fields>;
        private void writeObject(java.io.ObjectOutputStream);
        private void readObject(java.io.ObjectInputStream);
        java.lang.Object writeReplace();
        java.lang.Object readResolve();
}

-dontwarn org.jetbrains.kotlin.fir.**

# used in commonizer
-keep class com.intellij.util.SmartFMap {
    public static ** emptyMap();
    public ** plus(java.lang.Object, java.lang.Object);
    public ** plusAll(java.util.Map);
}

# These classes is needed for test framework
-keep class com.intellij.openapi.util.text.StringUtil { *; }
-keepclassmembers class com.intellij.openapi.util.io.NioFiles {
    public static void deleteRecursively(java.nio.file.Path);
}


# This is used from standalone analysis API, which is NOT a part of the compiler but is bundled into kotlin-annotation-processing.
-keepclassmembers class com.intellij.openapi.vfs.VirtualFileManager {
    com.intellij.openapi.vfs.VirtualFile findFileByNioPath(java.nio.file.Path);
}
-keepclassmembers class com.intellij.openapi.application.Application {
    void addApplicationListener(com.intellij.openapi.application.ApplicationListener, com.intellij.openapi.Disposable);
}
-keepclassmembers class com.intellij.openapi.extensions.ExtensionPointName {
    java.util.List getExtensionList(com.intellij.openapi.extensions.AreaInstance);
}
-keepclassmembers class kotlinx.collections.immutable.ExtensionsKt {
    kotlinx.collections.immutable.PersistentMap toPersistentHashMap(java.util.Map);
    kotlinx.collections.immutable.PersistentMap persistentHashMapOf(kotlin.Pair[]);
    kotlinx.collections.immutable.PersistentSet persistentHashSetOf(java.lang.Object[]);
}
-keepclassmembers class kotlinx.collections.immutable.PersistentMap {
    public *;
}
-keepclassmembers class kotlinx.collections.immutable.PersistentSet {
    public *;
}
-keepclassmembers class com.intellij.lang.jvm.JvmParameter {
    com.intellij.lang.jvm.types.JvmType getType();
}
-keepclassmembers class com.intellij.util.containers.ContainerUtil {
    public static java.util.concurrent.ConcurrentMap createConcurrentSoftMap();
    public static java.util.Map createSoftValueMap();
}
-keep class com.intellij.codeInsight.PsiEquivalenceUtil {
    public static boolean areElementsEquivalent(com.intellij.psi.PsiElement, com.intellij.psi.PsiElement);
}
-keepclassmembers class com.intellij.util.indexing.FileContentImpl {
    public static com.intellij.util.indexing.FileContent createByFile(com.intellij.openapi.vfs.VirtualFile);
}
# Uses a ClassLoader method from JDK 9+
-dontwarn org.jetbrains.kotlin.buildtools.internal.ClassLoaderUtilsKt
