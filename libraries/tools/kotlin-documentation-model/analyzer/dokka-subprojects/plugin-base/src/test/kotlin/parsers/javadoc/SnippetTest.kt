/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package parsers.javadoc

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.JavaClassReference
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.doc.B
import org.jetbrains.dokka.model.doc.CodeBlock
import org.jetbrains.dokka.model.doc.DocumentationLink
import org.jetbrains.dokka.model.doc.I
import org.jetbrains.dokka.model.doc.Mark
import org.jetbrains.dokka.model.doc.P
import org.jetbrains.dokka.model.doc.Text
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SnippetTest : BaseAbstractTest() {
    private val testDataDir = getTestDataDir("parsers/javadoc").toAbsolutePath()

    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
            }
        }
    }

    private val snippetPathConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
                samples = listOf(
                    Paths.get("$testDataDir/snippets").toString(),
                )
            }
        }
    }

    @Test
    fun `files in snippet-files directory are excluded from documentation`() {
        testInline(
            """
            |/src/main/java/example/Visible.java
            |package example;
            |public class Visible {}
            |
            |/src/main/java/example/snippet-files/Hidden.java
            |package example;
            |public class Hidden {}
            """.trimMargin(),
            configuration
        ) {
            documentablesCreationStage = { modules: List<DModule> ->
                val classNames = modules
                    .flatMap { it.packages }
                    .flatMap { it.classlikes }
                    .mapNotNull { it.name }
                    .toSet()

                assertTrue { "Visible" in classNames }
                assertFalse("Documentation should not be generated for files inside `snippet-files` directory") { "Hidden" in classNames }
            }
        }
    }

    @Test
    fun `plain inline snippet`() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example
            |
            | /**
            | * {@snippet :
            | * private void snippet() {
            | *     Test test = new Test();
            | *     System.out.println("test");
            | * }}
            | */
            | public class Test {}
            """.trimMargin(),
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocRoot(modules)

                assertEquals(
                    listOf(
                        CodeBlock(
                            children = listOf(
                                Text(
                                    """
                                    |private void snippet() {
                                    |    Test test = new Test();
                                    |    System.out.println("test");
                                    |}
                                    """.trimMargin()
                                )
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun `external snippet from snippet-files using file attribute`() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example;
            |
            | /**
            | * {@snippet file="MySnippet.java"}
            | */
            | public class Test {}
            |
            |/src/main/java/example/snippet-files/MySnippet.java
            |private void exampleMethod() {
            |    String message = "Hello from external snippet";
            |    System.out.println(message);
            |}
            """.trimMargin(),
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocRoot(modules)

                assertEquals(
                    listOf(
                        CodeBlock(
                            children = listOf(
                                Text(
                                    """
                                    |private void exampleMethod() {
                                    |    String message = "Hello from external snippet";
                                    |    System.out.println(message);
                                    |}
                                    """.trimMargin()
                                )
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun `external snippet from snippet-files using class attribute`() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example;
            |
            | /**
            | * {@snippet class="CalculatorSnippet"}
            | */
            | public class Test {}
            |
            |/src/main/java/example/snippet-files/CalculatorSnippet.java
            |public class CalculatorSnippet {
            |    public int add(int a, int b) {
            |        return a + b;
            |    }
            |}
            """.trimMargin(),
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocRoot(modules)

                assertEquals(
                    listOf(
                        CodeBlock(
                            children = listOf(
                                Text(
                                    """
                                    |public class CalculatorSnippet {
                                    |    public int add(int a, int b) {
                                    |        return a + b;
                                    |    }
                                    |}
                                    """.trimMargin()
                                )
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun `external snippet from snippet-files with region using file attribute and @end with region name`() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example;
            |
            | /**
            | * {@snippet file="RegionExample.java" region="calculation"}
            | */
            | public class Test {}
            |
            |/src/main/java/example/snippet-files/RegionExample.java
            |public class RegionExample {
            |    // @start region="calculation"
            |    int result = multiply(5, 3);
            |    // @end region="calculation"
            |
            |    private int multiply(int x, int y) {
            |        return x * y;
            |    }
            |}
            """.trimMargin(),
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocRoot(modules)

                assertEquals(
                    listOf(
                        CodeBlock(
                            children = listOf(
                                Text(
                                    """
                                    |int result = multiply(5, 3);
                                    """.trimMargin()
                                )
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun `external snippet from snippet-files with region using class attribute and anonymous @end`() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example;
            |
            | /**
            | * {@snippet class="AnonymousRegionExample" region="process"}
            | */
            | public class Test {}
            |
            |/src/main/java/example/snippet-files/AnonymousRegionExample.java
            |public class AnonymousRegionExample {
            |    // @start region="process"
            |    String data = "test";
            |    data = data.toUpperCase();
            |    // @end
            |}
            """.trimMargin(),
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocRoot(modules)

                assertEquals(
                    listOf(
                        CodeBlock(
                            children = listOf(
                                Text(
                                    """
                                    |String data = "test";
                                    |data = data.toUpperCase();
                                    """.trimMargin()
                                )
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun `external snippet from snippet-path using file attribute`() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example;
            |
            | /**
            | * {@snippet file="WholeFileSnippet.java"}
            | */
            | public class Test {}
            """.trimMargin(),
            snippetPathConfiguration,
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocRoot(modules)

                assertEquals(
                    listOf(
                        CodeBlock(
                            children = listOf(
                                Text(
                                    """
                                    |/*
                                    | * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
                                    | */
                                    |
                                    |public class WholeFileSnippet {
                                    |    public static void processData() {
                                    |        List<String> items = new ArrayList<>();
                                    |        items.add("item1");
                                    |        items.add("item2");
                                    |    }
                                    |}
                                    """.trimMargin()
                                )
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun `external snippet from snippet-path using class attribute`() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example;
            |
            | /**
            | * {@snippet class="WholeFileSnippet"}
            | */
            | public class Test {}
            """.trimMargin(),
            snippetPathConfiguration,
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocRoot(modules)

                assertEquals(
                    listOf(
                        CodeBlock(
                            children = listOf(
                                Text(
                                    """
                                    |/*
                                    | * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
                                    | */
                                    |
                                    |public class WholeFileSnippet {
                                    |    public static void processData() {
                                    |        List<String> items = new ArrayList<>();
                                    |        items.add("item1");
                                    |        items.add("item2");
                                    |    }
                                    |}
                                    """.trimMargin()
                                )
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun `external snippet from snippet-path with region using file attribute and @end with region name`() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example;
            |
            | /**
            | * {@snippet file="ExternalSnippets.java" region="configSetup"}
            | */
            | public class Test {}
            """.trimMargin(),
            snippetPathConfiguration,
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocRoot(modules)

                assertEquals(
                    listOf(
                        CodeBlock(
                            children = listOf(
                                Text(
                                    """
                                    |Config config = new Config();
                                    |config.setEnabled(true);
                                    """.trimMargin()
                                )
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun `external snippet from snippet-path with region using class attribute and anonymous @end`() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example;
            |
            | /**
            | * {@snippet class="ExternalSnippets" region="validation"}
            | */
            | public class Test {}
            """.trimMargin(),
            snippetPathConfiguration,
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocRoot(modules)

                assertEquals(
                    listOf(
                        CodeBlock(
                            children = listOf(
                                Text(
                                    """
                                    |if (value != null && value.length() > 0) {
                                    |    return true;
                                    |}
                                    """.trimMargin()
                                )
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun `properties snippets from different sources with lang attribute`() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example;
            |
            | /**
            | * Inline properties snippet:
            | * {@snippet lang="properties" :
            | * # inline properties
            | * app.name=MyApp
            | * app.version=1.0.0
            | * }
            | *
            | * External snippet from snippet-files:
            | * {@snippet lang="properties" file="app.properties"}
            | *
            | * External snippet from snippet-path:
            | * {@snippet lang="properties" file="config.properties" region="config"}
            | */
            | public class Test {}
            |
            |/src/main/java/example/snippet-files/app.properties
            |# snippet-files properties
            |db.host=localhost
            |db.port=5432
            """.trimMargin(),
            snippetPathConfiguration,
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocRoot(modules)

                assertEquals(
                    listOf(
                        P(
                            children = listOf(
                                Text("Inline properties snippet: ")
                            )
                        ),
                        CodeBlock(
                            children = listOf(
                                Text(
                                    """
                                    |# inline properties
                                    |app.name=MyApp
                                    |app.version=1.0.0
                                    """.trimMargin()
                                )
                            ),
                            params = mapOf("lang" to "properties")
                        ),
                        Text(" External snippet from snippet-files: "),
                        CodeBlock(
                            children = listOf(
                                Text(
                                    """
                                    |# snippet-files properties
                                    |db.host=localhost
                                    |db.port=5432
                                    """.trimMargin()
                                )
                            ),
                            params = mapOf("lang" to "properties")
                        ),
                        Text(" External snippet from snippet-path: "),
                        CodeBlock(
                            children = listOf(
                                Text(
                                    """
                                    |local.timezone=PST
                                    |local.zip=94123
                                    |local.area-code=415
                                    """.trimMargin()
                                )
                            ),
                            params = mapOf("lang" to "properties")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun `kotlin snippets from different sources with lang attribute`() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example;
            |
            | /**
            | * Inline Kotlin snippet:
            | * {@snippet lang="kotlin" :
            | * fun greet(name: String) {
            | *     println("Hello, ${'$'}name!")
            | * }
            | * }
            | *
            | * External snippet from snippet-files:
            | * {@snippet lang="kotlin" file="Extensions.kt"}
            | *
            | * External snippet from snippet-path:
            | * {@snippet lang="kotlin" file="KotlinSnippets.kt" region="dataClass"}
            | */
            | public class Test {}
            |
            |/src/main/java/example/snippet-files/Extensions.kt
            |// Extension function
            |fun String.capitalize(): String {
            |    return this.replaceFirstChar { it.uppercase() }
            |}
            """.trimMargin(),
            snippetPathConfiguration,
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocRoot(modules)

                assertEquals(
                    listOf(
                        P(
                            children = listOf(
                                Text("Inline Kotlin snippet: ")
                            )
                        ),
                        CodeBlock(
                            children = listOf(
                                Text(
                                    """
                                    |fun greet(name: String) {
                                    |    println("Hello, ${'$'}name!")
                                    |}
                                    """.trimMargin()
                                )
                            ),
                            params = mapOf("lang" to "kotlin")
                        ),
                        Text(" External snippet from snippet-files: "),
                        CodeBlock(
                            children = listOf(
                                Text(
                                    """
                                    |// Extension function
                                    |fun String.capitalize(): String {
                                    |    return this.replaceFirstChar { it.uppercase() }
                                    |}
                                    """.trimMargin()
                                )
                            ),
                            params = mapOf("lang" to "kotlin")
                        ),
                        Text(" External snippet from snippet-path: "),
                        CodeBlock(
                            children = listOf(
                                Text(
                                    """
                                    |data class User(
                                    |    val name: String,
                                    |    val age: Int
                                    |)
                                    """.trimMargin()
                                )
                            ),
                            params = mapOf("lang" to "kotlin")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun `highlight markup tag`() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example;
            |
            | /**
            | * {@snippet :
            | * public class Example {
            | *     public void method1() { // @highlight substring="public" type="bold"
            | *         String text = "hello";  // @highlight substring="text" type="italic"
            | *         int count = 42;         // @highlight substring="42" type="highlighted"
            | *     }
            | *     // @highlight regex="test[\d]{1}" region
            | *     private void method2() {
            | *         System.out.println("test1");
            | *         System.out.println("test2");
            | *         System.out.println("test3");
            | *     }
            | *     // @end
            | * }}
            | */
            | public class Test {}
            """.trimMargin(),
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocRoot(modules)

                assertEquals(
                    listOf(
                        CodeBlock(
                            children = listOf(
                                Text("public class Example {\n    "),
                                B(children = listOf(Text("public"))),
                                Text(" void method1() {\n        String "),
                                I(children = listOf(Text("text"))),
                                Text(" = \"hello\";\n        int count = "),
                                Mark(children = listOf(Text("42"))),
                                Text(";\n    }\n    private void method2() {\n        System.out.println(\""),
                                B(children = listOf(Text("test1"))),
                                Text("\");\n        System.out.println(\""),
                                B(children = listOf(Text("test2"))),
                                Text("\");\n        System.out.println(\""),
                                B(children = listOf(Text("test3"))),
                                Text("\");\n    }\n}")
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun `replace markup tag`() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example;
            |
            | /**
            | * {@snippet :
            | * public class Example {
            | *     String config = "oldValue"; // @replace substring="oldValue" replacement="newValue"
            | *     System.out.println("Hello World!");  // @replace regex='".*"' replacement="..."
            | *     
            | *     // @replace region="credentials" regex="secret\d+" replacement="***"
            | *     String password2 = "secret321";
            | *     String password1 = "secret123";
            | *     // @end region="credentials"
            | *     
            | *     System.out.println("Hello World!");  // @replace regex='"(.*)"' replacement='"I said, $1"'
            | * }}
            | */
            | public class Test {}
            """.trimMargin(),
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocRoot(modules)

                assertEquals(
                    listOf(
                        CodeBlock(
                            children = listOf(
                                Text(
                                    """
                                    |public class Example {
                                    |    String config = "newValue";
                                    |    System.out.println(...);
                                    |
                                    |    String password2 = "***";
                                    |    String password1 = "***";
                                    |
                                    |    System.out.println("I said, Hello World!");
                                    |}
                                    """.trimMargin()
                                )
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun `link markup tag`() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example;
            |
            |import java.util.List;
            |
            | /**
            | * {@snippet :
            | * // @link substring="System.out" target="System#out" region
            | * System.out.println("Hello World!");
            | * System.out.println("link"); // @link substring="println" target="java.io.PrintStream#println(String)"
            | * 
            | * List<String> items = new ArrayList<>(); // @link regex="\bList" target="List"
            | * items.stream() // @link substring="stream" target="java.util.Collection#stream()"
            | *     .filter(s -> !s.isEmpty())
            | *     .map(String::toUpperCase) // @link substring="String" target="String"
            | *     .forEach(System.out::println); // @end
            | * 
            | * processItems(items); // @link substring="processItems" target="#processItems(List)" 
            | * }
            | */
            | public class Test {
            |     /**  
            |     * process items function 
            |     */  
            |     public void processItems(List<String> items) {}
            | }
            """.trimMargin(),
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocRoot(modules)

                assertEquals(
                    listOf(
                        CodeBlock(
                            children = listOf(
                                DocumentationLink(
                                    dri = DRI(
                                        packageName = "java.lang",
                                        classNames = "System",
                                        callable = Callable(
                                            name = "out",
                                            params = emptyList(),
                                            isProperty = true
                                        )
                                    ),
                                    children = listOf(Text("System.out"))
                                ),
                                Text(".println(\"Hello World!\");\n"),
                                DocumentationLink(
                                    dri = DRI(
                                        packageName = "java.lang",
                                        classNames = "System",
                                        callable = Callable(
                                            name = "out",
                                            params = emptyList(),
                                            isProperty = true
                                        )
                                    ),
                                    children = listOf(Text("System.out"))
                                ),
                                Text("."),
                                DocumentationLink(
                                    dri = DRI(
                                        packageName = "java.io",
                                        classNames = "PrintStream",
                                        callable = Callable(
                                            name = "println",
                                            params = listOf(JavaClassReference("java.lang.String"))
                                        ),
                                    ), children = listOf(Text("println"))
                                ),
                                Text("(\"link\");\n\n"),
                                DocumentationLink(
                                    dri = DRI(
                                        packageName = "java.util",
                                        classNames = "List"
                                    ),
                                    children = listOf(Text("List"))
                                ),
                                Text("<String> items = new ArrayList<>();\nitems."),
                                DocumentationLink(
                                    dri = DRI(
                                        packageName = "java.util",
                                        classNames = "Collection",
                                        callable = Callable(
                                            name = "stream",
                                            params = emptyList()
                                        )
                                    ),
                                    children = listOf(Text("stream"))
                                ),
                                Text("()\n    .filter(s -> !s.isEmpty())\n    .map("),
                                DocumentationLink(
                                    dri = DRI(
                                        packageName = "java.lang",
                                        classNames = "String"
                                    ),
                                    children = listOf(Text("String"))
                                ),
                                Text("::toUpperCase)\n    .forEach("),
                                DocumentationLink(
                                    dri = DRI(
                                        packageName = "java.lang",
                                        classNames = "System",
                                        callable = Callable(
                                            name = "out",
                                            params = emptyList(),
                                            isProperty = true
                                        )
                                    ),
                                    children = listOf(Text("System.out"))
                                ),
                                Text("::println);\n\n"),
                                DocumentationLink(
                                    dri = DRI(
                                        packageName = "example",
                                        classNames = "Test",
                                        callable = Callable(
                                            name = "processItems",
                                            params = listOf(JavaClassReference("java.util.List<java.lang.String>"))
                                        )
                                    ),
                                    children = listOf(Text("processItems"))
                                ),
                                Text("(items);")
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun `multiple markup tags at single line`() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example;
            |
            | /**
            | * {@snippet :
            | * String s = "hello".toUpperCase().trim(); // @link substring="toUpperCase" target="String#toUpperCase()" @link substring="trim" target="String#trim()" @replace substring="hello" replacement="..."
            | * }
            | */
            | public class Test {}
            """.trimMargin(),
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocRoot(modules)

                assertEquals(
                    listOf(
                        CodeBlock(
                            children = listOf(
                                Text("String s = \"...\"."),
                                DocumentationLink(
                                    dri = DRI(
                                        packageName = "java.lang",
                                        classNames = "String",
                                        callable = Callable(
                                            name = "toUpperCase",
                                            params = emptyList()
                                        )
                                    ),
                                    children = listOf(Text("toUpperCase"))
                                ),
                                Text("()."),
                                DocumentationLink(
                                    dri = DRI(
                                        packageName = "java.lang",
                                        classNames = "String",
                                        callable = Callable(
                                            name = "trim",
                                            params = emptyList()
                                        )
                                    ),
                                    children = listOf(Text("trim"))
                                ),
                                Text("();")
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun `markup comment with double colon at the end applies to next line`() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example;
            |
            | /**
            | * {@snippet lang="properties" :
            | * local.timezone=PST
            | * # @highlight regex="[0-9]+" :
            | * local.zip=94123
            | * local.area-code=415
            | * }
            | */
            | public class Test {}
            """.trimMargin(),
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocRoot(modules)

                assertEquals(
                    listOf(
                        CodeBlock(
                            children = listOf(
                                Text("local.timezone=PST\nlocal.zip="),
                                B(children = listOf(Text("94123"))),
                                Text("\nlocal.area-code=415")
                            ),
                            params = mapOf("lang" to "properties")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun `snippet with overlapping regions`() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example;
            |
            | /**
            | * {@snippet file="OverlappingRegions.java" region="snippetBody"}
            | */
            | public class Test {}
            |
            |/src/main/java/example/snippet-files/OverlappingRegions.java
            |package example;
            |
            |public class OverlappingRegions {
            |    public void example() {
            |        // @replace region="replacePassword" regex="secret\d+" replacement="***"
            |        String password1 = "secret123";
            |        // @start region="snippetBody"
            |        System.out.println("Processing:");
            |        // @highlight region="highlightData" substring="data"
            |        String password2 = "secret123";
            |        String data = "important data";
            |        // @end region="replacePassword"
            |        String password3 = "secret123"; // @end region="highlightData"
            |        System.out.println(data);
            |        // @end region="snippetBody"
            |    }
            |}
            """.trimMargin(),
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocRoot(modules)

                assertEquals(
                    listOf(
                        CodeBlock(
                            children = listOf(
                                Text("System.out.println(\"Processing:\");\nString password2 = \"***\";\nString "),
                                B(children = listOf(Text("data"))),
                                Text(" = \"important "),
                                B(children = listOf(Text("data"))),
                                Text("\";\nString password3 = \"secret123\";\nSystem.out.println(data);")
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    @Test
    fun `correct hybrid snippet`() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example;
            |
            | /**
            | * {@snippet file="HybridSnippet.java" region="example" :
            | * String result = "processed";
            | * }
            | */
            | public class Test {}
            |
            |/src/main/java/example/snippet-files/HybridSnippet.java
            |package example;
            |
            |public class HybridSnippet {
            |    public void example() {
            |        // @start region="example"
            |        String result = "data"; // @replace substring="data" replacement="processed"
            |        // @end
            |    }
            |}
            """.trimMargin(),
            configuration
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocRoot(modules)

                assertEquals(
                    listOf(
                        CodeBlock(
                            children = listOf(
                                Text("String result = \"processed\";")
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children
                )

                assertEquals(0, logger.warningsCount)
                assertEquals(0, logger.errorsCount)
            }
        }
    }

    @Test
    fun `incorrect hybrid snippet`() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example;
            |
            | /**
            | * {@snippet file="HybridSnippet.java" region="example" :
            | * String first = "first line is the same"
            | * System.out.println("second line is different")
            | * System.out.println("third line is the same")
            | * }
            | */
            | public class Test {}
            |
            |/src/main/java/example/snippet-files/HybridSnippet.java
            |package example;
            |
            |public class HybridSnippet {
            |    public void example() {
            |        // @start region="example"
            |        String first = "first line is the same"
            |        String second = "second line is different"
            |        System.out.println("third line is the same")
            |        // @end
            |    }
            |}
            """.trimMargin(),
            configuration
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocRoot(modules)

                assertEquals(
                    listOf(
                        CodeBlock(
                            children = listOf(
                                Text(
                                    "String first = \"first line is the same\"\n" +
                                            "System.out.println(\"second line is different\")\n" +
                                            "System.out.println(\"third line is the same\")"
                                )
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children,
                    "inline snippet is returned"
                )

                assertTrue { logger.warningsCount == 1 }

                val warnMessage = logger.warnMessages.first()

                assertEquals(
                    warnMessage,
                    """
                        @snippet (Test.java): inline and external snippets are not the same in the hybrid snippet (after formatting and escaping are applied).
                        diff:
                        line 2:
                        inline: 'System.out.println(&quot;second line is different&quot;)'
                        external: 'String second = &quot;second line is different&quot;'
                    """.trimIndent()
                )
            }
        }
    }

    @Test
    fun `unresolved snippet`() {
        testInline(
            """
            |/src/main/java/example/Test.java
            |package example;
            |
            | /**
            | * {@snippet}
            | */
            | public class Test {}
            """.trimMargin(),
            configuration
        ) {
            documentablesCreationStage = { modules ->
                val root = getFirstClassDocRoot(modules)

                assertEquals(
                    listOf(
                        CodeBlock(
                            children = listOf(
                                Text("// snippet not resolved")
                            ),
                            params = mapOf("lang" to "java")
                        )
                    ),
                    root.children
                )
            }
        }
    }

    private fun getFirstClassDocRoot(modules: List<DModule>) =
        modules.first().packages.first().classlikes.single().documentation.values.first().children.first().root
}
