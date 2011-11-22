package org.jetbrains.k2js.test;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Talanov Pavel
 */
public class KotlinLibTest extends IncludeLibraryTest {

    final private static String MAIN = "kotlinLib/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    private void runPropertyTypeCheck(String objectName, Map<String, Class<? extends Scriptable>> propertyToType)
            throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath()), new RhinoPropertyTypesChecker(objectName, propertyToType));
    }

    @Test
    public void kotlinJsLibRunsWithRhino() throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath()), new RhinoResultChecker() {
            @Override
            public void runChecks(Context context, Scriptable scope) throws Exception {
                //do nothing
            }
        });
    }

    @Test
    public void classObjectHasCreateMethod() throws Exception {
        final Map<String, Class<? extends Scriptable>> propertyToType
                = new HashMap<String, Class<? extends Scriptable>>();
        propertyToType.put("create", Function.class);
        runPropertyTypeCheck("Class", propertyToType);
    }

    @Test
    public void namespaceObjectHasCreateMethod() throws Exception {
        final Map<String, Class<? extends Scriptable>> propertyToType
                = new HashMap<String, Class<? extends Scriptable>>();
        propertyToType.put("create", Function.class);
        runPropertyTypeCheck("Namespace", propertyToType);
    }

    @Test
    public void traitObjectHasCreateMethod() throws Exception {
        final Map<String, Class<? extends Scriptable>> propertyToType
                = new HashMap<String, Class<? extends Scriptable>>();
        propertyToType.put("create", Function.class);
        runPropertyTypeCheck("Trait", propertyToType);
    }

    @Test
    public void createdTraitIsJSObject() throws Exception {
        final Map<String, Class<? extends Scriptable>> propertyToType
                = new HashMap<String, Class<? extends Scriptable>>();
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("trait.js")),
                new RhinoPropertyTypesChecker("foo", propertyToType));
    }

    @Test
    public void createdNamespaceIsJSObject() throws Exception {
        final Map<String, Class<? extends Scriptable>> propertyToType
                = new HashMap<String, Class<? extends Scriptable>>();
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("namespace.js")),
                new RhinoPropertyTypesChecker("foo", propertyToType));
    }

    @Test
    public void namespaceHasDeclaredFunction() throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("namespace.js")),
                new RhinoFunctionResultChecker("test", true));
    }


    @Test
    public void namespaceHasDeclaredClasses() throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("namespaceWithClasses.js")),
                new RhinoFunctionResultChecker("test", true));
    }

    @Test
    public void isSameType() throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("isSameType.js")),
                new RhinoFunctionResultChecker("test", true));
    }

    @Test
    public void isAncestorType() throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("isAncestorType.js")),
                new RhinoFunctionResultChecker("test", true));
    }

    @Test
    public void isComplexTest() throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("isComplexTest.js")),
                new RhinoFunctionResultChecker("test", true));
    }


}
