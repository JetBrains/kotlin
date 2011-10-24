/*
 * @author max
 */
package jet;

import jet.typeinfo.TypeInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;

public abstract class Function0<R> extends DefaultJetObject {
    protected Function0(TypeInfo typeInfo) {
        super(typeInfo);
    }

    public abstract R invoke();

    @Override
    public String toString() {
        return "{() : R}";
    }

//    public static void main(String[] args) throws IOException {
//        for(int i = 1; i <= 22; i++) {
//            PrintStream out = new PrintStream(new File("/Development/Dev.Git/jet/stdlib/src/jet/Function" + i + ".java"));
//            out.print("/*\n" +
//                      " * @author alex.tkachman\n" +
//                      " */\n" +
//                      "package jet;\n" +
//                      "\n" +
//                      "import jet.typeinfo.TypeInfo;\n" +
//                      "public abstract class Function" + i + "<");
//            for(int k = 1; k <= i; k++)
//                out.print("D" + k + ", ");
//            out.println("R> extends DefaultJetObject {\n" +
//                        "    protected Function" + i + "(TypeInfo<?> typeInfo) {\n" +
//                        "        super(typeInfo);\n" +
//                        "    }");
//
//            out.print("\n    public abstract R invoke(");
//            for(int k = 1; k <= i; k++)
//                out.print("D" + k + " d" + k + (k != i ? ", " : ");\n\n"));
//
//            out.println("    @Override\n" +
//                        "    public String toString() {");
//            out.print("      return \"{(");
//            for(int k = 1; k <= i; k++)
//                out.print("d" + k + ": D" + k + (k != i ? ", " : ") : R)}\";\n"));
//            out.println("    }");
//
//            out.println("}\n");
//            out.close();
//        }
//
//        for(int i = 1; i <= 22; i++) {
//            PrintStream out = new PrintStream(new File("/Development/Dev.Git/jet/stdlib/src/jet/ExtensionFunction" + i + ".java"));
//            out.print("/*\n" +
//                      " * @author alex.tkachman\n" +
//                      " */\n" +
//                      "package jet;\n" +
//                      "\n" +
//                      "import jet.typeinfo.TypeInfo;\n" +
//                      "public abstract class ExtensionFunction" + i + "<E, ");
//            for(int k = 1; k <= i; k++)
//                out.print("D" + k + ", ");
//            out.println("R> extends DefaultJetObject {\n" +
//                        "    protected ExtensionFunction" + i + "(TypeInfo<?> typeInfo) {\n" +
//                        "        super(typeInfo);\n" +
//                        "    }");
//
//            out.print("\n    public abstract R invoke(E receiver, ");
//            for(int k = 1; k <= i; k++)
//                out.print("D" + k + " d" + k + (k != i ? ", " : ");\n\n"));
//
//            out.println("    @Override\n" +
//                        "    public String toString() {");
//            out.print("      return \"{E.(");
//            for(int k = 1; k <= i; k++)
//                out.print("d" + k + ": D" + k + (k != i ? ", " : ") : R)}\";\n"));
//            out.println("    }");
//
//            out.println("}\n");
//            out.close();
//        }
//    }
}
