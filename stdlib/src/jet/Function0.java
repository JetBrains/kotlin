/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * @author max
 */
package jet;

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
//                      "import jet.TypeInfo;\n" +
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
//                      "import jet.TypeInfo;\n" +
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
