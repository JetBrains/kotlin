package org.jetbrains.k2js.test;

import com.intellij.openapi.project.Project;
import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.compiler.JetCoreEnvironment;

/**
 * @author Pavel Talanov
 */
public abstract class BaseTest extends UsefulTestCase {
    @NonNls
    protected JetCoreEnvironment myEnvironment;

    public Project getProject() {
        return myEnvironment.getProject();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // createEnvironmentWithMockJdk();
    }

    @Override
    protected void tearDown() throws Exception {
        myEnvironment = null;
        super.tearDown();
    }

    protected void createEnvironmentWithMockJdk() {
        myEnvironment = JetTestUtils.createEnvironmentWithMockJdk(getTestRootDisposable());
    }
}
