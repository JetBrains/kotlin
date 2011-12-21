package org.jetbrains.jet.shift;

///**
// * @author abreslav
// */
//public class Shifter extends LightDaemonAnalyzerTestCase {
//    private String name;
//    public static final Pattern FILE_PATTERN = Pattern.compile("//\\s*FILE:\\s*(.*)$", Pattern.MULTILINE);
//
//    public Shifter(@NonNls String dataPath, String name) {
//        super(dataPath);
//        this.name = name;
//    }
//
//    @Override
//    public String getName() {
//        return "test" + name;
//    }
//
//
////    private class TestFile {
////        private final List<CheckerTestUtil.DiagnosedRange> diagnosedRanges = Lists.newArrayList();
////        private final String expectedText;
////        private final String clearText;
////        private final JetFile jetFile;
////
////        public TestFile(String fileName, String textWithMarkers) {
////            expectedText = textWithMarkers;
////            clearText = CheckerTestUtil.parseDiagnosedRanges(expectedText, diagnosedRanges);
////            jetFile = createCheckAndReturnPsiFile(fileName, clearText);
////        }
////
////        public void getActualText(BindingContext bindingContext, StringBuilder actualText) {
////            CheckerTestUtil.diagnosticsDiff(diagnosedRanges, CheckerTestUtil.getDiagnosticsIncludingSyntaxErrors(bindingContext, jetFile), new CheckerTestUtil.DiagnosticDiffCallbacks() {
////                @Override
////                public void missingDiagnostic(String type, int expectedStart, int expectedEnd) {
////                    String message = "Missing " + type + DiagnosticUtils.atLocation(jetFile, new TextRange(expectedStart, expectedEnd));
////                    System.err.println(message);
////                }
////
////                @Override
////                public void unexpectedDiagnostic(String type, int actualStart, int actualEnd) {
////                    String message = "Unexpected " + type + DiagnosticUtils.atLocation(jetFile, new TextRange(actualStart, actualEnd));
////                    System.err.println(message);
////                }
////            });
////
////            actualText.append(CheckerTestUtil.addDiagnosticMarkersToText(jetFile, bindingContext, AnalyzingUtils.getSyntaxErrorRanges(jetFile)));
////        }
////
////    }
//
//    @Override
//    public void runTest() throws Exception {
//        String testFileName = name + ".jet";
//
//        String expectedText = loadFile(testFileName);
//
//        final JetFile file = createCheckAndReturnPsiFile(testFileName, expectedText);
//
//
////        List<TestFile> testFileFiles = createTestFiles(testFileName, expectedText);
////
////        boolean importJdk = expectedText.contains("+JDK");
////        Configuration configuration = importJdk ? JavaBridgeConfiguration.createJavaBridgeConfiguration(getProject()) : Configuration.EMPTY;
////
////        List<JetDeclaration> namespaces = Lists.newArrayList();
////        for (TestFile testFileFile : testFileFiles) {
////            namespaces.add(testFileFile.jetFile.getRootNamespace());
////        }
////
////        BindingContext bindingContext;
////        if (importJdk) {
////            bindingContext = AnalyzerFacade.analyzeNamespacesWithJavaIntegration(getProject(), namespaces, Predicates.<PsiFile>alwaysTrue(), JetControlFlowDataTraceFactory.EMPTY);
////        }
////        else {
////            bindingContext = AnalyzingUtils.analyzeNamespaces(getProject(), Configuration.EMPTY, namespaces, Predicates.<PsiFile>alwaysTrue(), JetControlFlowDataTraceFactory.EMPTY, JetSemanticServices.createSemanticServices(getProject()));
////        }
////
////        StringBuilder actualText = new StringBuilder();
////        for (TestFile testFileFile : testFileFiles) {
////            testFileFile.getActualText(bindingContext, actualText);
////        }
////
////        Assert.assertEquals(expectedText, actualText.toString());
////        final JetFile file = JetPsiFactory.createFile(environment.getProject(), "namespace foo\nclass A");
//        ((MockApplication) ApplicationManager.getApplication()).registerService(PomModel.class, new PomModelImpl(getProject()));
//        final ASTNode packageKeyword = JetPsiFactory.createFile(getProject(), "package foo").getRootNamespace().getHeader().getNode().findChildByType(NAMESPACE_KEYWORD);
//
////        SwingUtilities.invokeAndWait(new Runnable() {
////            @Override
////            public void run() {
//                ApplicationManager.getApplication().runWriteAction(new Runnable() {
//                    @Override
//                    public void run() {
//                        convert(file, packageKeyword);
//                    }
//                });
////            }
////        });
//
//    }
//
//    private static void convert(JetFile file, final ASTNode packageKeyword) {
//        file.getRootNamespace().accept(new JetTreeVisitor<Void>() {
//            @Override
//            public Void visitNamespace(JetNamespace namespace, Void data) {
//                ASTNode namespaceNode = namespace.getHeader().getNode();
//                ASTNode token = namespaceNode.findChildByType(NAMESPACE_KEYWORD);
//                namespaceNode.replaceChild(token, packageKeyword);
//                return super.visitNamespace(namespace, data);
//            }
//        }, null);
//        System.out.println("file = " + file.getText());
//    }
//
//
//    //    private void convert(File src, File dest) throws IOException {
////        File[] files = src.listFiles();
////        for (File file : files) {
////            try {
////                if (file.isDirectory()) {
////                    File destDir = new File(dest, file.getName());
////                    destDir.mkdir();
////                    convert(file, destDir);
////                    continue;
////                }
////                if (!file.getName().endsWith(".jet")) continue;
////                String text = doLoadFile(file.getParentFile().getAbsolutePath(), file.getName());
////                Pattern pattern = Pattern.compile("</?(error|warning)>");
////                String clearText = pattern.matcher(text).replaceAll("");
////                createAndCheckPsiFile(name, clearText);
////
////                BindingContext bindingContext = AnalyzingUtils.getInstance(ImportingStrategy.NONE).analyzeFileWithCache((JetFile) myFile);
////                String expectedText = CheckerTestUtil.addDiagnosticMarkersToText(myFile, bindingContext).toString();
////
////                File destFile = new File(dest, file.getName());
////                FileWriter fileWriter = new FileWriter(destFile);
////                fileWriter.write(expectedText);
////                fileWriter.close();
////            }
////            catch (RuntimeException e) {
////                e.printStackTrace();
////            }
////        }
////    }
//
//    public static Test suite() {
//        return JetTestCaseBuilder.suiteForDirectory(JetTestCaseBuilder.getTestDataPathBase(), "/diagnostics/tests/shift", true, new JetTestCaseBuilder.NamedTestFactory() {
//            @NotNull
//            @Override
//            public Test createTest(@NotNull String dataPath, @NotNull String name, @NotNull File file) {
//                return new Shifter(dataPath, name);
//            }
//        });
//    }
//}
