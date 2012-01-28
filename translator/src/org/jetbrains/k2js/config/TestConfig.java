package org.jetbrains.k2js.config;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.compiler.JetCoreEnvironment;

/**
 * @author Pavel Talanov
 */
public final class TestConfig extends Config {

    @NotNull
    private static JetCoreEnvironment getTestEnvironment() {
        if (testOnlyEnvironment == null) {
            testOnlyEnvironment = new JetCoreEnvironment(new Disposable() {
                @Override
                public void dispose() {
                }
            });
        }
        return testOnlyEnvironment;
    }

    @Nullable
    private static /*var*/ JetCoreEnvironment testOnlyEnvironment = null;

    public TestConfig() {
    }

    @NotNull
    @Override
    public Project getProject() {
        return getTestEnvironment().getProject();
    }
}
