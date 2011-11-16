package org.jetbrains.k2js.test;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private void runPropertyTypeCheck(String objectName, Map<String, Class<? extends Scriptable>> propertyToType)
            throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath()), new RhinoPropertyTypesChecker(objectName, propertyToType));
    }

    @Test
    public void kotlinJsLibRunsWithRhino() throws Exception {
        Context context = Context.enter();
        Scriptable scope = context.initStandardObjects();
        runFileWithRhino(kotlinLibraryPath(), context, scope);
        Context.exit();
    }

    @Test
    public void classObjectHasCreateMethod() throws Exception {
        String objectName = "Class";
        final Map<String, Class<? extends Scriptable>> propertyToType
                = new HashMap<String, Class<? extends Scriptable>>();
        propertyToType.put("create", Function.class);
        runPropertyTypeCheck(objectName, propertyToType);
    }


}
