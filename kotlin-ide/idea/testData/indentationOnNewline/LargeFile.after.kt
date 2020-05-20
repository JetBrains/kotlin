/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter.uber

import com.intellij.application.options.CodeStyle
import com.intellij.formatting.*
import com.intellij.formatting.DependentSpacingRule.Anchor
import com.intellij.formatting.DependentSpacingRule.Trigger
import com.intellij.formatting.SpacingBuilder.RuleBuilder
import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.text.TextRangeUtil
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.formatter.CommonAlignmentStrategy
import org.jetbrains.kotlin.idea.formatter.KotlinCommonCodeStyleSettings
import org.jetbrains.kotlin.idea.formatter.uber.NodeIndentStrategy.Companion.strategy
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.parser.KDocElementTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.psi.stubs.elements.KtModifierListElementType
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import kotlin.math.max

abstract class NodeIndentStrategy {

    abstract fun getIndent(node: ASTNode, settings: CodeStyleSettings): Indent?

    class ConstIndentStrategy(private val indent: Indent) : NodeIndentStrategy() {

        override fun getIndent(node: ASTNode, settings: CodeStyleSettings): Indent? {
            return indent
        }
    }

    class PositionStrategy(private val debugInfo: String?) : NodeIndentStrategy() {
        private var indentCallback: (CodeStyleSettings) -> Indent = { Indent.getNoneIndent() }

        private val within = ArrayList<IElementType>()
        private var withinCallback: ((ASTNode) -> Boolean)? = null

        private val notIn = ArrayList<IElementType>()

        private val forElement = ArrayList<IElementType>()
        private val notForElement = ArrayList<IElementType>()
        private var forElementCallback: ((ASTNode) -> Boolean)? = null

        override fun toString(): String {
            return "PositionStrategy " + (debugInfo ?: "No debug info")
        }

        fun set(indent: Indent): PositionStrategy {
            indentCallback = { indent }
            return this
        }

        fun set(indentCallback: (CodeStyleSettings) -> Indent): PositionStrategy {
            this.indentCallback = indentCallback
            return this
        }

        fun within(parents: TokenSet): PositionStrategy {
            val types = parents.types
            if (types.isEmpty()) {
                throw IllegalArgumentException("Empty token set is unexpected")
            }

            fillTypes(within, types[0], types.copyOfRange(1, types.size))
            return this
        }

        fun within(parentType: IElementType, vararg orParentTypes: IElementType): PositionStrategy {
            fillTypes(within, parentType, orParentTypes)
            return this
        }

        fun within(callback: (ASTNode) -> Boolean): PositionStrategy {
            withinCallback = callback
            return this
        }

        fun notWithin(parentType: IElementType, vararg orParentTypes: IElementType): PositionStrategy {
            fillTypes(notIn, parentType, orParentTypes)
            return this
        }

        fun withinAny(): PositionStrategy {
            within.clear()
            notIn.clear()
            return this
        }

        fun forType(elementType: IElementType, vararg otherTypes: IElementType): PositionStrategy {
            fillTypes(forElement, elementType, otherTypes)
            return this
        }

        fun notForType(elementType: IElementType, vararg otherTypes: IElementType): PositionStrategy {
            fillTypes(notForElement, elementType, otherTypes)
            return this
        }

        fun forAny(): PositionStrategy {
            forElement.clear()
            notForElement.clear()
            return this
        }

        fun forElement(callback: (ASTNode) -> Boolean): PositionStrategy {
            forElementCallback = callback
            return this
        }

        override fun getIndent(node: ASTNode, settings: CodeStyleSettings): Indent? {
            if (!isValidIndent(forElement, notForElement, node, forElementCallback)) return null

            val parent = node.treeParent
            if (parent != null) {
                if (!isValidIndent(within, notIn, parent, withinCallback)) return null
            } else if (within.isNotEmpty()) return null

            return indentCallback(settings)
        }

        private fun fillTypes(resultCollection: MutableList<IElementType>, singleType: IElementType, otherTypes: Array<out IElementType>) {
            resultCollection.clear()
            resultCollection.add(singleType)
            resultCollection.addAll(otherTypes)
        }
    }

    companion object {
        fun constIndent(indent: Indent): NodeIndentStrategy {
            return ConstIndentStrategy(indent)
        }

        fun strategy(@NonNls debugInfo: String?): PositionStrategy {
            return PositionStrategy(debugInfo)
        }
    }
}

private fun isValidIndent(
    elements: ArrayList<IElementType>,
    excludeElements: ArrayList<IElementType>,
    node: ASTNode,
    callback: ((ASTNode) -> Boolean)?
): Boolean {
    if (elements.isNotEmpty() && !elements.contains(node.elementType)) return false
    if (excludeElements.contains(node.elementType)) return false
    if (callback?.invoke(node) == false) return false
    return true
}

class SyntheticKotlinBlock(
    private val node: ASTNode,
    private val subBlocks: List<ASTBlock>,
    private val alignment: Alignment?,
    private val indent: Indent?,
    private val wrap: Wrap?,
    private val spacingBuilder: KotlinSpacingBuilder,
    private val createParentSyntheticSpacingBlock: (ASTNode) -> ASTBlock
) : ASTBlock {

    private val textRange = TextRange(
        subBlocks.first().textRange.startOffset,
        subBlocks.last().textRange.endOffset
    )

    override fun getTextRange(): TextRange = textRange
    override fun getSubBlocks() = subBlocks
    override fun getWrap() = wrap
    override fun getIndent() = indent
    override fun getAlignment() = alignment
    override fun getChildAttributes(newChildIndex: Int) = ChildAttributes(getIndent(), null)
    override fun isIncomplete() = getSubBlocks().last().isIncomplete
    override fun isLeaf() = false
    override fun getNode() = node
    override fun getSpacing(child1: Block?, child2: Block): Spacing? {
        return spacingBuilder.getSpacing(createParentSyntheticSpacingBlock(node), child1, child2)
    }


    override fun toString(): String {
        var child = subBlocks.first()
        var treeNode: ASTNode? = null

        loop@
        while (treeNode == null) when (child) {
            is SyntheticKotlinBlock -> child = child.getSubBlocks().first()

            else -> treeNode = child.node
        }

        val textRange = getTextRange()
        val psi = treeNode.psi
        if (psi != null) {
            val file = psi.containingFile
            if (file != null) {
                return file.text!!.subSequence(textRange.startOffset, textRange.endOffset).toString() + " " + textRange
            }
        }

        return this::class.java.name + ": " + textRange
    }
}

/*
 * ASTBlock.node is nullable, this extension was introduced to minimize changes
 */
fun ASTBlock.requireNode() = node ?: error("ASTBlock.getNode() returned null")

/**
 * Can be removed with all usages after moving master to 1.3 with new default code style settings.
 */
val isDefaultOfficialCodeStyle by lazy { !KotlinCodeStyleSettings.defaultSettings().CONTINUATION_INDENT_FOR_CHAINED_CALLS }

fun PsiElement.getLineCount(): Int {
    val doc = containingFile?.let { PsiDocumentManager.getInstance(project).getDocument(it) }
    if (doc != null) {
        val spaceRange = textRange ?: TextRange.EMPTY_RANGE

        if (spaceRange.endOffset <= doc.textLength && spaceRange.startOffset < spaceRange.endOffset) {
            val startLine = doc.getLineNumber(spaceRange.startOffset)
            val endLine = doc.getLineNumber(spaceRange.endOffset - 1)

            return endLine - startLine + 1
        }
    }

    return StringUtil.getLineBreakCount(text ?: "") + 1
}

fun PsiElement.isMultiline() = getLineCount() > 1

fun KtFunctionLiteral.needTrailingComma(settings: CodeStyleSettings?, checkExistingTrailingComma: Boolean = true): Boolean =
    needTrailingComma(
        settings = settings,
        trailingComma = { if (checkExistingTrailingComma) valueParameterList?.trailingComma else null },
        globalStartOffset = { valueParameterList?.startOffset },
        globalEndOffset = { arrow?.endOffset },
    )

fun KtWhenEntry.needTrailingComma(settings: CodeStyleSettings?, checkExistingTrailingComma: Boolean = true): Boolean = needTrailingComma(
    settings = settings,
    trailingComma = { if (checkExistingTrailingComma) trailingComma else null },
    additionalCheck = { !isElse && parent.cast<KtWhenExpression>().leftParenthesis != null },
    globalEndOffset = { arrow?.endOffset },
)

fun KtDestructuringDeclaration.needTrailingComma(settings: CodeStyleSettings?, checkExistingTrailingComma: Boolean = true): Boolean =
    needTrailingComma(
        settings = settings,
        trailingComma = { if (checkExistingTrailingComma) trailingComma else null },
        globalStartOffset = { lPar?.startOffset },
        globalEndOffset = { rPar?.endOffset },
    )

fun <T : PsiElement> T.needTrailingComma(
    settings: CodeStyleSettings?,
    trailingComma: T.() -> PsiElement?,
    additionalCheck: () -> Boolean = { true },
    globalStartOffset: T.() -> Int? = PsiElement::startOffset,
    globalEndOffset: T.() -> Int? = PsiElement::endOffset,
): Boolean {
    if (trailingComma() == null && settings?.kotlinCustomSettings?.addTrailingCommaIsAllowedFor(this) == false) return false
    if (!additionalCheck()) return false

    val startOffset = globalStartOffset() ?: return false
    val endOffset = globalEndOffset() ?: return false
    return containsLineBreakInThis(startOffset, endOffset)
}

fun PsiElement.containsLineBreakInThis(globalStartOffset: Int, globalEndOffset: Int): Boolean {
    val textRange = TextRange.create(globalStartOffset, globalEndOffset).shiftLeft(startOffset)
    return StringUtil.containsLineBreak(textRange.subSequence(text))
}

fun trailingCommaIsAllowedOnCallSite(): Boolean = Registry.`is`("kotlin.formatter.allowTrailingCommaOnCallSite")

private val TYPES_WITH_TRAILING_COMMA = TokenSet.create(
    KtNodeTypes.TYPE_PARAMETER_LIST,
    KtNodeTypes.DESTRUCTURING_DECLARATION,
    KtNodeTypes.WHEN_ENTRY,
    KtNodeTypes.FUNCTION_LITERAL,
    KtNodeTypes.VALUE_PARAMETER_LIST,
)

private val TYPES_WITH_TRAILING_COMMA_ON_CALL_SITE = TokenSet.create(
    KtNodeTypes.COLLECTION_LITERAL_EXPRESSION,
    KtNodeTypes.TYPE_ARGUMENT_LIST,
    KtNodeTypes.INDICES,
    KtNodeTypes.VALUE_ARGUMENT_LIST,
<caret>)

fun UserDataHolder.addTrailingCommaIsAllowedForThis(): Boolean {
    val type = when (this) {
        is ASTNode -> PsiUtilCore.getElementType(this)
        is PsiElement -> PsiUtilCore.getElementType(this)
        else -> return false
    }

    return type in TYPES_WITH_TRAILING_COMMA || trailingCommaIsAllowedOnCallSite() && type in TYPES_WITH_TRAILING_COMMA_ON_CALL_SITE
}

fun KotlinCodeStyleSettings.addTrailingCommaIsAllowedFor(element: UserDataHolder): Boolean =
    ALLOW_TRAILING_COMMA && element.addTrailingCommaIsAllowedForThis()

data class KtCodeStyleSettings(
    val custom: KotlinCodeStyleSettings,
    val common: KotlinCommonCodeStyleSettings,
    val all: CodeStyleSettings
)

fun KtCodeStyleSettings.canRestore(): Boolean {
    return custom.canRestore() || common.canRestore()
}

fun KtCodeStyleSettings.hasDefaultLoadScheme(): Boolean {
    return custom.CODE_STYLE_DEFAULTS == null || common.CODE_STYLE_DEFAULTS == null
}

fun KtCodeStyleSettings.restore() {
    custom.restore()
    common.restore()
}

fun ktCodeStyleSettings(project: Project): KtCodeStyleSettings? {
    val settings = CodeStyle.getSettings(project)

    val ktCommonSettings = settings.getCommonSettings(KotlinLanguage.INSTANCE) as KotlinCommonCodeStyleSettings
    val ktCustomSettings = settings.getCustomSettings(KotlinCodeStyleSettings::class.java)

    return KtCodeStyleSettings(ktCustomSettings, ktCommonSettings, settings)
}

val CodeStyleSettings.kotlinCommonSettings: KotlinCommonCodeStyleSettings
    get() = getCommonSettings(KotlinLanguage.INSTANCE) as KotlinCommonCodeStyleSettings

val CodeStyleSettings.kotlinCustomSettings: KotlinCodeStyleSettings
    get() = getCustomSettings(KotlinCodeStyleSettings::class.java)

