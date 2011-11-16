package org.jetbrains.k2js.test;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * @author Talanov Pavel
 */
public class KotlinLibTest extends TranslationTest {

    final private static String MAIN = "kotlinLib/";

    @Override
    protected List<String> generateFilenameList(String inputFile) {
        return Arrays.asList(kotlinLibraryPath());
    }

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    protected void verifyObjectHasExpectedPropertiesOfExpectedTypes
            (NativeObject object, Map<String, Class<? extends Scriptable>> nameToClassMap) {
        for (Map.Entry<String, Class<? extends Scriptable>> entry : nameToClassMap.entrySet()) {
            String name = entry.getKey();
            Class expectedClass = entry.getValue();
            assertTrue(object + " must contain key " + name, object.containsKey(name));
            assertTrue(object + "'s property " + name + " must be of type " + expectedClass,
                    expectedClass.isInstance(object.get(name)));
        }
    }

    @Test
    public void kotlinJsLibRunsWithRhino() throws Exception {
        Context context = Context.enter();
        Scriptable scope = context.initStandardObjects();
        //       ss
        runFileWithRhino(kotlinLibraryPath(), context, scope);
        Context.exit();
    }

    @Test
    public void classObjectHasCreateMethod() throws Exception {
        final Map<String, Class<? extends Scriptable>> nameToClassMap =
                new HashMap<String, Class<? extends Scriptable>>();
        nameToClassMap.put("create", Function.class);

        runRhinoTest(Arrays.asList(kotlinLibraryPath()),
                new RhinoResultChecker() {
                    @Override
                    public void runChecks(Context context, Scriptable scope) throws Exception {
                        NativeObject object = RhinoUtils.extractObject("Class", scope);
                        verifyObjectHasExpectedPropertiesOfExpectedTypes(object, nameToClassMap);
                    }
                });
    }
}
