package transitiveStory.apiJvm.jbeginning;

import playground.SomeUsefulInfoKt;

public class JavaApiContainer {
    private String privateJavaDeclaration = "I'm a private string from `" + SomeUsefulInfoKt.getModuleName() +
            "` and shall be never visible to the others.";

    String packageVisibleJavaDeclaration = "I'm a packag1e visible string from `" + SomeUsefulInfoKt.getModuleName() +
            "` and shall be never visible to the other modules.";

    protected String protectedJavaDeclaration = "I'm a protected string from `" + SomeUsefulInfoKt.getModuleName() +
            "` and shall be never visible to the other modules except my subclasses.";

    public String publicJavaDeclaration = "I'm a public string from `" + SomeUsefulInfoKt.getModuleName() +
            "` and shall be visible to the other modules.";

    public static String publicStaticJavaDeclaration = "I'm a public static string from `" + SomeUsefulInfoKt.getModuleName() +
            "` and shall be visible to the other modules even without instantiation of `JavaApiContainer` class.";
}