fun CodeStyleSettings.kotlinCodeStyleDefaults(): String? {
    return kotlinCustomSettings.CODE_STYLE_DEFAULTS ?: kotlinCommonSettings.CODE_STYLE_DEFAULTS
}

fun CommonCodeStyleSettings.createSpaceBeforeRBrace(numSpacesOtherwise: Int, textRange: TextRange): Spacing? {
    return Spacing.createDependentLFSpacing(
        numSpacesOtherwise, numSpacesOtherwise, textRange,
        KEEP_LINE_BREAKS,
        KEEP_BLANK_LINES_BEFORE_RBRACE
    )
}

class KotlinSpacingBuilder(val commonCodeStyleSettings: CommonCodeStyleSettings, val spacingBuilderUtil: KotlinSpacingBuilderUtil) {
    private val builders = ArrayList<Builder>()

    private interface Builder {
        fun getSpacing(parent: ASTBlock, left: ASTBlock, right: ASTBlock): Spacing?
    }

    inner class BasicSpacingBuilder : SpacingBuilder(commonCodeStyleSettings), Builder {
        override fun getSpacing(parent: ASTBlock, left: ASTBlock, right: ASTBlock): Spacing? {
            return super.getSpacing(parent, left, right)
        }
    }

    private data class Condition(
        val parent: IElementType? = null,
        val left: IElementType? = null,
        val right: IElementType? = null,
        val parentSet: TokenSet? = null,
        val leftSet: TokenSet? = null,
        val rightSet: TokenSet? = null
    ) : (ASTBlock, ASTBlock, ASTBlock) -> Boolean {
        override fun invoke(p: ASTBlock, l: ASTBlock, r: ASTBlock): Boolean =
            (parent == null || p.requireNode().elementType == parent) &&
                    (left == null || l.requireNode().elementType == left) &&
                    (right == null || r.requireNode().elementType == right) &&
                    (parentSet == null || parentSet.contains(p.requireNode().elementType)) &&
                    (leftSet == null || leftSet.contains(l.requireNode().elementType)) &&
                    (rightSet == null || rightSet.contains(r.requireNode().elementType))
    }

    private data class Rule(
        val conditions: List<Condition>,
        val action: (ASTBlock, ASTBlock, ASTBlock) -> Spacing?
    ) : (ASTBlock, ASTBlock, ASTBlock) -> Spacing? {
        override fun invoke(p: ASTBlock, l: ASTBlock, r: ASTBlock): Spacing? =
            if (conditions.all { it(p, l, r) }) action(p, l, r) else null
    }

    inner class CustomSpacingBuilder : Builder {
        private val rules = ArrayList<Rule>()
        private var conditions = ArrayList<Condition>()

        override fun getSpacing(parent: ASTBlock, left: ASTBlock, right: ASTBlock): Spacing? {
            for (rule in rules) {
                val spacing = rule(parent, left, right)
                if (spacing != null) {
                    return spacing
                }
            }
            return null
        }

        fun inPosition(
            parent: IElementType? = null, left: IElementType? = null, right: IElementType? = null,
            parentSet: TokenSet? = null, leftSet: TokenSet? = null, rightSet: TokenSet? = null
        ): CustomSpacingBuilder {
            conditions.add(Condition(parent, left, right, parentSet, leftSet, rightSet))
            return this
        }

        fun lineBreakIfLineBreakInParent(numSpacesOtherwise: Int, allowBlankLines: Boolean = true) {
            newRule { p, _, _ ->
                Spacing.createDependentLFSpacing(
                    numSpacesOtherwise, numSpacesOtherwise, p.textRange,
                    commonCodeStyleSettings.KEEP_LINE_BREAKS,
                    if (allowBlankLines) commonCodeStyleSettings.KEEP_BLANK_LINES_IN_CODE else 0
                )
            }
        }

        fun emptyLinesIfLineBreakInLeft(emptyLines: Int, numberOfLineFeedsOtherwise: Int = 1, numSpacesOtherwise: Int = 0) {
            newRule { _: ASTBlock, left: ASTBlock, _: ASTBlock ->
                val lastChild = left.node?.psi?.lastChild
                val leftEndsWithComment = lastChild is PsiComment && lastChild.tokenType == KtTokens.EOL_COMMENT
                val dependentSpacingRule = DependentSpacingRule(Trigger.HAS_LINE_FEEDS).registerData(Anchor.MIN_LINE_FEEDS, emptyLines + 1)
                val textRange = left.node
                    ?.startOfDeclaration()
                    ?.startOffset
                    ?.let { TextRange.create(it, left.textRange.endOffset) }
                    ?: left.textRange

                spacingBuilderUtil.createLineFeedDependentSpacing(
                    numSpacesOtherwise,
                    numSpacesOtherwise,
                    if (leftEndsWithComment) max(1, numberOfLineFeedsOtherwise) else numberOfLineFeedsOtherwise,
                    commonCodeStyleSettings.KEEP_LINE_BREAKS,
                    commonCodeStyleSettings.KEEP_BLANK_LINES_IN_DECLARATIONS,
                    textRange,
                    dependentSpacingRule
                )
            }
        }

        fun spacing(spacing: Spacing) {
            newRule { _, _, _ -> spacing }
        }

        fun customRule(block: (parent: ASTBlock, left: ASTBlock, right: ASTBlock) -> Spacing?) {
            newRule(block)
        }

        private fun newRule(rule: (ASTBlock, ASTBlock, ASTBlock) -> Spacing?) {
            val savedConditions = ArrayList(conditions)
            rules.add(Rule(savedConditions, rule))
            conditions.clear()
        }
    }

    fun getSpacing(parent: Block, child1: Block?, child2: Block): Spacing? {
        if (parent !is ASTBlock || child1 !is ASTBlock || child2 !is ASTBlock) {
            return null
        }

        for (builder in builders) {
            val spacing = builder.getSpacing(parent, child1, child2)

            if (spacing != null) {
                // TODO: it's a severe hack but I don't know how to implement it in other way
                if (child1.requireNode().elementType == KtTokens.EOL_COMMENT && spacing.toString().contains("minLineFeeds=0")) {
                    val isBeforeBlock =
                        child2.requireNode().elementType == KtNodeTypes.BLOCK || child2.requireNode().firstChildNode
                            ?.elementType == KtNodeTypes.BLOCK
                    val keepBlankLines = if (isBeforeBlock) 0 else commonCodeStyleSettings.KEEP_BLANK_LINES_IN_CODE
                    return createSpacing(0, minLineFeeds = 1, keepLineBreaks = true, keepBlankLines = keepBlankLines)
                }
                return spacing
            }
        }
        return null
    }

    fun simple(init: BasicSpacingBuilder.() -> Unit) {
        val builder = BasicSpacingBuilder()
        builder.init()
        builders.add(builder)
    }

    fun custom(init: CustomSpacingBuilder.() -> Unit) {
        val builder = CustomSpacingBuilder()
        builder.init()
        builders.add(builder)
    }

    fun createSpacing(
        minSpaces: Int,
        maxSpaces: Int = minSpaces,
        minLineFeeds: Int = 0,
        keepLineBreaks: Boolean = commonCodeStyleSettings.KEEP_LINE_BREAKS,
        keepBlankLines: Int = commonCodeStyleSettings.KEEP_BLANK_LINES_IN_CODE
    ): Spacing {
        return Spacing.createSpacing(minSpaces, maxSpaces, minLineFeeds, keepLineBreaks, keepBlankLines)
    }
}

interface KotlinSpacingBuilderUtil {
    fun createLineFeedDependentSpacing(
        minSpaces: Int,
        maxSpaces: Int,
        minimumLineFeeds: Int,
        keepLineBreaks: Boolean,
        keepBlankLines: Int,
        dependency: TextRange,
        rule: DependentSpacingRule
    ): Spacing

    fun getPreviousNonWhitespaceLeaf(node: ASTNode?): ASTNode?

    fun isWhitespaceOrEmpty(node: ASTNode?): Boolean
}

fun rules(
    commonCodeStyleSettings: CommonCodeStyleSettings,
    builderUtil: KotlinSpacingBuilderUtil,
    init: KotlinSpacingBuilder.() -> Unit
): KotlinSpacingBuilder {
    val builder = KotlinSpacingBuilder(commonCodeStyleSettings, builderUtil)
    builder.init()
    return builder
}

internal fun ASTNode.startOfDeclaration(): ASTNode? = children().firstOrNull {
    val elementType = it.elementType
    elementType !is KtModifierListElementType<*> && elementType !in KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET
}

val MODIFIERS_LIST_ENTRIES = TokenSet.orSet(TokenSet.create(ANNOTATION_ENTRY, ANNOTATION), MODIFIER_KEYWORDS)

val EXTEND_COLON_ELEMENTS =
    TokenSet.create(TYPE_CONSTRAINT, CLASS, OBJECT_DECLARATION, TYPE_PARAMETER, ENUM_ENTRY, SECONDARY_CONSTRUCTOR)

val DECLARATIONS = TokenSet.create(PROPERTY, FUN, CLASS, OBJECT_DECLARATION, ENUM_ENTRY, SECONDARY_CONSTRUCTOR, CLASS_INITIALIZER)

fun SpacingBuilder.beforeInside(element: IElementType, tokenSet: TokenSet, spacingFun: RuleBuilder.() -> Unit) {
    tokenSet.types.forEach { inType -> beforeInside(element, inType).spacingFun() }
}

fun SpacingBuilder.afterInside(element: IElementType, tokenSet: TokenSet, spacingFun: RuleBuilder.() -> Unit) {
    tokenSet.types.forEach { inType -> afterInside(element, inType).spacingFun() }
}

fun RuleBuilder.spacesNoLineBreak(spaces: Int): SpacingBuilder? =
    spacing(spaces, spaces, 0, false, 0)

