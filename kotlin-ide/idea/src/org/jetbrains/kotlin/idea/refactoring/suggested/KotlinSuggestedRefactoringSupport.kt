package org.jetbrains.kotlin.idea.refactoring.suggested

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport.Parameter
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport.Signature
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

class KotlinSuggestedRefactoringSupport : SuggestedRefactoringSupport {
    override fun isDeclaration(psiElement: PsiElement): Boolean {
        if (psiElement !is KtDeclaration) return false
        if (psiElement is KtParameter && psiElement.ownerFunction != null) return false
        return true
    }
    
    override fun signatureRange(declaration: PsiElement): TextRange? {
        when (declaration) {
            is KtPrimaryConstructor -> return declaration.textRange

            is KtSecondaryConstructor -> return declaration.valueParameterList?.textRange

            is KtCallableDeclaration -> {
                if (isOnlyRenameSupported(declaration)) {
                    return declaration.nameIdentifier?.textRange
                }

                val start = declaration.receiverTypeReference?.textRange?.startOffset
                    ?: declaration.nameIdentifier?.textRange?.startOffset
                    ?: return null
                val end = (declaration.typeReference ?: declaration.valueParameterList ?: declaration.nameIdentifier)
                    ?.textRange?.endOffset
                    ?: return null
                return TextRange(start, end)
            }

            is KtNamedDeclaration -> return declaration.nameIdentifier?.textRange

            else -> return null
        }
    }

    override fun importsRange(psiFile: PsiFile): TextRange? {
        return (psiFile as KtFile).importList?.textRange
    }

    override fun nameRange(declaration: PsiElement): TextRange? {
        val identifier = when (declaration) {
            is KtPrimaryConstructor -> declaration.containingClassOrObject?.nameIdentifier
            is KtSecondaryConstructor -> declaration.getConstructorKeyword()
            is KtNamedDeclaration -> declaration.nameIdentifier
            else -> null
        }
        return identifier?.textRange
    }

    override fun hasSyntaxError(declaration: PsiElement): Boolean {
        if (super.hasSyntaxError(declaration)) return true

        // do not suggest renaming of local variable which has neither type nor initializer
        // it's important because such variable declarations may appear on typing "val name = " before an expression
        if (declaration is KtProperty && declaration.isLocal) {
            if (declaration.typeReference == null && declaration.initializer == null) return true
        }

        return false
    }

    override fun isIdentifierStart(c: Char) = c.isJavaIdentifierStart()
    override fun isIdentifierPart(c: Char) = c.isJavaIdentifierPart()

    override val stateChanges = KotlinSuggestedRefactoringStateChanges(this)
    override val availability = KotlinSuggestedRefactoringAvailability(this)
    override val ui get() = KotlinSuggestedRefactoringUI
    override val execution = KotlinSuggestedRefactoringExecution(this)

    companion object {
        fun isOnlyRenameSupported(declaration: KtCallableDeclaration): Boolean {
            // for local variable - only rename
            return declaration is KtVariableDeclaration && KtPsiUtil.isLocal(declaration)
        }
    }
}

enum class DeclarationType {
    FUN {
        override val prefixKeyword get() = "fun"
        override val isFunction get() = true
    },
    VAL {
        override val prefixKeyword get() = "val"
        override val isFunction get() = false
    },
    VAR {
        override val prefixKeyword get() = "var"
        override val isFunction get() = false
    },
    PRIMARY_CONSTRUCTOR {
        override val prefixKeyword: String?
            get() = null
        override val isFunction get() = true
    },
    SECONDARY_CONSTRUCTOR {
        override val prefixKeyword: String?
            get() = null
        override val isFunction get() = true
    },
    OTHER {
        override val prefixKeyword: String?
            get() = null
        override val isFunction get() = false
    }

    ;

    abstract val prefixKeyword: String?
    abstract val isFunction: Boolean

    companion object {
        fun fromDeclaration(declaration: KtDeclaration): DeclarationType = when (declaration) {
            is KtPrimaryConstructor -> PRIMARY_CONSTRUCTOR

            is KtSecondaryConstructor -> SECONDARY_CONSTRUCTOR

            is KtNamedFunction -> FUN

            is KtProperty -> if (declaration.isVar) VAR else VAL

            else -> OTHER
        }
    }
}

internal data class KotlinSignatureAdditionalData(
    val declarationType: DeclarationType,
    val receiverType: String?
) : SuggestedRefactoringSupport.SignatureAdditionalData

internal data class KotlinParameterAdditionalData(
    val defaultValue: String?,
    val modifiers: String
) : SuggestedRefactoringSupport.ParameterAdditionalData

internal val Signature.receiverType: String?
    get() = (additionalData as KotlinSignatureAdditionalData?)?.receiverType

internal val Parameter.defaultValue: String?
    get() = (additionalData as KotlinParameterAdditionalData?)?.defaultValue

internal val Parameter.modifiers: String
    get() = (additionalData as KotlinParameterAdditionalData?)?.modifiers ?: ""
