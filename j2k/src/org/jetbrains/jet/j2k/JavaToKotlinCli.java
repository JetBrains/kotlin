//package org.jetbrains.jet.j2k;
//
//import com.intellij.psi.PsiFile;
//import com.intellij.psi.PsiJavaFile;
//import org.apache.commons.cli.*;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.logging.Logger;
//import java.util.regex.Pattern;
//
//import static org.apache.commons.io.FileUtils.readFileToString;
//import static org.apache.commons.io.FileUtils.writeStringToFile;
//
//@SuppressWarnings({"CallToPrintStackTrace", "UseOfSystemOutOrSystemErr"})
//public class JavaToKotlinCli {
//  private static final Logger myLogger = Logger.getAnonymousLogger();
//
//  private JavaToKotlinCli() {
//  }
//
//  public static void main(String[] args) {
//    CommandLineParser parser = new BasicParser();
//    Options options = new Options()
//      .addOption("h", "help", false, "Print usage information")
//      .addOption("f", "from", true, "Directory with Java sources")
//      .addOption("t", "to", true, "Directory with Kotlin sources")
//      .addOption("p", "public-only", false, "Only public and protected members")
//      .addOption("fqn", "fqn", false, "Full qualified names")
//      .addOption("d", "declarations-only", false, "Declarations only")
//      ;
//
//    try {
//      CommandLine commandLine = parser.parse(options, args);
//
//      if (commandLine.hasOption("help"))
//        showHelpAndExit();
//
//      if (commandLine.hasOption("from") && commandLine.hasOption("to")) {
//        String from = commandLine.getOptionValue("from");
//        String to = commandLine.getOptionValue("to");
//
//        for (Option o : commandLine.getOptions()) {
//          Converter.addFlag(o.getLongOpt());
//        }
//
//        if (!from.isEmpty() && !to.isEmpty())
//          convertSourceTree(from, to);
//        else
//          showHelpAndExit();
//      } else
//        showHelpAndExit();
//    } catch (ParseException e) {
//      e.printStackTrace();
//    }
//  }
//
//  @SuppressWarnings("ResultOfMethodCallIgnored")
//  private static void convertSourceTree(String javaPath, String kotlinPath) {
//    try {
//      File javaDir = new File(javaPath);
//      File kotlinDir = new File(kotlinPath);
//
//      if (kotlinDir.exists())
//        kotlinDir.delete();
//
//      if (!kotlinDir.exists() && !kotlinDir.mkdir())
//        myLogger.warning("Creation failed: " + kotlinDir.getAbsolutePath());
//
//      for (File f : getJavaFiles(javaDir.getAbsolutePath())) {
//        String relative = javaDir.toURI().relativize(f.toURI()).getPath().replace(".java", ".kt");
//        File file = new File(kotlinPath, relative);
//
//        if (file.exists())
//          file.delete();
//
//        if (f.isDirectory())
//          if (!file.exists() && !file.mkdir())
//            myLogger.warning("Creation failed: " + file.getAbsolutePath());
//
//        if (f.isFile()) {
//          writeStringToFile(file, fileToKotlin(f));
//        }
//      }
//    } catch (FileNotFoundException e) {
//      e.printStackTrace();
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
//  }
//
//  @NotNull
//  private static String fileToKotlin(File f) throws IOException {
//    final String javaCode = readJavaFileToString(f);
//    return generateKotlinCode(JavaToKotlinTranslator.createFile(JavaToKotlinTranslator.setUpJavaCoreEnvironment(), javaCode));
//  }
//
//  @NotNull
//  private static String generateKotlinCode(@Nullable PsiFile file) {
//    if (file != null && file instanceof PsiJavaFile) {
//      JavaToKotlinTranslator.setClassIdentifiers(file);
//      return JavaToKotlinTranslator.prettify(Converter.fileToFile((PsiJavaFile) file).toKotlin());
//    }
//    return "";
//  }
//
//  @NotNull
//  private static String readJavaFileToString(@NotNull File javaFile) throws IOException {
//    return Pattern.compile("\\s*/\\*.*\\*/", Pattern.DOTALL).matcher(readFileToString(javaFile)).replaceAll("");
//  }
//
//  private static void showHelpAndExit() {
//    System.err.println("Usage: java -jar java2kotlin.jar -f <from> -t <to>");
//    System.exit(1);
//  }
//
//  static public List<File> getJavaFiles(String startDirName) throws FileNotFoundException {
//    return getJavaFiles(new File(startDirName));
//  }
//
//  private static List<File> getJavaFiles(File start) throws FileNotFoundException {
//    List<File> result = new ArrayList<File>();
//
//    if (start.isFile())
//      return Arrays.asList(start);
//
//    for (File file : Arrays.asList(start.listFiles())) {
//      if ((file.isFile() && file.getName().endsWith(".java")) || file.isDirectory())
//        result.add(file);
//
//      if (file.isDirectory()) {
//        List<File> deeperList = getJavaFiles(file);
//        result.addAll(deeperList);
//      }
//    }
//    return result;
//  }
//}