fun createSpacingBuilder(settings: CodeStyleSettings, builderUtil: KotlinSpacingBuilderUtil): KotlinSpacingBuilder {
    val kotlinCommonSettings = settings.kotlinCommonSettings
    val kotlinCustomSettings = settings.kotlinCustomSettings
    return rules(kotlinCommonSettings, builderUtil) {
        simple {
            before(FILE_ANNOTATION_LIST).lineBreakInCode()
            between(IMPORT_DIRECTIVE, IMPORT_DIRECTIVE).lineBreakInCode()
            after(IMPORT_LIST).blankLines(1)
        }

        custom {
            fun commentSpacing(minSpaces: Int): Spacing {
                if (kotlinCommonSettings.KEEP_FIRST_COLUMN_COMMENT) {
                    return Spacing.createKeepingFirstColumnSpacing(
                        minSpaces,
                        Int.MAX_VALUE,
                        settings.KEEP_LINE_BREAKS,
                        kotlinCommonSettings.KEEP_BLANK_LINES_IN_CODE
                    )
                }
                return Spacing.createSpacing(
                    minSpaces,
                    Int.MAX_VALUE,
                    0,
                    settings.KEEP_LINE_BREAKS,
                    kotlinCommonSettings.KEEP_BLANK_LINES_IN_CODE
                )
            }

            // Several line comments happened to be generated in one line
            inPosition(parent = null, left = EOL_COMMENT, right = EOL_COMMENT).customRule { _, _, right ->
                val nodeBeforeRight = right.requireNode().treePrev
                if (nodeBeforeRight is PsiWhiteSpace && !nodeBeforeRight.textContains('\n')) {
                    createSpacing(0, minLineFeeds = 1)
                } else {
                    null
                }
            }

            inPosition(right = BLOCK_COMMENT).spacing(commentSpacing(0))
            inPosition(right = EOL_COMMENT).spacing(commentSpacing(1))
            inPosition(parent = FUNCTION_LITERAL, right = BLOCK).customRule { _, _, right ->
                when (right.node?.children()?.firstOrNull()?.elementType) {
                    BLOCK_COMMENT -> commentSpacing(0)
                    EOL_COMMENT -> commentSpacing(1)
                    else -> null
                }
            }
        }

        simple {
            after(FILE_ANNOTATION_LIST).blankLines(1)
            after(PACKAGE_DIRECTIVE).blankLines(1)
        }

        custom {
            inPosition(leftSet = DECLARATIONS, rightSet = DECLARATIONS).customRule(fun(
                _: ASTBlock,
                _: ASTBlock,
                right: ASTBlock
            ): Spacing? {
                val node = right.node ?: return null
                val elementStart = node.startOfDeclaration() ?: return null
                return if (StringUtil.containsLineBreak(
                        node.text.subSequence(0, elementStart.startOffset - node.startOffset).trimStart()
                    )
                )
                    createSpacing(0, minLineFeeds = 2)
                else
                    null
            })

            inPosition(left = CLASS, right = CLASS).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = CLASS, right = OBJECT_DECLARATION).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = OBJECT_DECLARATION, right = OBJECT_DECLARATION).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = OBJECT_DECLARATION, right = CLASS).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = FUN, right = FUN).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = PROPERTY, right = FUN).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = FUN, right = PROPERTY).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = SECONDARY_CONSTRUCTOR, right = SECONDARY_CONSTRUCTOR).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = TYPEALIAS, right = TYPEALIAS).emptyLinesIfLineBreakInLeft(1)

            // Case left for alternative constructors
            inPosition(left = FUN, right = CLASS).emptyLinesIfLineBreakInLeft(1)

            inPosition(left = ENUM_ENTRY, right = ENUM_ENTRY).emptyLinesIfLineBreakInLeft(
                emptyLines = 0, numberOfLineFeedsOtherwise = 0, numSpacesOtherwise = 1
            )

            inPosition(parent = CLASS_BODY, left = SEMICOLON).customRule { parent, _, right ->
                val klass = parent.requireNode().treeParent.psi as? KtClass ?: return@customRule null
                if (klass.isEnum() && right.requireNode().elementType in DECLARATIONS) {
                    createSpacing(0, minLineFeeds = 2, keepBlankLines = settings.KEEP_BLANK_LINES_IN_DECLARATIONS)
                } else null
            }

            inPosition(parent = CLASS_BODY, left = LBRACE).customRule { parent, left, right ->
                if (right.requireNode().elementType == RBRACE) {
                    return@customRule createSpacing(0)
                }
                val classBody = parent.requireNode().psi as KtClassBody
                val parentPsi = classBody.parent as? KtClassOrObject ?: return@customRule null
                if (kotlinCommonSettings.BLANK_LINES_AFTER_CLASS_HEADER == 0 || parentPsi.isObjectLiteral()) {
                    null
                } else {
                    val minLineFeeds = if (right.requireNode().elementType == FUN || right.requireNode().elementType == PROPERTY)
                        kotlinCommonSettings.BLANK_LINES_AFTER_CLASS_HEADER + 1
                    else
                        0

                    builderUtil.createLineFeedDependentSpacing(
                        1,
                        1,
                        minLineFeeds,
                        commonCodeStyleSettings.KEEP_LINE_BREAKS,
                        commonCodeStyleSettings.KEEP_BLANK_LINES_IN_DECLARATIONS,
                        TextRange(parentPsi.textRange.startOffset, left.requireNode().psi.textRange.startOffset),
                        DependentSpacingRule(DependentSpacingRule.Trigger.HAS_LINE_FEEDS)
                            .registerData(
                                DependentSpacingRule.Anchor.MIN_LINE_FEEDS,
                                kotlinCommonSettings.BLANK_LINES_AFTER_CLASS_HEADER + 1
                            )
                    )
                }
            }

            val parameterWithDocCommentRule = { _: ASTBlock, _: ASTBlock, right: ASTBlock ->
                if (right.requireNode().firstChildNode.elementType == DOC_COMMENT) {
                    createSpacing(0, minLineFeeds = 1, keepLineBreaks = true, keepBlankLines = settings.KEEP_BLANK_LINES_IN_DECLARATIONS)
                } else {
                    null
                }
            }
            inPosition(parent = VALUE_PARAMETER_LIST, right = VALUE_PARAMETER).customRule(parameterWithDocCommentRule)

            inPosition(parent = PROPERTY, right = PROPERTY_ACCESSOR).customRule { parent, _, _ ->
                val startNode = parent.requireNode().psi.firstChild
                    .siblings()
                    .dropWhile { it is PsiComment || it is PsiWhiteSpace }.firstOrNull() ?: parent.requireNode().psi
                Spacing.createDependentLFSpacing(
                    1, 1,
                    TextRange(startNode.textRange.startOffset, parent.textRange.endOffset),
                    false, 0
                )
            }

            if (!kotlinCustomSettings.ALLOW_TRAILING_COMMA) {
                inPosition(parent = VALUE_ARGUMENT_LIST, left = LPAR).customRule { parent, _, _ ->
                    if (kotlinCommonSettings.CALL_PARAMETERS_LPAREN_ON_NEXT_LINE && org.jetbrains.kotlin.idea.formatter.needWrapArgumentList(
                            parent.requireNode().psi
                        )
                    ) {
                        Spacing.createDependentLFSpacing(
                            0, 0,
                            excludeLambdasAndObjects(parent),
                            commonCodeStyleSettings.KEEP_LINE_BREAKS,
                            commonCodeStyleSettings.KEEP_BLANK_LINES_IN_CODE
                        )
                    } else {
                        createSpacing(0)
                    }
                }

                inPosition(parent = VALUE_ARGUMENT_LIST, right = RPAR).customRule { parent, left, _ ->
                    when {
                        kotlinCommonSettings.CALL_PARAMETERS_RPAREN_ON_NEXT_LINE ->
                            Spacing.createDependentLFSpacing(
                                0, 0,
                                excludeLambdasAndObjects(parent),
                                commonCodeStyleSettings.KEEP_LINE_BREAKS,
                                commonCodeStyleSettings.KEEP_BLANK_LINES_IN_CODE
                            )
                        left.requireNode().elementType == COMMA -> // incomplete call being edited
                            createSpacing(1)
                        else ->
                            createSpacing(0)
                    }
                }
            }

            inPosition(left = CONDITION, right = RPAR).customRule { _, left, _ ->
                if (kotlinCustomSettings.IF_RPAREN_ON_NEW_LINE) {
                    Spacing.createDependentLFSpacing(
                        0, 0,
                        excludeLambdasAndObjects(left),
                        commonCodeStyleSettings.KEEP_LINE_BREAKS,
                        commonCodeStyleSettings.KEEP_BLANK_LINES_IN_CODE
                    )
                } else {
                    createSpacing(0)
                }
            }

            inPosition(left = VALUE_PARAMETER, right = COMMA).customRule { _, left, _ ->
                if (left.node?.lastChildNode?.elementType === EOL_COMMENT)
                    createSpacing(0, minLineFeeds = 1)
                else
                    null
            }

            inPosition(parent = LONG_STRING_TEMPLATE_ENTRY, right = LONG_TEMPLATE_ENTRY_END).lineBreakIfLineBreakInParent(0)
            inPosition(parent = LONG_STRING_TEMPLATE_ENTRY, left = LONG_TEMPLATE_ENTRY_START).lineBreakIfLineBreakInParent(0)
        }

        simple {
            // ============ Line breaks ==============
            before(DOC_COMMENT).lineBreakInCode()
            between(PROPERTY, PROPERTY).lineBreakInCode()

            // CLASS - CLASS, CLASS - OBJECT_DECLARATION are exception
            between(CLASS, DECLARATIONS).blankLines(1)

            // FUN - FUN, FUN - PROPERTY, FUN - CLASS are exceptions
            between(FUN, DECLARATIONS).blankLines(1)

            // PROPERTY - PROPERTY, PROPERTY - FUN are exceptions
            between(PROPERTY, DECLARATIONS).blankLines(1)

            // OBJECT_DECLARATION - OBJECT_DECLARATION, CLASS - OBJECT_DECLARATION are exception
            between(OBJECT_DECLARATION, DECLARATIONS).blankLines(1)
            between(SECONDARY_CONSTRUCTOR, DECLARATIONS).blankLines(1)
            between(CLASS_INITIALIZER, DECLARATIONS).blankLines(1)

            // TYPEALIAS - TYPEALIAS is an exception
            between(TYPEALIAS, DECLARATIONS).blankLines(1)

            // ENUM_ENTRY - ENUM_ENTRY is exception
            between(ENUM_ENTRY, DECLARATIONS).blankLines(1)

            between(ENUM_ENTRY, SEMICOLON).spaces(0)

            between(COMMA, SEMICOLON).lineBreakInCodeIf(kotlinCustomSettings.ALLOW_TRAILING_COMMA)

            beforeInside(FUN, TokenSet.create(BODY, CLASS_BODY)).lineBreakInCode()
            beforeInside(SECONDARY_CONSTRUCTOR, TokenSet.create(BODY, CLASS_BODY)).lineBreakInCode()
            beforeInside(CLASS, TokenSet.create(BODY, CLASS_BODY)).lineBreakInCode()
            beforeInside(OBJECT_DECLARATION, TokenSet.create(BODY, CLASS_BODY)).lineBreakInCode()
            beforeInside(PROPERTY, WHEN).spaces(0)
            beforeInside(PROPERTY, LABELED_EXPRESSION).spacesNoLineBreak(1)
            before(PROPERTY).lineBreakInCode()

            after(DOC_COMMENT).lineBreakInCode()

            // =============== Spacing ================
            between(EOL_COMMENT, COMMA).lineBreakInCode()
            before(COMMA).spacesNoLineBreak(if (kotlinCommonSettings.SPACE_BEFORE_COMMA) 1 else 0)
            after(COMMA).spaceIf(kotlinCommonSettings.SPACE_AFTER_COMMA)

            val spacesAroundAssignment = if (kotlinCommonSettings.SPACE_AROUND_ASSIGNMENT_OPERATORS) 1 else 0
            beforeInside(EQ, PROPERTY).spacesNoLineBreak(spacesAroundAssignment)
            beforeInside(EQ, FUN).spacing(spacesAroundAssignment, spacesAroundAssignment, 0, false, 0)

            around(
                TokenSet.create(EQ, MULTEQ, DIVEQ, PLUSEQ, MINUSEQ, PERCEQ)
            ).spaceIf(kotlinCommonSettings.SPACE_AROUND_ASSIGNMENT_OPERATORS)
            around(TokenSet.create(ANDAND, OROR)).spaceIf(kotlinCommonSettings.SPACE_AROUND_LOGICAL_OPERATORS)
            around(TokenSet.create(EQEQ, EXCLEQ, EQEQEQ, EXCLEQEQEQ)).spaceIf(kotlinCommonSettings.SPACE_AROUND_EQUALITY_OPERATORS)
            aroundInside(
                TokenSet.create(LT, GT, LTEQ, GTEQ), BINARY_EXPRESSION
            ).spaceIf(kotlinCommonSettings.SPACE_AROUND_RELATIONAL_OPERATORS)
            aroundInside(TokenSet.create(PLUS, MINUS), BINARY_EXPRESSION).spaceIf(kotlinCommonSettings.SPACE_AROUND_ADDITIVE_OPERATORS)
            aroundInside(
                TokenSet.create(MUL, DIV, PERC), BINARY_EXPRESSION
            ).spaceIf(kotlinCommonSettings.SPACE_AROUND_MULTIPLICATIVE_OPERATORS)
            around(
                TokenSet.create(PLUSPLUS, MINUSMINUS, EXCLEXCL, MINUS, PLUS, EXCL)
            ).spaceIf(kotlinCommonSettings.SPACE_AROUND_UNARY_OPERATOR)
            before(ELVIS).spaces(1)
            after(ELVIS).spacesNoLineBreak(1)
            around(RANGE).spaceIf(kotlinCustomSettings.SPACE_AROUND_RANGE)

            after(MODIFIER_LIST).spaces(1)

            beforeInside(IDENTIFIER, CLASS).spaces(1)
            beforeInside(IDENTIFIER, OBJECT_DECLARATION).spaces(1)

            after(VAL_KEYWORD).spaces(1)
            after(VAR_KEYWORD).spaces(1)
            betweenInside(TYPE_PARAMETER_LIST, IDENTIFIER, PROPERTY).spaces(1)
            betweenInside(TYPE_REFERENCE, DOT, PROPERTY).spacing(0, 0, 0, false, 0)
            betweenInside(DOT, IDENTIFIER, PROPERTY).spacing(0, 0, 0, false, 0)

            betweenInside(RETURN_KEYWORD, LABEL_QUALIFIER, RETURN).spaces(0)
            afterInside(RETURN_KEYWORD, RETURN).spaces(1)
            afterInside(LABEL_QUALIFIER, RETURN).spaces(1)
            betweenInside(LABEL_QUALIFIER, EOL_COMMENT, LABELED_EXPRESSION).spacing(
                0, Int.MAX_VALUE, 0, true, kotlinCommonSettings.KEEP_BLANK_LINES_IN_CODE
            )
            betweenInside(LABEL_QUALIFIER, BLOCK_COMMENT, LABELED_EXPRESSION).spacing(
                0, Int.MAX_VALUE, 0, true, kotlinCommonSettings.KEEP_BLANK_LINES_IN_CODE
            )
            betweenInside(LABEL_QUALIFIER, LAMBDA_EXPRESSION, LABELED_EXPRESSION).spaces(0)
            afterInside(LABEL_QUALIFIER, LABELED_EXPRESSION).spaces(1)

            betweenInside(FUN_KEYWORD, VALUE_PARAMETER_LIST, FUN).spacing(0, 0, 0, false, 0)
            after(FUN_KEYWORD).spaces(1)
            betweenInside(TYPE_PARAMETER_LIST, TYPE_REFERENCE, FUN).spaces(1)
            betweenInside(TYPE_PARAMETER_LIST, IDENTIFIER, FUN).spaces(1)
            betweenInside(TYPE_REFERENCE, DOT, FUN).spacing(0, 0, 0, false, 0)
            betweenInside(DOT, IDENTIFIER, FUN).spacing(0, 0, 0, false, 0)
            afterInside(IDENTIFIER, FUN).spacing(0, 0, 0, false, 0)
            aroundInside(DOT, USER_TYPE).spaces(0)

            around(AS_KEYWORD).spaces(1)
            around(AS_SAFE).spaces(1)
            around(IS_KEYWORD).spaces(1)
            around(NOT_IS).spaces(1)
            around(IN_KEYWORD).spaces(1)
            around(NOT_IN).spaces(1)
            aroundInside(IDENTIFIER, BINARY_EXPRESSION).spaces(1)

            // before LPAR in constructor(): this() {}
            after(CONSTRUCTOR_DELEGATION_REFERENCE).spacing(0, 0, 0, false, 0)

            // class A() - no space before LPAR of PRIMARY_CONSTRUCTOR
            // class A private() - one space before modifier
            custom {
                inPosition(right = PRIMARY_CONSTRUCTOR).customRule { _, _, r ->
                    val spacesCount = if (r.requireNode().findLeafElementAt(0)?.elementType != LPAR) 1 else 0
                    createSpacing(spacesCount, minLineFeeds = 0, keepLineBreaks = true, keepBlankLines = 0)
                }
            }

            afterInside(CONSTRUCTOR_KEYWORD, PRIMARY_CONSTRUCTOR).spaces(0)
            betweenInside(IDENTIFIER, TYPE_PARAMETER_LIST, CLASS).spaces(0)

            beforeInside(DOT, DOT_QUALIFIED_EXPRESSION).spaces(0)
            afterInside(DOT, DOT_QUALIFIED_EXPRESSION).spacesNoLineBreak(0)
            beforeInside(SAFE_ACCESS, SAFE_ACCESS_EXPRESSION).spaces(0)
            afterInside(SAFE_ACCESS, SAFE_ACCESS_EXPRESSION).spacesNoLineBreak(0)

            between(MODIFIERS_LIST_ENTRIES, MODIFIERS_LIST_ENTRIES).spaces(1)

            after(LBRACKET).spaces(0)
            before(RBRACKET).spaces(0)

            afterInside(LPAR, VALUE_PARAMETER_LIST).spaces(0, kotlinCommonSettings.METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE)
            beforeInside(RPAR, VALUE_PARAMETER_LIST).spaces(0, kotlinCommonSettings.METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE)
            afterInside(LT, TYPE_PARAMETER_LIST).spaces(0)
            beforeInside(GT, TYPE_PARAMETER_LIST).spaces(0)
            afterInside(LT, TYPE_ARGUMENT_LIST).spaces(0)
            beforeInside(GT, TYPE_ARGUMENT_LIST).spaces(0)
            before(TYPE_ARGUMENT_LIST).spaces(0)

            after(LPAR).spaces(0)
            before(RPAR).spaces(0)

            betweenInside(FOR_KEYWORD, LPAR, FOR).spaceIf(kotlinCommonSettings.SPACE_BEFORE_FOR_PARENTHESES)
            betweenInside(IF_KEYWORD, LPAR, IF).spaceIf(kotlinCommonSettings.SPACE_BEFORE_IF_PARENTHESES)
            betweenInside(WHILE_KEYWORD, LPAR, WHILE).spaceIf(kotlinCommonSettings.SPACE_BEFORE_WHILE_PARENTHESES)
            betweenInside(WHILE_KEYWORD, LPAR, DO_WHILE).spaceIf(kotlinCommonSettings.SPACE_BEFORE_WHILE_PARENTHESES)
            betweenInside(WHEN_KEYWORD, LPAR, WHEN).spaceIf(kotlinCustomSettings.SPACE_BEFORE_WHEN_PARENTHESES)
            betweenInside(CATCH_KEYWORD, VALUE_PARAMETER_LIST, CATCH).spaceIf(kotlinCommonSettings.SPACE_BEFORE_CATCH_PARENTHESES)

            betweenInside(LPAR, VALUE_PARAMETER, FOR).spaces(0)
            betweenInside(LPAR, DESTRUCTURING_DECLARATION, FOR).spaces(0)
            betweenInside(LOOP_RANGE, RPAR, FOR).spaces(0)

            afterInside(ANNOTATION_ENTRY, ANNOTATED_EXPRESSION).spaces(1)

            before(SEMICOLON).spaces(0)

            beforeInside(INITIALIZER_LIST, ENUM_ENTRY).spaces(0)

            beforeInside(QUEST, NULLABLE_TYPE).spaces(0)

            val TYPE_COLON_ELEMENTS = TokenSet.create(PROPERTY, FUN, VALUE_PARAMETER, DESTRUCTURING_DECLARATION_ENTRY, FUNCTION_LITERAL)
            beforeInside(COLON, TYPE_COLON_ELEMENTS) { spaceIf(kotlinCustomSettings.SPACE_BEFORE_TYPE_COLON) }
            afterInside(COLON, TYPE_COLON_ELEMENTS) { spaceIf(kotlinCustomSettings.SPACE_AFTER_TYPE_COLON) }

            afterInside(COLON, EXTEND_COLON_ELEMENTS) { spaceIf(kotlinCustomSettings.SPACE_AFTER_EXTEND_COLON) }

            beforeInside(ARROW, FUNCTION_LITERAL).spaceIf(kotlinCustomSettings.SPACE_BEFORE_LAMBDA_ARROW)

            aroundInside(ARROW, FUNCTION_TYPE).spaceIf(kotlinCustomSettings.SPACE_AROUND_FUNCTION_TYPE_ARROW)

            before(VALUE_ARGUMENT_LIST).spaces(0)
            between(VALUE_ARGUMENT_LIST, LAMBDA_ARGUMENT).spaces(1)
            betweenInside(REFERENCE_EXPRESSION, LAMBDA_ARGUMENT, CALL_EXPRESSION).spaces(1)
            betweenInside(TYPE_ARGUMENT_LIST, LAMBDA_ARGUMENT, CALL_EXPRESSION).spaces(1)

            around(COLONCOLON).spaces(0)

            around(BY_KEYWORD).spaces(1)
            betweenInside(IDENTIFIER, PROPERTY_DELEGATE, PROPERTY).spaces(1)
            betweenInside(TYPE_REFERENCE, PROPERTY_DELEGATE, PROPERTY).spaces(1)

            before(INDICES).spaces(0)
            before(WHERE_KEYWORD).spaces(1)

            afterInside(GET_KEYWORD, PROPERTY_ACCESSOR).spaces(0)
            afterInside(SET_KEYWORD, PROPERTY_ACCESSOR).spaces(0)
        }
        custom {

            fun KotlinSpacingBuilder.CustomSpacingBuilder.ruleForKeywordOnNewLine(
                shouldBeOnNewLine: Boolean,
                keyword: IElementType,
                parent: IElementType,
                afterBlockFilter: (wordParent: ASTNode, block: ASTNode) -> Boolean = { _, _ -> true }
            ) {
                if (shouldBeOnNewLine) {
                    inPosition(parent = parent, right = keyword)
                        .lineBreakIfLineBreakInParent(numSpacesOtherwise = 1, allowBlankLines = false)
                } else {
                    inPosition(parent = parent, right = keyword).customRule { _, _, right ->

                        val previousLeaf = builderUtil.getPreviousNonWhitespaceLeaf(right.requireNode())
                        val leftBlock = if (
                            previousLeaf != null &&
                            previousLeaf.elementType == RBRACE &&
                            previousLeaf.treeParent?.elementType == BLOCK
                        ) {
                            previousLeaf.treeParent!!
                        } else null

                        val removeLineBreaks = leftBlock != null && afterBlockFilter(right.node?.treeParent!!, leftBlock)
                        createSpacing(1, minLineFeeds = 0, keepLineBreaks = !removeLineBreaks, keepBlankLines = 0)
                    }
                }
            }

            ruleForKeywordOnNewLine(kotlinCommonSettings.ELSE_ON_NEW_LINE, keyword = ELSE_KEYWORD, parent = IF) { keywordParent, block ->
                block.treeParent?.elementType == THEN && block.treeParent?.treeParent == keywordParent
            }
            ruleForKeywordOnNewLine(
                kotlinCommonSettings.WHILE_ON_NEW_LINE,
                keyword = WHILE_KEYWORD,
                parent = DO_WHILE
            ) { keywordParent, block ->
                block.treeParent?.elementType == BODY && block.treeParent?.treeParent == keywordParent
            }
            ruleForKeywordOnNewLine(kotlinCommonSettings.CATCH_ON_NEW_LINE, keyword = CATCH, parent = TRY)
            ruleForKeywordOnNewLine(kotlinCommonSettings.FINALLY_ON_NEW_LINE, keyword = FINALLY, parent = TRY)


            fun spacingForLeftBrace(block: ASTNode?, blockType: IElementType = BLOCK): Spacing? {
                if (block != null && block.elementType == blockType) {
                    val leftBrace = block.findChildByType(LBRACE)
                    if (leftBrace != null) {
                        val previousLeaf = builderUtil.getPreviousNonWhitespaceLeaf(leftBrace)
                        val isAfterEolComment = previousLeaf != null && (previousLeaf.elementType == EOL_COMMENT)
                        val keepLineBreaks = kotlinCustomSettings.LBRACE_ON_NEXT_LINE || isAfterEolComment
                        val minimumLF = if (kotlinCustomSettings.LBRACE_ON_NEXT_LINE) 1 else 0
                        return createSpacing(1, minLineFeeds = minimumLF, keepLineBreaks = keepLineBreaks, keepBlankLines = 0)
                    }
                }
                return createSpacing(1)
            }

            fun leftBraceRule(blockType: IElementType = BLOCK) = { _: ASTBlock, _: ASTBlock, right: ASTBlock ->
                spacingForLeftBrace(right.node, blockType)
            }

            val leftBraceRuleIfBlockIsWrapped = { _: ASTBlock, _: ASTBlock, right: ASTBlock ->
                spacingForLeftBrace(right.requireNode().firstChildNode)
            }

            // Add space after a semicolon if there is another child at the same line
            inPosition(left = SEMICOLON).customRule { _, left, _ ->
                val nodeAfterLeft = left.requireNode().treeNext
                if (nodeAfterLeft is PsiWhiteSpace && !nodeAfterLeft.textContains('\n')) {
                    createSpacing(1)
                } else {
                    null
                }
            }

            inPosition(parent = IF, right = THEN).customRule(leftBraceRuleIfBlockIsWrapped)
            inPosition(parent = IF, right = ELSE).customRule(leftBraceRuleIfBlockIsWrapped)

            inPosition(parent = FOR, right = BODY).customRule(leftBraceRuleIfBlockIsWrapped)
            inPosition(parent = WHILE, right = BODY).customRule(leftBraceRuleIfBlockIsWrapped)
            inPosition(parent = DO_WHILE, right = BODY).customRule(leftBraceRuleIfBlockIsWrapped)

            inPosition(parent = TRY, right = BLOCK).customRule(leftBraceRule())
            inPosition(parent = CATCH, right = BLOCK).customRule(leftBraceRule())
            inPosition(parent = FINALLY, right = BLOCK).customRule(leftBraceRule())

            inPosition(parent = FUN, right = BLOCK).customRule(leftBraceRule())
            inPosition(parent = SECONDARY_CONSTRUCTOR, right = BLOCK).customRule(leftBraceRule())
            inPosition(parent = CLASS_INITIALIZER, right = BLOCK).customRule(leftBraceRule())
            inPosition(parent = PROPERTY_ACCESSOR, right = BLOCK).customRule(leftBraceRule())

            inPosition(right = CLASS_BODY).customRule(leftBraceRule(blockType = CLASS_BODY))

            inPosition(left = WHEN_ENTRY, right = WHEN_ENTRY).customRule { _, left, right ->
                val leftEntry = left.requireNode().psi as KtWhenEntry
                val rightEntry = right.requireNode().psi as KtWhenEntry
                val blankLines = if (leftEntry.expression is KtBlockExpression || rightEntry.expression is KtBlockExpression)
                    settings.kotlinCustomSettings.BLANK_LINES_AROUND_BLOCK_WHEN_BRANCHES
                else
                    0

                createSpacing(0, minLineFeeds = blankLines + 1)
            }

            inPosition(parent = WHEN_ENTRY, right = BLOCK).customRule(leftBraceRule())
            inPosition(parent = WHEN, right = LBRACE).customRule { parent, _, _ ->
                spacingForLeftBrace(block = parent.requireNode(), blockType = WHEN)
            }

            inPosition(left = LBRACE, right = WHEN_ENTRY).customRule { _, _, _ ->
                createSpacing(0, minLineFeeds = 1)
            }

            val spacesInSimpleFunction = if (kotlinCustomSettings.INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD) 1 else 0
            inPosition(
                parent = FUNCTION_LITERAL,
                left = LBRACE,
                right = BLOCK
            ).lineBreakIfLineBreakInParent(numSpacesOtherwise = spacesInSimpleFunction)

            inPosition(
                parent = FUNCTION_LITERAL,
                left = ARROW,
                right = BLOCK
            ).lineBreakIfLineBreakInParent(numSpacesOtherwise = 1)

            inPosition(
                parent = FUNCTION_LITERAL,
                left = LBRACE,
                right = RBRACE
            ).spacing(createSpacing(minSpaces = 0, maxSpaces = 1))

            inPosition(
                parent = FUNCTION_LITERAL,
                right = RBRACE
            ).lineBreakIfLineBreakInParent(numSpacesOtherwise = spacesInSimpleFunction)

            inPosition(
                parent = FUNCTION_LITERAL,
                left = LBRACE
            ).customRule { _, _, right ->
                val rightNode = right.requireNode()
                val rightType = rightNode.elementType
                if (rightType == VALUE_PARAMETER_LIST) {
                    createSpacing(spacesInSimpleFunction, keepLineBreaks = false)
                } else {
                    createSpacing(spacesInSimpleFunction)
                }
            }

            inPosition(parent = CLASS_BODY, right = RBRACE).customRule { parent, _, _ ->
                kotlinCommonSettings.createSpaceBeforeRBrace(1, parent.textRange)
            }

            inPosition(parent = BLOCK, right = RBRACE).customRule { block, left, _ ->
                val psiElement = block.requireNode().treeParent.psi

                val empty = left.requireNode().elementType == LBRACE

                when (psiElement) {
                    is KtFunction -> {
                        if (psiElement.name != null && !empty) return@customRule null
                    }
                    is KtPropertyAccessor ->
                        if (!empty) return@customRule null
                    else ->
                        return@customRule null
                }

                val spaces = if (empty) 0 else spacesInSimpleFunction
                kotlinCommonSettings.createSpaceBeforeRBrace(spaces, psiElement.textRangeWithoutComments)
            }

            inPosition(parent = BLOCK, left = LBRACE).customRule { parent, _, _ ->
                val psiElement = parent.requireNode().treeParent.psi
                val funNode = psiElement as? KtFunction ?: return@customRule null

                if (funNode.name != null) return@customRule null

                // Empty block is covered in above rule
                Spacing.createDependentLFSpacing(
                    spacesInSimpleFunction, spacesInSimpleFunction, funNode.textRangeWithoutComments,
                    kotlinCommonSettings.KEEP_LINE_BREAKS,
                    kotlinCommonSettings.KEEP_BLANK_LINES_IN_CODE
                )
            }

            inPosition(parentSet = EXTEND_COLON_ELEMENTS, left = PRIMARY_CONSTRUCTOR, right = COLON).customRule { _, left, _ ->
                val primaryConstructor = left.requireNode().psi as KtPrimaryConstructor
                val rightParenthesis = primaryConstructor.valueParameterList?.rightParenthesis
                val prevSibling = rightParenthesis?.prevSibling
                val spaces = if (kotlinCustomSettings.SPACE_BEFORE_EXTEND_COLON) 1 else 0
                // TODO This should use DependentSpacingRule, but it doesn't set keepLineBreaks to false if max LFs is 0
                if ((prevSibling as? PsiWhiteSpace)?.textContains('\n') == true || kotlinCommonSettings
                        .METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE
                ) {
                    createSpacing(spaces, keepLineBreaks = false)
                } else {
                    createSpacing(spaces)
                }
            }

            inPosition(
                parent = CLASS_BODY,
                left = LBRACE,
                right = ENUM_ENTRY
            ).lineBreakIfLineBreakInParent(numSpacesOtherwise = 1)
        }

        simple {
            afterInside(LBRACE, BLOCK).lineBreakInCode()
            beforeInside(RBRACE, BLOCK).spacing(
                1, 0, 1,
                kotlinCommonSettings.KEEP_LINE_BREAKS,
                kotlinCommonSettings.KEEP_BLANK_LINES_BEFORE_RBRACE
            )
            between(LBRACE, ENUM_ENTRY).spacing(1, 0, 0, true, kotlinCommonSettings.KEEP_BLANK_LINES_IN_CODE)
            beforeInside(RBRACE, WHEN).lineBreakInCode()
            between(RPAR, BODY).spaces(1)

            // if when entry has block, spacing after arrow should be set by lbrace rule
            aroundInside(ARROW, WHEN_ENTRY).spaceIf(kotlinCustomSettings.SPACE_AROUND_WHEN_ARROW)

            beforeInside(COLON, EXTEND_COLON_ELEMENTS) { spaceIf(kotlinCustomSettings.SPACE_BEFORE_EXTEND_COLON) }

            after(EOL_COMMENT).lineBreakInCode()
        }
    }
}

