fun main(args: Array<String>) {
    val buffer: List<PsiElement> = getSelectedElements(code.getFile(), code.getStartOffsets(), code.getEndOffsets())

    val project: Project = file.getProject()
    val converter = Converter(project, J2kPackage.getPluginSettings())
    val result = StringBuilder()
    for (e in buffer) {
        result.append(converter.elementToKotlin(e))
    }

    return StringUtil.convertLineSeparators(result.toString())
}