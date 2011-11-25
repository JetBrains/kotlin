import junit.framework.Test;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetLiteFixture;


public final class ExampleTestSuite extends JetLiteFixture {
    private String name;

    public ExampleTestSuite(@NonNls String dataPath, String name) {
        this.name = name;
    }

    public String getName() {
        return "test" + name;
    }

    public void runTest() throws Exception {

    }


    public static Test suite() {
        return JetTestCaseBuilder.suiteForDirectory(JetTestCaseBuilder.getTestDataPathBase(),
                "/checkerWithErrorTypes/quick", true, new JetTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name) {
                return (new ExampleTestSuite(dataPath, name));
            }
        });
    }
}