private fun excludeLambdasAndObjects(parent: ASTBlock): List<TextRange> {
    val rangesToExclude = mutableListOf<TextRange>()
    parent.requireNode().psi.accept(object : KtTreeVisitorVoid() {
        override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
            super.visitLambdaExpression(lambdaExpression)
            rangesToExclude.add(lambdaExpression.textRange)
        }

        override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression) {
            super.visitObjectLiteralExpression(expression)
            rangesToExclude.add(expression.textRange)
        }

        override fun visitNamedFunction(function: KtNamedFunction) {
            super.visitNamedFunction(function)
            if (function.name == null) {
                rangesToExclude.add(function.textRange)
            }
        }
    })
    return TextRangeUtil.excludeRanges(parent.textRange, rangesToExclude).toList()
}

private val QUALIFIED_OPERATION = TokenSet.create(DOT, SAFE_ACCESS)
private val QUALIFIED_EXPRESSIONS = TokenSet.create(DOT_QUALIFIED_EXPRESSION, SAFE_ACCESS_EXPRESSION)
private val ELVIS_SET = TokenSet.create(ELVIS)
private val QUALIFIED_EXPRESSIONS_WITHOUT_WRAP = TokenSet.create(IMPORT_DIRECTIVE, PACKAGE_DIRECTIVE)

