package org.jetbrains.k2js.test;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Pavel Talanov
 */
public final class KotlinLibTest extends TranslationTest {

    final private static String MAIN = "kotlinLib/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    private void runPropertyTypeCheck(String objectName, Map<String, Class<? extends Scriptable>> propertyToType)
            throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath()), new RhinoPropertyTypesChecker(objectName, propertyToType));
    }


    public void testKotlinJsLibRunsWithRhino() throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath()), new RhinoResultChecker() {
            @Override
            public void runChecks(Context context, Scriptable scope) throws Exception {
                //do nothing
            }
        });
    }

    //TODO: refactor
    public void testCreatedTraitIsJSObject() throws Exception {
        final Map<String, Class<? extends Scriptable>> propertyToType
                = new HashMap<String, Class<? extends Scriptable>>();
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("trait.js")),
                new RhinoPropertyTypesChecker("foo", propertyToType));
    }


    public void testCreatedNamespaceIsJSObject() throws Exception {
        final Map<String, Class<? extends Scriptable>> propertyToType
                = new HashMap<String, Class<? extends Scriptable>>();
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("namespace.js")),
                new RhinoPropertyTypesChecker("foo", propertyToType));
    }

    //
    // TODO:Refactor calls to function result checker with test
    public void testNamespaceHasDeclaredFunction() throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("namespace.js")),
                new RhinoFunctionResultChecker("test", true));
    }


    public void testNamespaceHasDeclaredClasses() throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("namespaceWithClasses.js")),
                new RhinoFunctionResultChecker("test", true));
    }


    public void testIsSameType() throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("isSameType.js")),
                new RhinoFunctionResultChecker("test", true));
    }


    public void testIsAncestorType() throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("isAncestorType.js")),
                new RhinoFunctionResultChecker("test", true));
    }


    public void testIsComplexTest() throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("isComplexTest.js")),
                new RhinoFunctionResultChecker("test", true));
    }


    public void testCommaExpression() throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("commaExpression.js")),
                new RhinoFunctionResultChecker("test", true));
    }


    public void testArray() throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("array.js")),
                new RhinoFunctionResultChecker("test", true));
    }


    public void testHashMap() throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("hashMap.js")),
                new RhinoFunctionResultChecker("test", true));
    }

}
