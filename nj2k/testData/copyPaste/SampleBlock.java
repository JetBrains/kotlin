class A {
    private static String convertCopiedCodeToKotlin(CopiedCode code, PsiFile file) {
        <selection>List<PsiElement> buffer = getSelectedElements(code.getFile(), code.getStartOffsets(), code.getEndOffsets());

        Project project = file.getProject();
        Converter converter = new Converter(project, J2kPackage.getPluginSettings());
        StringBuilder result = new StringBuilder();
        for (PsiElement e : buffer) {
            result.append(converter.elementToKotlin(e));
        }

        return StringUtil.convertLineSeparators(result.toString());</selection>
    }
}