private const val KDOC_COMMENT_INDENT = 1

private val BINARY_EXPRESSIONS = TokenSet.create(BINARY_EXPRESSION, BINARY_WITH_TYPE, IS_EXPRESSION)
private val KDOC_CONTENT = TokenSet.create(KDocTokens.KDOC, KDocElementTypes.KDOC_SECTION, KDocElementTypes.KDOC_TAG)

private val CODE_BLOCKS = TokenSet.create(BLOCK, CLASS_BODY, FUNCTION_LITERAL)

private val ALIGN_FOR_BINARY_OPERATIONS = TokenSet.create(MUL, DIV, PERC, PLUS, MINUS, ELVIS, LT, GT, LTEQ, GTEQ, ANDAND, OROR)
private val ANNOTATIONS = TokenSet.create(ANNOTATION_ENTRY, ANNOTATION)

typealias WrappingStrategy = (childElement: ASTNode) -> Wrap?

fun noWrapping(childElement: ASTNode): Wrap? = null

abstract class KotlinCommonBlock(
    private val node: ASTNode,
    private val settings: CodeStyleSettings,
    private val spacingBuilder: KotlinSpacingBuilder,
    private val alignmentStrategy: CommonAlignmentStrategy,
    private val overrideChildren: Sequence<ASTNode>? = null,
) {
    @Volatile
    private var mySubBlocks: List<ASTBlock>? = null

    fun getTextRange(): TextRange {
        if (overrideChildren != null) {
            return TextRange(overrideChildren.first().startOffset, overrideChildren.last().textRange.endOffset)
        }
        return node.textRange
    }

    protected abstract fun createBlock(
        node: ASTNode,
        alignmentStrategy: CommonAlignmentStrategy,
        indent: Indent?,
        wrap: Wrap?,
        settings: CodeStyleSettings,
        spacingBuilder: KotlinSpacingBuilder,
        overrideChildren: Sequence<ASTNode>? = null,
    ): ASTBlock

    protected abstract fun createSyntheticSpacingNodeBlock(node: ASTNode): ASTBlock

    protected abstract fun getSubBlocks(): List<Block>

    protected abstract fun getSuperChildAttributes(newChildIndex: Int): ChildAttributes

    protected abstract fun isIncompleteInSuper(): Boolean

    protected abstract fun getAlignmentForCaseBranch(shouldAlignInColumns: Boolean): CommonAlignmentStrategy

    protected abstract fun getAlignment(): Alignment?

    protected abstract fun createAlignmentStrategy(alignOption: Boolean, defaultAlignment: Alignment?): CommonAlignmentStrategy

    protected abstract fun getNullAlignmentStrategy(): CommonAlignmentStrategy

    fun isLeaf(): Boolean = node.firstChildNode == null

    fun isIncomplete(): Boolean {
        if (isIncompleteInSuper()) {
            return true
        }

        // An incomplete declaration is the reason when modifier list can become a class body child, otherwise
        // it's going to be a declaration child.
        return node.elementType == MODIFIER_LIST && node.treeParent?.elementType == CLASS_BODY
    }

    fun buildChildren(): List<Block> {
        if (mySubBlocks != null) {
            return mySubBlocks!!
        }

        var nodeSubBlocks = buildSubBlocks()

        if (node.elementType in QUALIFIED_EXPRESSIONS) {
            nodeSubBlocks = splitSubBlocksOnDot(nodeSubBlocks)
        } else {
            val psi = node.psi
            if (psi is KtBinaryExpression && psi.operationToken == ELVIS) {
                nodeSubBlocks = splitSubBlocksOnElvis(nodeSubBlocks)
            }
        }

        mySubBlocks = nodeSubBlocks

        return nodeSubBlocks
    }

    private fun splitSubBlocksOnDot(nodeSubBlocks: List<ASTBlock>): List<ASTBlock> {
        if (node.treeParent?.isQualifier == true || node.isCallChainWithoutWrap) return nodeSubBlocks

        val operationBlockIndex = nodeSubBlocks.indexOfBlockWithType(QUALIFIED_OPERATION)
        if (operationBlockIndex == -1) return nodeSubBlocks

        val block = nodeSubBlocks.first()
        val wrap = createWrapForQualifierExpression(node)
        val enforceIndentToChildren = anyCallInCallChainIsWrapped(node)
        val indent = createIndentForQualifierExpression(enforceIndentToChildren)
        val newBlock = block.processBlock(wrap, enforceIndentToChildren)
        return nodeSubBlocks.replaceBlock(newBlock, 0).splitAtIndex(operationBlockIndex, indent, wrap)
    }

    private fun ASTBlock.processBlock(wrap: Wrap?, enforceIndentToChildren: Boolean): ASTBlock {
        val currentNode = requireNode()
        val enforceIndent = enforceIndentToChildren && anyCallInCallChainIsWrapped(currentNode)
        val indent = createIndentForQualifierExpression(enforceIndent)

        @Suppress("UNCHECKED_CAST")
        val subBlocks = subBlocks as List<ASTBlock>
        val elementType = currentNode.elementType
        if (elementType != POSTFIX_EXPRESSION && elementType !in QUALIFIED_EXPRESSIONS) return this

        val index = 0
        val resultWrap = if (currentNode.wrapForFirstCallInChainIsAllowed)
            wrap ?: createWrapForQualifierExpression(currentNode)
        else
            null

        val newBlock = subBlocks.elementAt(index).processBlock(resultWrap, enforceIndent)
        return subBlocks.replaceBlock(newBlock, index).let {
            val operationIndex = subBlocks.indexOfBlockWithType(QUALIFIED_OPERATION)
            if (operationIndex != -1)
                it.splitAtIndex(operationIndex, indent, resultWrap)
            else
                it
        }.wrapToBlock(currentNode, this)
    }

    private fun List<ASTBlock>.replaceBlock(block: ASTBlock, index: Int = 0): List<ASTBlock> = toMutableList().apply { this[index] = block }

    private val ASTNode.wrapForFirstCallInChainIsAllowed: Boolean
        get() {
            if (unwrapQualifier()?.isCall != true) return false
            return settings.kotlinCommonSettings.WRAP_FIRST_METHOD_IN_CALL_CHAIN || receiverIsCall()
        }

    private fun createWrapForQualifierExpression(node: ASTNode): Wrap? =
        if (node.wrapForFirstCallInChainIsAllowed && node.receiverIsCall())
            Wrap.createWrap(
                settings.kotlinCommonSettings.METHOD_CALL_CHAIN_WRAP,
                true /* wrapFirstElement */,
            )
        else
            null

    // enforce indent to children when there's a line break before the dot in any call in the chain (meaning that
    // the call chain following that call is indented)
    private fun createIndentForQualifierExpression(enforceIndentToChildren: Boolean): Indent {
        val indentType = if (settings.kotlinCustomSettings.CONTINUATION_INDENT_FOR_CHAINED_CALLS) {
            if (enforceIndentToChildren) Indent.Type.CONTINUATION else Indent.Type.CONTINUATION_WITHOUT_FIRST
        } else {
            Indent.Type.NORMAL
        }

        return Indent.getIndent(
            indentType, false,
            enforceIndentToChildren,
        )
    }

    private fun List<ASTBlock>.wrapToBlock(
        anchor: ASTNode?,
        parentBlock: ASTBlock?,
    ): ASTBlock = splitAtIndex(0, null, null, anchor, parentBlock).single()

    private fun List<ASTBlock>.splitAtIndex(
        index: Int,
        indent: Indent?,
        wrap: Wrap?,
        anchor: ASTNode? = null,
        parentBlock: ASTBlock? = null,
    ): List<ASTBlock> {
        val operationBlock = this[index]
        val createParentSyntheticSpacingBlock: (ASTNode) -> ASTBlock = if (parentBlock != null) {
            { parentBlock }
        } else {
            {
                val parent = it.treeParent ?: node
                val skipOperationNodeParent = if (parent.elementType === OPERATION_REFERENCE) {
                    parent.treeParent ?: parent
                } else {
                    parent
                }
                createSyntheticSpacingNodeBlock(skipOperationNodeParent)
            }
        }
        val operationSyntheticBlock = SyntheticKotlinBlock(
            anchor ?: operationBlock.requireNode(),
            subList(index, size),
            null, indent, wrap, spacingBuilder, createParentSyntheticSpacingBlock,
        )

        return subList(0, index) + operationSyntheticBlock
    }

    private fun splitSubBlocksOnElvis(nodeSubBlocks: List<ASTBlock>): List<ASTBlock> {
        val elvisIndex = nodeSubBlocks.indexOfBlockWithType(ELVIS_SET)
        if (elvisIndex >= 0) {
            val indent = if (settings.kotlinCustomSettings.CONTINUATION_INDENT_IN_ELVIS) {
                Indent.getContinuationIndent()
            } else {
                Indent.getNormalIndent()
            }

            return nodeSubBlocks.splitAtIndex(
                elvisIndex,
                indent,
                null,
            )
        }
        return nodeSubBlocks
    }

    private fun createChildIndent(child: ASTNode): Indent? {
        val childParent = child.treeParent
        val childType = child.elementType

        if (childParent != null && isInCodeChunk(childParent)) {
            return Indent.getNoneIndent()
        }

        // do not indent child after heading comments inside declaration
        if (childParent != null && childParent.psi is KtDeclaration) {
            val prev = getPrevWithoutWhitespace(child)
            if (prev != null && COMMENTS.contains(prev.elementType) && getSiblingWithoutWhitespaceAndComments(prev) == null) {
                return Indent.getNoneIndent()
            }
        }

        for (strategy in INDENT_RULES) {
            val indent = strategy.getIndent(child, settings)
            if (indent != null) {
                return indent
            }
        }

        // TODO: Try to rewrite other rules to declarative style
        if (childParent != null) {
            val parentType = childParent.elementType

            if (parentType === VALUE_PARAMETER_LIST || parentType === VALUE_ARGUMENT_LIST) {
                val prev = getPrevWithoutWhitespace(child)
                if (childType === RPAR && (prev == null || prev.elementType !== COMMA || !hasDoubleLineBreakBefore(child))) {
                    return Indent.getNoneIndent()
                }

                return if (settings.kotlinCustomSettings.CONTINUATION_INDENT_IN_ARGUMENT_LISTS)
                    Indent.getContinuationWithoutFirstIndent()
                else
                    Indent.getNormalIndent()
            }

            if (parentType === TYPE_PARAMETER_LIST || parentType === TYPE_ARGUMENT_LIST) {
                return Indent.getContinuationWithoutFirstIndent()
            }
        }

        return Indent.getNoneIndent()
    }

    private fun isInCodeChunk(node: ASTNode): Boolean {
        val parent = node.treeParent ?: return false

        if (node.elementType != BLOCK) {
            return false
        }

        val parentType = parent.elementType
        return parentType == SCRIPT
                || parentType == BLOCK_CODE_FRAGMENT
                || parentType == EXPRESSION_CODE_FRAGMENT
                || parentType == TYPE_CODE_FRAGMENT
    }

    fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        val type = node.elementType

        if (isInCodeChunk(node)) {
            return ChildAttributes(Indent.getNoneIndent(), null)
        }

        if (type == IF) {
            val elseBlock = mySubBlocks?.getOrNull(newChildIndex)
            if (elseBlock != null && elseBlock.requireNode().elementType == ELSE_KEYWORD) {
                return ChildAttributes.DELEGATE_TO_NEXT_CHILD
            }
        }

        if (newChildIndex > 0) {
            val prevBlock = mySubBlocks?.get(newChildIndex - 1)
            if (prevBlock?.node?.elementType == MODIFIER_LIST) {
                return ChildAttributes(Indent.getNoneIndent(), null)
            }
        }

        return when (type) {
            in CODE_BLOCKS, WHEN, IF, FOR, WHILE, DO_WHILE, WHEN_ENTRY -> ChildAttributes(
                Indent.getNormalIndent(),
                null,
            )

            TRY -> ChildAttributes(Indent.getNoneIndent(), null)

            in QUALIFIED_EXPRESSIONS -> ChildAttributes(Indent.getContinuationWithoutFirstIndent(), null)

            VALUE_PARAMETER_LIST, VALUE_ARGUMENT_LIST -> {
                val subBlocks = getSubBlocks()
                if (newChildIndex != 1 && newChildIndex != 0 && newChildIndex < subBlocks.size) {
                    val block = subBlocks[newChildIndex]
                    ChildAttributes(block.indent, block.alignment)
                } else {
                    val indent =
                        if ((type == VALUE_PARAMETER_LIST && !settings.kotlinCustomSettings.CONTINUATION_INDENT_IN_PARAMETER_LISTS) ||
                            (type == VALUE_ARGUMENT_LIST && !settings.kotlinCustomSettings.CONTINUATION_INDENT_IN_ARGUMENT_LISTS)
                        ) {
                            Indent.getNormalIndent()
                        } else {
                            Indent.getContinuationIndent()
                        }
                    ChildAttributes(indent, null)
                }
            }

            DOC_COMMENT -> ChildAttributes(Indent.getSpaceIndent(KDOC_COMMENT_INDENT), null)

            PARENTHESIZED -> getSuperChildAttributes(newChildIndex)

            else -> {
                val blocks = getSubBlocks()
                if (newChildIndex != 0) {
                    val isIncomplete = if (newChildIndex < blocks.size) blocks[newChildIndex - 1].isIncomplete else isIncompleteInSuper()
                    if (isIncomplete) {
                        if (blocks.size == newChildIndex && !settings.kotlinCustomSettings.CONTINUATION_INDENT_FOR_EXPRESSION_BODIES) {
                            val lastInParent = blocks.last()
                            if (lastInParent is ASTBlock && lastInParent.node?.elementType in ALL_ASSIGNMENTS) {
                                return ChildAttributes(Indent.getNormalIndent(), null)
                            }
                        }

                        return getSuperChildAttributes(newChildIndex)
                    }
                }

                if (blocks.size > newChildIndex) {
                    val block = blocks[newChildIndex]
                    return ChildAttributes(block.indent, block.alignment)
                }

                ChildAttributes(Indent.getNoneIndent(), null)
            }
        }
    }

    private fun getChildrenAlignmentStrategy(): CommonAlignmentStrategy {
        val kotlinCommonSettings = settings.kotlinCommonSettings
        val kotlinCustomSettings = settings.kotlinCustomSettings
        val parentType = node.elementType
        return when {
            parentType === VALUE_PARAMETER_LIST ->
                getAlignmentForChildInParenthesis(
                    kotlinCommonSettings.ALIGN_MULTILINE_PARAMETERS, VALUE_PARAMETER, COMMA,
                    kotlinCommonSettings.ALIGN_MULTILINE_METHOD_BRACKETS, LPAR, RPAR,
                )

            parentType === VALUE_ARGUMENT_LIST ->
                getAlignmentForChildInParenthesis(
                    kotlinCommonSettings.ALIGN_MULTILINE_PARAMETERS_IN_CALLS, VALUE_ARGUMENT, COMMA,
                    kotlinCommonSettings.ALIGN_MULTILINE_METHOD_BRACKETS, LPAR, RPAR,
                )

            parentType === WHEN ->
                getAlignmentForCaseBranch(kotlinCustomSettings.ALIGN_IN_COLUMNS_CASE_BRANCH)

            parentType === WHEN_ENTRY ->
                alignmentStrategy

            parentType in BINARY_EXPRESSIONS && getOperationType(node) in ALIGN_FOR_BINARY_OPERATIONS ->
                createAlignmentStrategy(kotlinCommonSettings.ALIGN_MULTILINE_BINARY_OPERATION, getAlignment())

            parentType === SUPER_TYPE_LIST ->
                createAlignmentStrategy(kotlinCommonSettings.ALIGN_MULTILINE_EXTENDS_LIST, getAlignment())

            parentType === PARENTHESIZED ->
                object : CommonAlignmentStrategy() {
                    private var bracketsAlignment: Alignment? =
                        if (kotlinCommonSettings.ALIGN_MULTILINE_BINARY_OPERATION) Alignment.createAlignment() else null

                    override fun getAlignment(node: ASTNode): Alignment? {
                        val childNodeType = node.elementType
                        val prev = getPrevWithoutWhitespace(node)

                        if (prev != null && prev.elementType === TokenType.ERROR_ELEMENT || childNodeType === TokenType.ERROR_ELEMENT) {
                            return bracketsAlignment
                        }

                        if (childNodeType === LPAR || childNodeType === RPAR) {
                            return bracketsAlignment
                        }

                        return null
                    }
                }

            parentType == TYPE_CONSTRAINT_LIST ->
                createAlignmentStrategy(true, getAlignment())

            else ->
                getNullAlignmentStrategy()
        }
    }


    private fun buildSubBlock(
        child: ASTNode,
        alignmentStrategy: CommonAlignmentStrategy,
        wrappingStrategy: WrappingStrategy,
        overrideChildren: Sequence<ASTNode>? = null,
    ): ASTBlock {
        val childWrap = wrappingStrategy(child)

        // Skip one sub-level for operators, so type of block node is an element type of operator
        if (child.elementType === OPERATION_REFERENCE) {
            val operationNode = child.firstChildNode
            if (operationNode != null) {
                return createBlock(
                    operationNode,
                    alignmentStrategy,
                    createChildIndent(child),
                    childWrap,
                    settings,
                    spacingBuilder,
                    overrideChildren,
                )
            }
        }

        return createBlock(child, alignmentStrategy, createChildIndent(child), childWrap, settings, spacingBuilder, overrideChildren)
    }

    private fun buildSubBlocks(): List<ASTBlock> {
        val childrenAlignmentStrategy = getChildrenAlignmentStrategy()
        val wrappingStrategy = getWrappingStrategy()

        val childNodes = when {
            overrideChildren != null -> overrideChildren.asSequence()
            node.elementType == BINARY_EXPRESSION -> {
                val binaryExpression = node.psi as? KtBinaryExpression
                if (binaryExpression != null && ALL_ASSIGNMENTS.contains(binaryExpression.operationToken)) {
                    node.children()
                } else {
                    val binaryExpressionChildren = mutableListOf<ASTNode>()
                    collectBinaryExpressionChildren(node, binaryExpressionChildren)
                    binaryExpressionChildren.asSequence()
                }
            }
            else -> node.children()
        }

        return childNodes
            .filter { it.textRange.length > 0 && it.elementType != TokenType.WHITE_SPACE }
            .flatMap { buildSubBlocksForChildNode(it, childrenAlignmentStrategy, wrappingStrategy) }
            .toList()
    }

    private fun buildSubBlocksForChildNode(
        node: ASTNode,
        childrenAlignmentStrategy: CommonAlignmentStrategy,
        wrappingStrategy: WrappingStrategy,
    ): Sequence<ASTBlock> {
        if (node.elementType == FUN && false /* TODO fix tests and restore */) {
            val filteredChildren = node.children().filter {
                it.textRange.length > 0 && it.elementType != TokenType.WHITE_SPACE
            }
            val significantChildren = filteredChildren.dropWhile { it.elementType == EOL_COMMENT }
            val funIndent = extractIndent(significantChildren.first())
            val eolComments = filteredChildren.takeWhile {
                it.elementType == EOL_COMMENT && extractIndent(it) != funIndent
            }.toList()
            val remainingChildren = filteredChildren.drop(eolComments.size)

            val blocks = eolComments.map { buildSubBlock(it, childrenAlignmentStrategy, wrappingStrategy) } +
                    sequenceOf(buildSubBlock(node, childrenAlignmentStrategy, wrappingStrategy, remainingChildren))
            val blockList = blocks.toList()
            return blockList.asSequence()
        }

        return sequenceOf(buildSubBlock(node, childrenAlignmentStrategy, wrappingStrategy))
    }

    private fun collectBinaryExpressionChildren(node: ASTNode, result: MutableList<ASTNode>) {
        for (child in node.children()) {
            if (child.elementType == BINARY_EXPRESSION) {
                collectBinaryExpressionChildren(child, result)
            } else {
                result.add(child)
            }
        }
    }

    private fun getWrappingStrategy(): WrappingStrategy {
        val commonSettings = settings.kotlinCommonSettings
        val elementType = node.elementType
        val parentElementType = node.treeParent?.elementType
        val nodePsi = node.psi

        when {
            elementType === VALUE_ARGUMENT_LIST -> {
                val wrapSetting = commonSettings.CALL_PARAMETERS_WRAP
                if (!node.addTrailingComma &&
                    (wrapSetting == CommonCodeStyleSettings.WRAP_AS_NEEDED || wrapSetting == CommonCodeStyleSettings.WRAP_ON_EVERY_ITEM) &&
                    !needWrapArgumentList(nodePsi)
                ) {
                    return ::noWrapping
                }
                return getWrappingStrategyForItemList(
                    wrapSetting,
                    VALUE_ARGUMENT,
                    node.addTrailingComma,
                    additionalWrap = trailingCommaWrappingStrategyWithMultiLineCheck(LPAR, RPAR),
                )
            }

            elementType === VALUE_PARAMETER_LIST -> {
                when (parentElementType) {
                    FUN, PRIMARY_CONSTRUCTOR, SECONDARY_CONSTRUCTOR -> return getWrappingStrategyForItemList(
                        commonSettings.METHOD_PARAMETERS_WRAP,
                        VALUE_PARAMETER,
                        node.addTrailingComma,
                        additionalWrap = trailingCommaWrappingStrategyWithMultiLineCheck(LPAR, RPAR),
                    )
                    FUNCTION_TYPE -> return defaultTrailingCommaWrappingStrategy(LPAR, RPAR)
                    FUNCTION_LITERAL -> {
                        if (nodePsi.parent?.safeAs<KtFunctionLiteral>()?.needTrailingComma(settings) == true) {
                            val check = thisOrPrevIsMultiLineElement(COMMA, LBRACE /* not necessary */, ARROW /* not necessary */)
                            return { childElement ->
                                createWrapAlwaysIf(getSiblingWithoutWhitespaceAndComments(childElement) == null || check(childElement))
                            }
                        }
                    }
                }
            }

            elementType === FUNCTION_LITERAL -> {
                if (nodePsi.cast<KtFunctionLiteral>().needTrailingComma(settings))
                    return trailingCommaWrappingStrategy(leftAnchor = LBRACE, rightAnchor = ARROW)
            }

            elementType === WHEN_ENTRY -> {
                // with argument
                if (nodePsi.cast<KtWhenEntry>().needTrailingComma(settings)) {
                    val check = thisOrPrevIsMultiLineElement(COMMA, LBRACE /* not necessary */, ARROW /* not necessary */)
                    return trailingCommaWrappingStrategy(rightAnchor = ARROW) {
                        getSiblingWithoutWhitespaceAndComments(it, true) != null && check(it)
                    }
                }
            }

            elementType === DESTRUCTURING_DECLARATION -> {
                nodePsi as KtDestructuringDeclaration
                if (nodePsi.valOrVarKeyword == null) return defaultTrailingCommaWrappingStrategy(LPAR, RPAR)
                else if (nodePsi.needTrailingComma(settings)) {
                    val check = thisOrPrevIsMultiLineElement(COMMA, LPAR, RPAR)
                    return trailingCommaWrappingStrategy(leftAnchor = LPAR, rightAnchor = RPAR, filter = { it.elementType !== EQ }) {
                        getSiblingWithoutWhitespaceAndComments(it, true) != null && check(it)
                    }
                }
            }

            elementType === INDICES -> return defaultTrailingCommaWrappingStrategy(LBRACKET, RBRACKET)

            elementType === TYPE_PARAMETER_LIST -> return defaultTrailingCommaWrappingStrategy(LT, GT)

            elementType === TYPE_ARGUMENT_LIST -> return defaultTrailingCommaWrappingStrategy(LT, GT)

            elementType === COLLECTION_LITERAL_EXPRESSION -> return defaultTrailingCommaWrappingStrategy(LBRACKET, RBRACKET)

            elementType === SUPER_TYPE_LIST -> {
                val wrap = Wrap.createWrap(commonSettings.EXTENDS_LIST_WRAP, false)
                return { childElement -> if (childElement.psi is KtSuperTypeListEntry) wrap else null }
            }

            elementType === CLASS_BODY -> return getWrappingStrategyForItemList(commonSettings.ENUM_CONSTANTS_WRAP, ENUM_ENTRY)

            elementType === MODIFIER_LIST -> {
                when (val parent = node.treeParent.psi) {
                    is KtParameter ->
                        return getWrappingStrategyForItemList(
                            commonSettings.PARAMETER_ANNOTATION_WRAP,
                            ANNOTATIONS,
                            !node.treeParent.isFirstParameter(),
                        )
                    is KtClassOrObject, is KtTypeAlias ->
                        return getWrappingStrategyForItemList(
                            commonSettings.CLASS_ANNOTATION_WRAP,
                            ANNOTATIONS,
                        )

                    is KtNamedFunction, is KtSecondaryConstructor ->
                        return getWrappingStrategyForItemList(
                            commonSettings.METHOD_ANNOTATION_WRAP,
                            ANNOTATIONS,
                        )

                    is KtProperty ->
                        return getWrappingStrategyForItemList(
                            if (parent.isLocal)
                                commonSettings.VARIABLE_ANNOTATION_WRAP
                            else
                                commonSettings.FIELD_ANNOTATION_WRAP,
                            ANNOTATIONS,
                        )
                }
            }

            elementType === VALUE_PARAMETER ->
                return wrapAfterAnnotation(commonSettings.PARAMETER_ANNOTATION_WRAP)

            nodePsi is KtClassOrObject || nodePsi is KtTypeAlias ->
                return wrapAfterAnnotation(commonSettings.CLASS_ANNOTATION_WRAP)

            nodePsi is KtNamedFunction || nodePsi is KtSecondaryConstructor ->
                return wrap@{ childElement ->
                    getWrapAfterAnnotation(childElement, commonSettings.METHOD_ANNOTATION_WRAP)?.let {
                        return@wrap it
                    }
                    if (getSiblingWithoutWhitespaceAndComments(childElement)?.elementType == EQ) {
                        return@wrap Wrap.createWrap(settings.kotlinCustomSettings.WRAP_EXPRESSION_BODY_FUNCTIONS, true)
                    }
                    null
                }

            nodePsi is KtProperty ->
                return wrap@{ childElement ->
                    val wrapSetting = if (nodePsi.isLocal) commonSettings.VARIABLE_ANNOTATION_WRAP else commonSettings.FIELD_ANNOTATION_WRAP
                    getWrapAfterAnnotation(childElement, wrapSetting)?.let {
                        return@wrap it
                    }
                    if (getSiblingWithoutWhitespaceAndComments(childElement)?.elementType == EQ) {
                        return@wrap Wrap.createWrap(settings.kotlinCommonSettings.ASSIGNMENT_WRAP, true)
                    }
                    null
                }

            nodePsi is KtBinaryExpression -> {
                if (nodePsi.operationToken == EQ) {
                    return { childElement ->
                        if (getSiblingWithoutWhitespaceAndComments(childElement)?.elementType == OPERATION_REFERENCE) {
                            Wrap.createWrap(settings.kotlinCommonSettings.ASSIGNMENT_WRAP, true)
                        } else {
                            null
                        }
                    }
                }
                if (nodePsi.operationToken == ELVIS && nodePsi.getStrictParentOfType<KtStringTemplateExpression>() == null) {
                    return { childElement ->
                        if (childElement.elementType == OPERATION_REFERENCE && (childElement.psi as? KtOperationReferenceExpression)?.operationSignTokenType == ELVIS) {
                            Wrap.createWrap(settings.kotlinCustomSettings.WRAP_ELVIS_EXPRESSIONS, true)
                        } else {
                            null
                        }
                    }
                }
                return ::noWrapping
            }
        }

        return ::noWrapping
    }

    private fun defaultTrailingCommaWrappingStrategy(leftAnchor: IElementType, rightAnchor: IElementType): WrappingStrategy =
        fun(childElement: ASTNode): Wrap? = trailingCommaWrappingStrategyWithMultiLineCheck(leftAnchor, rightAnchor)(childElement)

    private val ASTNode.addTrailingComma: Boolean
        get() = (settings.kotlinCustomSettings.addTrailingCommaIsAllowedFor(this) ||
                lastChildNode?.let { getSiblingWithoutWhitespaceAndComments(it) }?.elementType === COMMA) &&
                psi?.let(PsiElement::isMultiline) == true


    private fun ASTNode.notDelimiterSiblingNodeInSequence(
        forward: Boolean,
        delimiterType: IElementType,
        typeOfLastElement: IElementType,
    ): ASTNode? {
        var sibling: ASTNode? = null
        for (element in siblings(forward).filter { it.elementType != WHITE_SPACE }.takeWhile { it.elementType != typeOfLastElement }) {
            val elementType = element.elementType
            if (!forward) {
                sibling = element
                if (elementType != delimiterType && elementType !in COMMENTS) break
            } else {
                if (elementType !in COMMENTS) break
                sibling = element
            }
        }

        return sibling
    }

    private fun thisOrPrevIsMultiLineElement(
        delimiterType: IElementType,
        typeOfFirstElement: IElementType,
        typeOfLastElement: IElementType,
    ) = fun(childElement: ASTNode): Boolean {
        when (childElement.elementType) {
            typeOfFirstElement,
            typeOfLastElement,
            delimiterType,
            in WHITE_SPACE_OR_COMMENT_BIT_SET,
            -> return false
        }

        val psi = childElement.psi ?: return false
        if (psi.isMultiline()) return true

        val startOffset = childElement.notDelimiterSiblingNodeInSequence(false, delimiterType, typeOfFirstElement)?.startOffset
            ?: psi.startOffset
        val endOffset = childElement.notDelimiterSiblingNodeInSequence(true, delimiterType, typeOfLastElement)?.psi?.endOffset
            ?: psi.endOffset
        return psi.parent.containsLineBreakInThis(startOffset, endOffset)
    }

    private fun trailingCommaWrappingStrategyWithMultiLineCheck(
        leftAnchor: IElementType,
        rightAnchor: IElementType,
    ) = trailingCommaWrappingStrategy(
        leftAnchor = leftAnchor,
        rightAnchor = rightAnchor,
        checkTrailingComma = true,
        additionalCheck = thisOrPrevIsMultiLineElement(COMMA, leftAnchor, rightAnchor),
    )

    private fun trailingCommaWrappingStrategy(
        leftAnchor: IElementType? = null,
        rightAnchor: IElementType? = null,
        checkTrailingComma: Boolean = false,
        filter: (ASTNode) -> Boolean = { true },
        additionalCheck: (ASTNode) -> Boolean = { false },
    ): WrappingStrategy = fun(childElement: ASTNode): Wrap? {
        if (!filter(childElement)) return null
        val childElementType = childElement.elementType
        return createWrapAlwaysIf(
            (!checkTrailingComma || childElement.treeParent.addTrailingComma) && (
                    rightAnchor != null && rightAnchor === childElementType ||
                            leftAnchor != null && leftAnchor === getSiblingWithoutWhitespaceAndComments(childElement)?.elementType ||
                            additionalCheck(childElement)
                    ),
        )
    }
}

