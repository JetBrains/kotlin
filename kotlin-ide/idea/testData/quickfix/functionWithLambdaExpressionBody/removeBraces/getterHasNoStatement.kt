// "Remove braces" "false"
// TOOL: org.jetbrains.kotlin.idea.inspections.FunctionWithLambdaExpressionBodyInspection
// ACTION: Convert property getter to initializer
// ACTION: Convert to block body
// ACTION: Convert to multi-line lambda
// ACTION: Convert to run { ... }
// ACTION: Enable a trailing comma by default in the formatter
// ACTION: Specify explicit lambda signature
// ACTION: Specify explicit lambda signature
// ACTION: Specify type explicitly
// ACTION: Specify type explicitly
val test get() = <caret>{}