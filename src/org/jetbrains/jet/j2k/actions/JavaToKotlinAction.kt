//package org.jetbrains.jet.j2k.actions
//
//import com.intellij.openapi.actionSystem.AnAction
//import com.intellij.openapi.actionSystem.AnActionEvent
//import org.jetbrains.jet.j2k.Converter
//import com.intellij.openapi.actionSystem.PlatformDataKeys
//import com.intellij.openapi.actionSystem.LangDataKeys
//import com.intellij.psi.PsiJavaFile
//import com.intellij.openapi.util.io.FileUtil
//import com.intellij.openapi.fileEditor.FileDocumentManager
//import com.intellij.psi.PsiDocumentManager
//import com.intellij.openapi.command.WriteCommandAction
//import com.intellij.openapi.application.ApplicationManager
//import com.intellij.psi.codeStyle.CodeStyleManager
//
//class JavaToKotlinAction(): AnAction() {
//    public override fun actionPerformed(event : AnActionEvent?) {
//        val converter = Converter()
//        val psiFile = event!!.getData(LangDataKeys.PSI_FILE)!!
//        ApplicationManager.getApplication()?.runWriteAction(object : Runnable {
//            public override fun run() {
//                val result = converter.fileToFile(psiFile as PsiJavaFile).toKotlin()
//                val newName = FileUtil.getNameWithoutExtension(psiFile.getName()) + ".kt"
//                val newFile = psiFile.getContainingDirectory()?.createFile(newName)!!
//                val project = psiFile.getProject()
//                val psiDocumentManager = PsiDocumentManager.getInstance(project)!!
//                val document = psiDocumentManager.getDocument(newFile)!!
//                document.setText(result)
//                psiDocumentManager.doPostponedOperationsAndUnblockDocument(document)
//                psiDocumentManager.commitDocument(document)
//                CodeStyleManager.getInstance(project)!!.reformat(newFile)
//                psiFile.setName(psiFile.getName() + ".old")
//                newFile.navigate(true)
//            }
//        })
//    }
//
//
//    public override fun update(e : AnActionEvent?) {
//        val psiFile = e!!.getData(LangDataKeys.PSI_FILE)
//        e.getPresentation().setEnabled(psiFile is PsiJavaFile)
//    }
//}