private fun ASTNode.qualifierReceiver(): ASTNode? = unwrapQualifier()?.psi
    ?.safeAs<KtQualifiedExpression>()
    ?.receiverExpression
    ?.node
    ?.unwrapQualifier()

private tailrec fun ASTNode.unwrapQualifier(): ASTNode? {
    if (elementType in QUALIFIED_EXPRESSIONS) return this

    val psi = psi as? KtPostfixExpression ?: return null
    if (psi.operationToken != EXCLEXCL) return null

    return psi.baseExpression?.node?.unwrapQualifier()
}

private fun ASTNode.receiverIsCall(): Boolean = qualifierReceiver()?.isCall == true

private val ASTNode.isCallChainWithoutWrap: Boolean
    get() {
        val callChainParent = parents().firstOrNull { !it.isQualifier } ?: return true
        return callChainParent.elementType in QUALIFIED_EXPRESSIONS_WITHOUT_WRAP
    }

private val ASTNode.isQualifier: Boolean
    get() {
        var currentNode: ASTNode? = this
        while (currentNode != null) {
            if (currentNode.elementType in QUALIFIED_EXPRESSIONS) return true
            if (currentNode.psi?.safeAs<KtPostfixExpression>()?.operationToken != EXCLEXCL) return false

            currentNode = currentNode.treeParent
        }

        return false
    }

