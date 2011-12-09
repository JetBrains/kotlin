package org.jetbrains.k2js.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.Severity;
import org.jetbrains.jet.lang.diagnostics.UnresolvedReferenceDiagnostic;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacade;
import org.jetbrains.jet.util.slicedmap.ReadOnlySlice;
import org.jetbrains.jet.util.slicedmap.SlicedMap;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

/**
 * @author abreslav
 */
public class JetTestUtils {
    public static final BindingTrace DUMMY_TRACE = new BindingTrace() {


        @Override
        public BindingContext getBindingContext() {
            return new BindingContext() {

                @Override
                public Collection<Diagnostic> getDiagnostics() {
                    throw new UnsupportedOperationException(); // TODO
                }

                @Override
                public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
                    return DUMMY_TRACE.get(slice, key);
                }

                @NotNull
                @Override
                public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
                    return DUMMY_TRACE.getKeys(slice);
                }
            };
        }

        @Override
        public <K, V> void record(WritableSlice<K, V> slice, K key, V value) {
        }

        @Override
        public <K> void record(WritableSlice<K, Boolean> slice, K key) {
        }

        @Override
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            if (slice == BindingContext.PROCESSED) return (V) Boolean.FALSE;
            return SlicedMap.DO_NOTHING.get(slice, key);
        }

        @NotNull
        @Override
        public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
            assert slice.isCollective();
            return Collections.emptySet();
        }

        @Override
        public void report(@NotNull Diagnostic diagnostic) {
            if (diagnostic instanceof UnresolvedReferenceDiagnostic) {
                UnresolvedReferenceDiagnostic unresolvedReferenceDiagnostic = (UnresolvedReferenceDiagnostic) diagnostic;
                throw new IllegalStateException("Unresolved: " + unresolvedReferenceDiagnostic.getPsiElement().getText());
            }
        }
    };

    public static BindingTrace DUMMY_EXCEPTION_ON_ERROR_TRACE = new BindingTrace() {
        @Override
        public BindingContext getBindingContext() {
            return new BindingContext() {
                @Override
                public Collection<Diagnostic> getDiagnostics() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
                    return DUMMY_EXCEPTION_ON_ERROR_TRACE.get(slice, key);
                }

                @NotNull
                @Override
                public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
                    return DUMMY_EXCEPTION_ON_ERROR_TRACE.getKeys(slice);
                }
            };
        }

        @Override
        public <K, V> void record(WritableSlice<K, V> slice, K key, V value) {
        }

        @Override
        public <K> void record(WritableSlice<K, Boolean> slice, K key) {
        }

        @Override
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            return null;
        }

        @NotNull
        @Override
        public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
            assert slice.isCollective();
            return Collections.emptySet();
        }

        @Override
        public void report(@NotNull Diagnostic diagnostic) {
            if (diagnostic.getSeverity() == Severity.ERROR) {
                throw new IllegalStateException(diagnostic.getMessage());
            }
        }
    };

    public static BindingContext analyzeNamespace(@NotNull JetNamespace namespace, @NotNull JetControlFlowDataTraceFactory flowDataTraceFactory) {
        return AnalyzerFacade.analyzeOneNamespaceWithJavaIntegration(namespace, flowDataTraceFactory);
    }


//    public static JetCoreEnvironment createEnvironmentWithMockJdk(Disposable disposable) {
//        JetCoreEnvironment environment = new JetCoreEnvironment(disposable);
//        final File rtJar = new File(JetTestCaseBuilder.getHomeDirectory(), "compiler/testData/mockJDK-1.7/jre/lib/rt.jar");
//        environment.addToClasspath(rtJar);
//        environment.addToClasspath(new File(JetTestCaseBuilder.getHomeDirectory(), "compiler/testData/mockJDK-1.7/jre/lib/annotations.jar"));
//        return environment;
//    }


    public static void mkdirs(File file) throws IOException {
        if (file.isDirectory()) {
            return;
        }
        if (!file.mkdirs()) {
            throw new IOException();
        }
    }

    public static void rmrf(File file) {
        if (file != null) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    rmrf(child);
                }
            }
            file.delete();
        }
    }

    public static void recreateDirectory(File file) throws IOException {
        rmrf(file);
        mkdirs(file);
    }


}
