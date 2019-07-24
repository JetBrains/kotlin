class A {
    fun someOther() = false

    private fun formatElement(element: PsiElement): String? {
        var element: PsiElement = element
        element = JetPsiUtil.ascendIfPropertyAccessor(element)
        if (element is JetNamedFunction || element is JetProperty) {
            val bindingContext: BindingContext = AnalyzerFacadeWithCache.analyzeFileWithCache(element.getContainingJetFile()).getBindingContext()
            val declarationDescriptor: DeclarationDescriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element)
            if (declarationDescriptor is CallableMemberDescriptor) {
                val containingDescriptor: DeclarationDescriptor = declarationDescriptor.getContainingDeclaration()
                if (containingDescriptor is ClassDescriptor) {
                    return JetBundle.message(
                            "x.in.y",
                            DescriptorRenderer.COMPACT.render(declarationDescriptor),
                            IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.render(containingDescriptor)
                    )
                }
            }
        }
        assert(element is PsiMethod
        ) { "Method accepts only kotlin functions/properties and java methods, but '" + element.getText().toString() + "' was found" }
        return JetRefactoringUtil.formatPsiMethod(element as PsiMethod, true, false)
    }

    protected fun getDimensionServiceKey(): String {
        return "#org.jetbrains.kotlin.idea.refactoring.safeDelete.KotlinOverridingDialog"
    }

    fun getSelected(): ArrayList<UsageInfo> {
        val result: ArrayList<UsageInfo> = ArrayList<UsageInfo?>()
        for (i in 0 until myChecked.length) {
            if (myChecked.get(i)) {
                result.add(myOverridingMethods.get(i))
            }
        }
        return result
    }
}