private val ASTNode.isCall: Boolean
    get() = unwrapQualifier()?.lastChildNode?.elementType == CALL_EXPRESSION

private fun anyCallInCallChainIsWrapped(node: ASTNode): Boolean {
    val sequentialNodes = generateSequence(node) {
        when (it.elementType) {
            POSTFIX_EXPRESSION, in QUALIFIED_EXPRESSIONS -> it.firstChildNode
            PARENTHESIZED -> getSiblingWithoutWhitespaceAndComments(it.firstChildNode, true)
            else -> null
        }
    }

    return sequentialNodes.any {
        val checkedElement = when (it.elementType) {
            in QUALIFIED_EXPRESSIONS -> it.findChildByType(QUALIFIED_OPERATION)
            PARENTHESIZED -> it.lastChildNode
            else -> null
        }

        checkedElement != null && hasLineBreakBefore(checkedElement)
    }
}

private fun ASTNode.isFirstParameter(): Boolean = treePrev?.elementType == LPAR

private fun wrapAfterAnnotation(wrapType: Int): WrappingStrategy {
    return { childElement -> getWrapAfterAnnotation(childElement, wrapType) }
}

private fun getWrapAfterAnnotation(childElement: ASTNode, wrapType: Int): Wrap? {
    if (childElement.elementType in COMMENTS) return null
    var prevLeaf = childElement.treePrev
    while (prevLeaf?.elementType == TokenType.WHITE_SPACE) {
        prevLeaf = prevLeaf.treePrev
    }
    if (prevLeaf?.elementType == MODIFIER_LIST) {
        if (prevLeaf?.lastChildNode?.elementType in ANNOTATIONS) {
            return Wrap.createWrap(wrapType, true)
        }
    }
    return null
}

