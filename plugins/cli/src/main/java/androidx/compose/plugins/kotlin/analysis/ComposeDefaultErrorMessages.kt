package androidx.compose.plugins.kotlin.analysis

import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.Renderers
import org.jetbrains.kotlin.diagnostics.rendering.Renderers.RENDER_COLLECTION_OF_TYPES

object ComposeDefaultErrorMessages : DefaultErrorMessages.Extension {
    private val MAP = DiagnosticFactoryToRendererMap("Compose")
    override fun getMap() = MAP

    init {
        MAP.put(
            ComposeErrors.NO_COMPOSER_FOUND,
            "Couldn't find a valid composer."
        )
        MAP.put(
            ComposeErrors.DUPLICATE_ATTRIBUTE,
            "Duplicate attribute; Attributes must appear at most once per tag."
        )
        MAP.put(
            ComposeErrors.OPEN_COMPONENT,
            "Component is open. Components cannot be an open or abstract class."
        )
        MAP.put(
            ComposeErrors.OPEN_MODEL,
            "Model objects cannot be open or abstract"
        )
        MAP.put(
            ComposeErrors.UNSUPPORTED_MODEL_INHERITANCE,
            "Model objects do not support inheritance"
        )
        MAP.put(
            ComposeErrors.MISMATCHED_ATTRIBUTE_TYPE,
            "<html>Type Mismatch.<br/>Required: {1}<br/>Found: {0}</html>",
            Renderers.RENDER_TYPE,
            Renderers.commaSeparated(Renderers.RENDER_TYPE)
        )
        MAP.put(
            ComposeErrors.MISMATCHED_INFERRED_ATTRIBUTE_TYPE,
            "<html>Type Mismatch.<br/>Required: {1}<br/>Found: {0}</html>",
            Renderers.RENDER_TYPE,
            Renderers.commaSeparated(Renderers.RENDER_TYPE)
        )
        MAP.put(
            ComposeErrors.CALLABLE_RECURSION_DETECTED,
            "Recursion detected"
        )
        MAP.put(
            ComposeErrors.AMBIGUOUS_ATTRIBUTES_DETECTED,
            "KTX call targets resulted in ambiguous attributes: {0}",
            Renderers.commaSeparated(Renderers.STRING)
        )
        MAP.put(
            ComposeErrors.INVALID_COMPOSER_IMPLEMENTATION,
            "Composer of type ''{0}'' was found to be an invalid Composer implementation. " +
                    "Reason: {1}",
            Renderers.RENDER_TYPE,
            Renderers.STRING
        )
        MAP.put(
            ComposeErrors.UNRESOLVED_CHILDREN,
            "<html>Mismatched children body type.<br/>Expected: {0}</html>",
            Renderers.commaSeparated(Renderers.RENDER_TYPE)
        )
        MAP.put(
            ComposeErrors.UNRESOLVED_ATTRIBUTE_KEY,
            "No valid attribute on ''{0}'' found with key ''{1}'' and type ''{2}''",
            Renderers.commaSeparated(Renderers.COMPACT),
            Renderers.STRING,
            Renderers.RENDER_TYPE
        )
        MAP.put(
            ComposeErrors.UNRESOLVED_ATTRIBUTE_KEY_UNKNOWN_TYPE,
            "No valid attribute on ''{0}'' found with key ''{1}''",
            Renderers.commaSeparated(Renderers.COMPACT),
            Renderers.STRING
        )
        MAP.put(
            ComposeErrors.MISMATCHED_ATTRIBUTE_TYPE_NO_SINGLE_PARAM_SETTER_FNS,
            "Setters with multiple arguments are currently unsupported. Found: ''{0}''",
            Renderers.COMPACT
        )
        MAP.put(
            ComposeErrors.MISSING_REQUIRED_ATTRIBUTES,
            "Missing required attributes: {0}",
            Renderers.commaSeparated(Renderers.COMPACT)
        )
        MAP.put(
            ComposeErrors.INVALID_TAG_TYPE,
            "Invalid KTX tag type. Found ''{0}'', Expected ''{1}''",
            Renderers.RENDER_TYPE,
            Renderers.commaSeparated(Renderers.RENDER_TYPE)
        )
        MAP.put(
            ComposeErrors.SUSPEND_FUNCTION_USED_AS_SFC,
            "Suspend functions are not allowed to be used as Components"
        )
        MAP.put(
            ComposeErrors.INVALID_TYPE_SIGNATURE_SFC,
            "Only Unit-returning functions are allowed to be used as Components"
        )
        MAP.put(
            ComposeErrors.INVALID_TAG_DESCRIPTOR,
            "Invalid KTX tag type. Expected ''{0}''",
            Renderers.commaSeparated(Renderers.RENDER_TYPE)
        )
        MAP.put(
            ComposeErrors.SVC_INVOCATION,
            "Stateless Functional Components (SFCs) should not be invoked, use <{0} /> " +
                    "syntax instead",
            Renderers.STRING
        )
        MAP.put(
            ComposeErrors.NON_COMPOSABLE_INVOCATION,
            "{0} `{1}` must be marked as @Composable in order to be used as a KTX tag",
            Renderers.STRING,
            Renderers.COMPACT
        )
        MAP.put(
            ComposeErrors.KTX_IN_NON_COMPOSABLE,
            "Functions containing KTX Tags should be marked with the @Composable annotation"
        )
        MAP.put(
            ComposeErrors.COMPOSABLE_INVOCATION_IN_NON_COMPOSABLE,
            "Functions which invoke @Composable functions must be marked with the @Composable annotation"
        )
        MAP.put(
            ComposeErrors.UNRESOLVED_TAG,
            "Unresolved reference: {0}",
            Renderers.ELEMENT_TEXT
        )
        MAP.put(
            ComposeErrors.CHILDREN_ATTR_USED_AS_BODY_AND_KEYED_ATTRIBUTE,
            "Attribute {0} is provided as the body of the element and therefore can't " +
                    "simultaneously be provided as a keyed attribute.",
            Renderers.STRING
        )
        MAP.put(
            ComposeErrors.CHILDREN_PROVIDED_BUT_NO_CHILDREN_DECLARED,
            "Element has a children body provided, but no @Children declarations were found"
        )
        MAP.put(
            ComposeErrors.MISSING_REQUIRED_CHILDREN,
            "A children body of type '{0}' is required",
            Renderers.RENDER_TYPE
        )
        MAP.put(
            ComposeErrors.ILLEGAL_ASSIGN_TO_UNIONTYPE,
            "Value of type {0} can't be assigned to union type {1}.",
            RENDER_COLLECTION_OF_TYPES,
            RENDER_COLLECTION_OF_TYPES
        )
        MAP.put(
            ComposeErrors.AMBIGUOUS_KTX_CALL,
            "Ambiguous targets. {0}",
            Renderers.AMBIGUOUS_CALLS
        )
    }
}