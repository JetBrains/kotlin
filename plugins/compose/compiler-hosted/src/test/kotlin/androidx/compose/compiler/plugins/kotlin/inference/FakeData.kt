/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.inference

val data = mapOf(
    "identity" to fresh { t ->
        Function(
            "identity",
            typeParameters = listOf(t),
            parameters = listOf(Parameter("value", t)),
            result = t
        )
    },
    "mark" to Function("mark", annotations = composable + uiTarget),
    "run" to fresh { t ->
        Function(
            "run",
            typeParameters = listOf(t),
            parameters = listOf(
                Parameter(
                    name = "value",
                    type = t
                ),
                Parameter(
                    name = "block",
                    type = FunctionType(
                        name = "<anonymous>",
                        parameters = listOf(Parameter("it", t)),
                        result = t
                    )
                )
            ),
            result = t
        )
    },
    "useRun" to Function(
        "useRun",
        annotations = composable,
        parameters = listOf(
            Parameter("content", composableLambda())
        ),
        body = listOf(
            call("mark"),
            call("run",
                Ref("content"),
                Lambda(
                    type = FunctionType(
                        name = "<lambda>",
                        annotations = composable,
                        parameters = listOf(
                            Parameter(
                                name = "content",
                                type = composableLambda()
                            )
                        )
                    ),
                    body = listOf(
                        call("content")
                    )
                )
            )
        )
    ),
    "useIdentity" to Function(
        "useIdentity",
        annotations = composable,
        parameters = listOf(
            Parameter("content", composableLambda())
        ),
        body = listOf(
            Call(Call(Ref("identity"), listOf(Ref("content"))))
        )
    ),
    "Drawing" to Function(
        "Drawing",
        annotations = composable + uiTarget,
        parameters = listOf(
            Parameter(
                "content",
                FunctionType("lambda", annotations = composable + vectorTarget)
            )
        )
    ),
    "useVarAndIdentity" to Function(
        "useVarAndIdentity",
        annotations = composable,
        parameters = listOf(
            Parameter("content", composableLambda()),
            Parameter("image", composableLambda())
        ),
        body = listOf(
            Variable("tmp", call("identity", Ref("content"))),
            Variable("tmp2", call("identity", Ref("tmp"))),
            call("tmp2"),
            Variable("image_tmp", call("identity", Ref("image"))),
            call("Drawing", Ref("image_tmp"))
        )
    ),
    "useVar" to Function(
        "useVar",
        annotations = composable,
        parameters = listOf(
            Parameter("content", composableLambda())
        ),
        body = listOf(
            Variable("tmp", Ref("content")),
            Call(Ref("tmp"))
        )
    ),
    "Layout/1" to Function("Layout/1", annotations = composable + uiTarget),
    "Layout/2" to Function(
        "Layout/2",
        annotations = composable + uiTarget,
        parameters = listOf(
            Parameter(
                "content",
                FunctionType("lambda", annotations = composable + uiTarget)
            )
        )
    ),
    "Vector/1" to Function("Vector/1", annotations = composable + vectorTarget),
    "Vector/2" to Function(
        "Vector/2",
        annotations = composable + vectorTarget,
        parameters = listOf(
            Parameter(
                "content",
                FunctionType("lambda", annotations = composable + vectorTarget)
            )
        )
    ),
    "CoreText" to Function(
        "CoreText",
        annotations = composable,
        body = listOf(
            call("Layout/1")
        )
    ),
    "BasicText" to Function(
        "BasicText",
        annotations = composable,
        body = listOf(
            call("CoreText")
        )
    ),
    "Text" to Function(
        "Text",
        annotations = composable,
        body = listOf(
            call("BasicText")
        )
    ),
    "Circle" to Function(
        "Circle",
        annotations = composable,
        body = listOf(
            call("Vector/1")
        )
    ),
    "Square" to Function(
        "Square",
        annotations = composable,
        body = listOf(
            call("Vector/1")
        )
    ),
    "Provider" to Function(
        "Provider",
        annotations = composable,
        parameters = listOf(Parameter("content", composableLambda())),
        body = listOf(
            call("content")
        )
    ),
    "Row" to Function(
        "Row",
        annotations = composable,
        parameters = listOf(Parameter("content", composableLambda())),
        body = listOf(
            call("Layout/2", Ref("content"))
        )
    ),
    "Button" to Function(
        "Button",
        annotations = composable,
        parameters = listOf(Parameter("content", composableLambda())),
        body = listOf(
            call("Row", Ref("content"))
        )
    ),
    "Layer" to Function(
        "Layer",
        annotations = composable,
        parameters = listOf(Parameter("content", composableLambda())),
        body = listOf(
            call("Vector/2", Ref("content"))
        )
    ),

    "SimpleOpen" to Function("SimpleOpen", annotations = composable),

    "OpenRecursive" to Function(
        "OpenRecursive",
        annotations = composable,
        body = listOf(
            call("OpenRecursive")
        )
    ),

    "ClosedRecursive" to Function(
        "ClosedRecursive",
        annotations = composable,
        body = listOf(
            call("ClosedRecursive"),
            call("Text")
        )
    ),

    "ClosedIndirectRecursive" to Function(
        "ClosedIndirectRecursive",
        annotations = composable,
        body = listOf(
            call("ClosedIndirectRecursiveRecurse")
        )
    ),

    "ClosedIndirectRecursiveRecurse" to Function(
        "ClosedIndirectRecursiveRecurse",
        annotations = composable,
        body = listOf(
            call("ClosedIndirectRecursive"),
            call("Text")
        )
    ),

    "OpenIndirectRecursive" to Function(
        "OpenIndirectRecursive",
        annotations = composable,
        body = listOf(
            call("OpenIndirectRecursiveRecurse")
        )
    ),

    "OpenIndirectRecursiveRecurse" to Function(
        "OpenIndirectRecursiveRecurse",
        annotations = composable,
        body = listOf(
            call("OpenIndirectRecursive")
        )
    ),

    "p1" to Function(
        "p1",
        annotations = composable,
        body = listOf(
            call("Text")
        )
    ),

    "p2" to Function(
        "p2",
        annotations = composable,
        body = listOf(
            call("Circle")
        )
    ),

    "p3" to Function(
        "p3",
        annotations = composable,
        body = listOf(
            call("Text"),
            call("Text")
        )
    ),

    "p4" to Function(
        "p4",
        annotations = composable,
        body = listOf(
            call(
                "Row",
                lambda(
                    call("Text"),
                    call("Text")
                )
            )
        )
    ),

    "p5" to Function(
        "p5",
        annotations = composable,
        body = listOf(
            call(
                "Provider",
                lambda(
                    call(
                        "Row",
                        lambda(
                            call("Text"),
                            call(
                                "Button",
                                lambda(
                                    call("Text")
                                )
                            )
                        )
                    )
                )
            )
        )
    ),

    "p6" to Function(
        "p6",
        annotations = composable,
        body = listOf(
            call(
                "Provider",
                lambda(
                    call("Circle"),
                    call("Square")
                )
            )
        )
    ),

    "p7" to Function(
        "p7",
        annotations = composable,
        body = listOf(
            call(
                "Row",
                lambda(
                    call(
                        "Drawing",
                        lambda(
                            call("Circle"),
                            call("Square"),
                            call(
                                "Layer",
                                lambda(
                                    call("Circle")
                                )
                            )
                        )
                    )
                )
            )
        )
    ),

    "p8" to Function(
        "p8",
        annotations = composable,
        body = listOf(
            call(
                "Provider",
                lambda(
                    call(
                        "Row",
                        lambda(
                            call(
                                "Drawing",
                                lambda(
                                    call(
                                        "Provider",
                                        lambda(
                                            call("Circle"),
                                            call("Square")
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    ),

    "p9" to Function(
        "p9",
        annotations = composable,
        parameters = listOf(
            Parameter(
                "content",
                FunctionType(
                    "lambda",
                    annotations = composable + openTarget(0),
                    parameters = listOf(
                        Parameter(
                            "content",
                            FunctionType(
                                "lambda",
                                annotations = composable + openTarget(0)
                            )
                        )
                    )
                )
            )
        ),
        body = listOf(
            call(
                "content",
                lambda(
                    call("Text")
                )
            )
        )
    ),

    "e1" to Function(
        "e1",
        annotations = composable,
        body = listOf(
            call("Text"),
            call("Circle")
        )
    ),

    "e2" to Function(
        "e2",
        annotations = composable,
        body = listOf(
            call(
                "Provider",
                lambda(
                    call("Text")
                )
            ),
            call(
                "Provider",
                lambda(
                    call("Circle")
                )
            )
        )
    )
)

fun walkData(visitor: Visitor) {
    for (function in data.values) {
        walkChildren(function, RecursiveVisitor(visitor))
    }
}

fun randomlyWalkData(visitor: Visitor) {
    val nodes = mutableListOf<Node>()
    for (function in data.values) {
        walk(
            function,
            visitor = object : Visitor {
                override fun visit(annotation: Annotation) {
                    nodes.add(annotation)
                }

                override fun visit(lambda: Lambda) {
                    nodes.add(lambda)
                    walkChildren(lambda, this)
                }

                override fun visit(call: Call) {
                    nodes.add(call)
                    walkChildren(call, this)
                }

                override fun visit(ref: Ref) {
                    nodes.add(ref)
                }

                override fun visit(variable: Variable) {
                    nodes.add(variable)
                    walkChildren(variable, this)
                }

                override fun visit(function: Function) {
                    nodes.add(function)
                    walkChildren(function, this)
                }

                override fun visit(parameter: Parameter) {
                    nodes.add(parameter)
                }
            }
        )
    }
    nodes.shuffle()
    nodes.forEach { walk(it, visitor) }
}

private var freshNumber = 0

fun <T> fresh(block: (t: OpenType) -> T) =
    block(OpenType("fresh-${freshNumber++}"))