fun needWrapArgumentList(psi: PsiElement): Boolean {
    val args = (psi as? KtValueArgumentList)?.arguments
    return args?.singleOrNull()?.getArgumentExpression() !is KtObjectLiteralExpression
}

private fun hasLineBreakBefore(node: ASTNode): Boolean {
    val prevSibling = node.leaves(false)
        .dropWhile { it.psi is PsiComment }
        .firstOrNull()
    return prevSibling?.elementType == TokenType.WHITE_SPACE && prevSibling?.textContains('\n') == true
}

private fun hasDoubleLineBreakBefore(node: ASTNode): Boolean {
    val prevSibling = node.leaves(false).firstOrNull() ?: return false

    return prevSibling.text.count { it == '\n' } >= 2
}

fun NodeIndentStrategy.PositionStrategy.continuationIf(
    option: (KotlinCodeStyleSettings) -> Boolean,
    indentFirst: Boolean = false,
): NodeIndentStrategy {
    return set { settings ->
        if (option(settings.kotlinCustomSettings)) {
            if (indentFirst)
                Indent.getContinuationIndent()
            else
                Indent.getContinuationWithoutFirstIndent()
        } else
            Indent.getNormalIndent()
    }
}

private val INDENT_RULES = arrayOf(
    strategy("No indent for braces in blocks")
        .within(BLOCK, CLASS_BODY, FUNCTION_LITERAL)
        .forType(RBRACE, LBRACE)
        .set(Indent.getNoneIndent()),

    strategy("Indent for block content")
        .within(BLOCK, CLASS_BODY, FUNCTION_LITERAL)
        .notForType(RBRACE, LBRACE, BLOCK)
        .set(Indent.getNormalIndent()),

    strategy("Indent for template content")
        .within(LONG_STRING_TEMPLATE_ENTRY)
        .notForType(LONG_TEMPLATE_ENTRY_START, LONG_TEMPLATE_ENTRY_END)
        .set(Indent.getNormalIndent()),

    strategy("No indent for braces in template")
        .within(LONG_STRING_TEMPLATE_ENTRY)
        .forType(LONG_TEMPLATE_ENTRY_START, LONG_TEMPLATE_ENTRY_END)
        .set(Indent.getNoneIndent()),

    strategy("Indent for property accessors")
        .within(PROPERTY).forType(PROPERTY_ACCESSOR)
        .set(Indent.getNormalIndent()),

    strategy("For a single statement in 'for'")
        .within(BODY).notForType(BLOCK)
        .set(Indent.getNormalIndent()),

    strategy("For WHEN content")
        .within(WHEN)
        .notForType(RBRACE, LBRACE, WHEN_KEYWORD)
        .set(Indent.getNormalIndent()),

    strategy("For single statement in THEN and ELSE")
        .within(THEN, ELSE).notForType(BLOCK)
        .set(Indent.getNormalIndent()),

    strategy("Expression body")
        .within(FUN)
        .forElement {
            (it.psi is KtExpression && it.psi !is KtBlockExpression)
        }
        .continuationIf(KotlinCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES, indentFirst = true),

    strategy("Line comment at expression body position")
        .forElement { node ->
            val psi = node.psi
            val parent = psi.parent
            if (psi is PsiComment && parent is KtDeclarationWithInitializer) {
                psi.getNextSiblingIgnoringWhitespace() == parent.initializer
            } else {
                false
            }
        }
        .continuationIf(KotlinCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES, indentFirst = true),

    strategy("If condition")
        .within(CONDITION)
        .set { settings ->
            val indentType = if (settings.kotlinCustomSettings.CONTINUATION_INDENT_IN_IF_CONDITIONS)
                Indent.Type.CONTINUATION
            else
                Indent.Type.NORMAL
            Indent.getIndent(indentType, false, true)
        },

    strategy("Property accessor expression body")
        .within(PROPERTY_ACCESSOR)
        .forElement {
            it.psi is KtExpression && it.psi !is KtBlockExpression
        }
        .set(Indent.getNormalIndent()),

    strategy("Property initializer")
        .within(PROPERTY)
        .forElement {
            it.psi is KtExpression
        }
        .continuationIf(KotlinCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES),

    strategy("Destructuring declaration")
        .within(DESTRUCTURING_DECLARATION)
        .forElement {
            it.psi is KtExpression
        }
        .continuationIf(KotlinCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES),

    strategy("Assignment expressions")
        .within(BINARY_EXPRESSION)
        .within {
            val binaryExpression = it.psi as? KtBinaryExpression
                ?: return@within false

            return@within ALL_ASSIGNMENTS.contains(binaryExpression.operationToken)
        }
        .forElement {
            val psi = it.psi
            val binaryExpression = psi?.parent as? KtBinaryExpression
            binaryExpression?.right == psi
        }
        .continuationIf(KotlinCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES),

    strategy("Indent for parts")
        .within(PROPERTY, FUN, DESTRUCTURING_DECLARATION, SECONDARY_CONSTRUCTOR)
        .notForType(
            BLOCK, FUN_KEYWORD, VAL_KEYWORD, VAR_KEYWORD, CONSTRUCTOR_KEYWORD, RPAR,
            EOL_COMMENT,
        )
        .set(Indent.getContinuationWithoutFirstIndent()),

    strategy("Chained calls")
        .within(QUALIFIED_EXPRESSIONS)
        .forType(EOL_COMMENT, BLOCK_COMMENT, DOC_COMMENT, SHEBANG_COMMENT)
        .continuationIf(KotlinCodeStyleSettings::CONTINUATION_INDENT_FOR_CHAINED_CALLS),

    strategy("Colon of delegation list")
        .within(CLASS, OBJECT_DECLARATION)
        .forType(COLON)
        .set(Indent.getNormalIndent()),

    strategy("Delegation list")
        .within(SUPER_TYPE_LIST)
        .continuationIf(KotlinCodeStyleSettings::CONTINUATION_INDENT_IN_SUPERTYPE_LISTS, indentFirst = true),

    strategy("Indices")
        .within(INDICES)
        .notForType(RBRACKET)
        .set(Indent.getContinuationIndent(false)),

    strategy("Binary expressions")
        .within(BINARY_EXPRESSIONS)
        .forElement { node ->
            !node.suppressBinaryExpressionIndent()
        }
        .set(Indent.getContinuationWithoutFirstIndent(false)),

    strategy("Parenthesized expression")
        .within(PARENTHESIZED)
        .set(Indent.getContinuationWithoutFirstIndent(false)),

    strategy("Opening parenthesis for conditions")
        .forType(LPAR)
        .within(IF, WHEN_ENTRY, WHILE, DO_WHILE)
        .set(Indent.getContinuationWithoutFirstIndent(true)),

    strategy("Closing parenthesis for conditions")
        .forType(RPAR)
        .forElement { node -> !hasErrorElementBefore(node) }
        .within(IF, WHEN_ENTRY, WHILE, DO_WHILE)
        .set(Indent.getNoneIndent()),

    strategy("Closing parenthesis for incomplete conditions")
        .forType(RPAR)
        .forElement { node -> hasErrorElementBefore(node) }
        .within(IF, WHEN_ENTRY, WHILE, DO_WHILE)
        .set(Indent.getContinuationWithoutFirstIndent()),

    strategy("KDoc comment indent")
        .within(KDOC_CONTENT)
        .forType(KDocTokens.LEADING_ASTERISK, KDocTokens.END)
        .set(Indent.getSpaceIndent(KDOC_COMMENT_INDENT)),

    strategy("Block in when entry")
        .within(WHEN_ENTRY)
        .notForType(
            BLOCK,
            WHEN_CONDITION_EXPRESSION,
            WHEN_CONDITION_IN_RANGE,
            WHEN_CONDITION_IS_PATTERN,
            ELSE_KEYWORD,
            ARROW,
        )
        .set(Indent.getNormalIndent()),

    strategy("Parameter list")
        .within(VALUE_PARAMETER_LIST)
        .forElement { it.elementType == VALUE_PARAMETER && it.psi.prevSibling != null }
        .continuationIf(KotlinCodeStyleSettings::CONTINUATION_INDENT_IN_PARAMETER_LISTS, indentFirst = true),

    strategy("Where clause")
        .within(CLASS, FUN, PROPERTY)
        .forType(WHERE_KEYWORD)
        .set(Indent.getContinuationIndent()),

    strategy("Array literals")
        .within(COLLECTION_LITERAL_EXPRESSION)
        .notForType(LBRACKET, RBRACKET)
        .set(Indent.getNormalIndent()),

    strategy("Type aliases")
        .within(TYPEALIAS)
        .notForType(
            TYPE_ALIAS_KEYWORD, EOL_COMMENT, MODIFIER_LIST, BLOCK_COMMENT,
            DOC_COMMENT,
        )
        .set(Indent.getContinuationIndent()),

    strategy("Default parameter values")
        .within(VALUE_PARAMETER)
        .forElement { node -> node.psi != null && node.psi == (node.psi.parent as? KtParameter)?.defaultValue }
        .continuationIf(KotlinCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES, indentFirst = true),
)


private fun getOperationType(node: ASTNode): IElementType? =
    node.findChildByType(OPERATION_REFERENCE)?.firstChildNode?.elementType

fun hasErrorElementBefore(node: ASTNode): Boolean {
    val prevSibling = getPrevWithoutWhitespace(node)
        ?: return false

    if (prevSibling.elementType == TokenType.ERROR_ELEMENT)
        return true

    val lastChild = TreeUtil.getLastChild(prevSibling)
    return lastChild?.elementType == TokenType.ERROR_ELEMENT
}

/**
 * Suppress indent for binary expressions when there is a block higher in the tree that forces
 * its indent to children ('if' condition or elvis).
 */
private fun ASTNode.suppressBinaryExpressionIndent(): Boolean {
    var psi = psi.parent as? KtBinaryExpression ?: return false
    while (psi.parent is KtBinaryExpression) {
        psi = psi.parent as KtBinaryExpression
    }
    return psi.parent?.node?.elementType == CONDITION || psi.operationToken == ELVIS
}

// WITHOUT_CUSTOM_LINE_INDENT_PROVIDER