//package com.androidpv.java.codeParser;
//
///**
// * Created by bradley on 2/12/2016.
// */
//
//import java.io.*;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.*;
//
//import com.androidpv.java.gui.DataSubmit;
//import com.androidpv.java.gui.PVView;
//import com.androidpv.java.xposed.APKBuilder;
//import com.androidpv.java.xposed.MBConstants;
//import com.androidpv.java.xposed.ModuleBuilder;
//import org.eclipse.jdt.core.JavaCore;
//import org.eclipse.jdt.core.dom.*;
//import org.jetbrains.annotations.Nullable;
//
//import javax.swing.*;
//
//public class Parser {
//
//    boolean sourcePathFound = false;
//
//    //use ASTParse to parse string
//    public static void parse(String str, String outputFile, File sourceFile, String jarFilesLoc, String adbLoc, String sdkLoc) {
//
//        ASTParser parser = ASTParser.newParser(AST.JLS8);
//        String sourcePath = getSourcePath(sourceFile);
//
//        if (sourcePath.equals("!*!*!*!*!*!*!*!*!")) {
//            return;
//        }
//
//        //PVView instance = PVView.getInstance();
//        //instance.updateOutLog("BRAD DID IT\n");
//
//        String[] classpath;
//
//        if (jarFilesLoc != null) {
//
//            File jarFolder = new File(jarFilesLoc);
//
//
//            File[] jarFiles = jarFolder.listFiles();
//
//            classpath = new String[jarFiles.length];
//
//            for (int jarIter = 0; jarIter < jarFiles.length; jarIter++) {
//                classpath[jarIter] = jarFiles[jarIter].getPath();
//            }
//        } else {
//            classpath = new String[]{""};
//        }
//
//        Map options = JavaCore.getOptions();
//        JavaCore.setComplianceOptions(JavaCore.VERSION_1_7, options);
//        parser.setCompilerOptions(options);
//
//        parser.setUnitName(sourceFile.getName());
//        parser.setEnvironment(classpath, new String[]{sourcePath}, new String[]{"UTF-8"}, true);
//        parser.setSource(str.toCharArray());
//
//        parser.setResolveBindings(true);
//        parser.setBindingsRecovery(true);
//        parser.setStatementsRecovery(true);
//
//        parser.setKind(ASTParser.K_COMPILATION_UNIT);
//
//        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
//
//        cu.accept(new ASTVisitor() {
//            Set names = new HashSet();
//
//            public boolean visit(TypeDeclaration typeDeclarationStatement) {
//
//                if (!typeDeclarationStatement.isPackageMemberTypeDeclaration()) {
//                    System.out.println(typeDeclarationStatement.getName());
//                    // Get more details from the type declaration.
//                }
//
//                return true;
//            }
//
//            public boolean visit(AnonymousClassDeclaration node) {
//
//                System.out.println("found anonymous class");
//
//                return true;
//            }
//
//            public boolean visit(MethodDeclaration node) {
//
//                boolean isInterface = false;
//                boolean classFound = false;
//
//                SimpleName name = node.getName();
//                List classes = cu.types();
//                String mainClassName = "";
//                List<String> parentModifiers = new ArrayList<>();
//
//                if (node.resolveBinding().getDeclaringClass().isEnum()) {
//                    if (!node.resolveBinding().getDeclaringClass().isNested()) {
//                        EnumDeclaration enumDec = (EnumDeclaration) classes.get(0);
//                        mainClassName = enumDec.getName().toString();
//                        try {
//                            parentModifiers = ((EnumDeclaration) node.getParent()).modifiers();
//                        } catch (Exception e) {
//
//                        }
//                        classFound = true;
//                    }
//                }
//                if (!classFound) {
//                    TypeDeclaration typeDec = (TypeDeclaration) classes.get(0);
//                    mainClassName = typeDec.getName().toString();
//                    try {
//                        parentModifiers = ((TypeDeclaration) node.getParent()).modifiers();
//                    } catch (Exception e) {
//
//                    }
//                    try {
//                        isInterface = ((TypeDeclaration) node.getParent()).isInterface();
//                    } catch (Throwable e) {
//
//                    }
//                }
//
//                List<List<String>> parentsAnonClassList = getParents(node, mainClassName);
//                List<String> parentList = parentsAnonClassList.get(0);
//                List<String> anonClassList = parentsAnonClassList.get(1);
//
//                int paramLength = node.resolveBinding().getParameterTypes().length;
//                String[] parameters = new String[paramLength];
//                for (int paramIndex = 0; paramIndex < paramLength; paramIndex++) {
//                    if (MBConstants.PRIMITIVES_LIST.contains(
//                            node.resolveBinding().getParameterTypes()[paramIndex].getName())) {
//                        parameters[paramIndex] = node.resolveBinding().getParameterTypes()[paramIndex].getName();
//                    } else {
//                        if (!node.resolveBinding().getParameterTypes()[paramIndex].getBinaryName().equals(
//                                node.resolveBinding().getParameterTypes()[paramIndex].getName())) {
//                            parameters[paramIndex] = node.resolveBinding().getParameterTypes()[paramIndex].getBinaryName();
//                        } else {
//
//                            System.err.println("Missing jar - unable to resolve parameter type bindings for parameter " +
//                                    node.resolveBinding().getParameterTypes()[paramIndex].getBinaryName() +
//                                    " in method " + name.toString() + "() in class " + mainClassName + ".\n\tMethod " +
//                                    name.toString() + "() will not be analyzed by module.");
//                            PVView.getInstance().updateOutLog("\nMissing jar for parameter " +
//                                    node.resolveBinding().getParameterTypes()[paramIndex].getBinaryName() +
//                                    " in method " + name.toString() + " in class " + mainClassName + ".\n\tMethod " +
//                                    name.toString() + " will not be analyzed by module.\n");
//
//                            return false;
//                        }
//                    }
//                }
//
//                printtoFile(outputFile, (cu.getPackage() != null ? cu.getPackage().getName().toString() : "Null") +
//                        ";" + mainClassName + ";" + parentList + ";" + anonClassList + ";" +
//                        name.toString() + ";" + Arrays.toString(parameters) + ";" + node.modifiers() + ";" +
//                        parentModifiers + ";" + node.isConstructor() + ";" + isInterface);
//
//                this.names.add(name.getIdentifier());
////                return true;
//                return false; // do not continue
//            }
//        });
//
//    }
//
//    /**
//     * This method returns the chain of parents as a list.
//     *
//     * @param originalNode
//     * @return
//     */
//    private static List<List<String>> getParents(MethodDeclaration originalNode, String mainClassName) {
//
//        ASTNode astNode = originalNode;
//
//        List<String> parentsList = new ArrayList<>();
//        List<String> anonClassList = new ArrayList<>();
//
//        boolean lastParentFound = false;
//
//        while (!lastParentFound) {
//            ASTNode node = astNode.getParent();
//            Class parentClass = node.getClass();
//
//            if (parentClass.getName().equals("org.eclipse.jdt.core.dom.TypeDeclaration")) {
//                parentsList.add(((TypeDeclaration) node).getName().toString());
//                if (((TypeDeclaration) node).getName().toString().equals(mainClassName)) {
//                    lastParentFound = true;
//                }
//            }
//            else if (parentClass.getName().equals("org.eclipse.jdt.core.dom.ClassInstanceCreation")) {
//                parentsList.add(((ClassInstanceCreation) node).getType().toString());
//                anonClassList.add(((ClassInstanceCreation) node).getType().toString());
//            }
//            else if (parentClass.getName().equals("org.eclipse.jdt.core.dom.EnumDeclaration")) {
//                parentsList.add(((EnumDeclaration) node).getName().toString());
//                if (((EnumDeclaration) node).getName().toString().equals(mainClassName)) {
//                    lastParentFound = true;
//                }
//            }
//            astNode = node;
//        }
//
//        List<List<String>> parentsAnonClassList = new ArrayList<>();
//        parentsAnonClassList.add(parentsList);
//        parentsAnonClassList.add(anonClassList);
//
//        return parentsAnonClassList;
//    }
//
//    //read file content into a string
//    public static String readFileToString(String filePath) throws IOException {
//        StringBuilder fileData = new StringBuilder(1000);
//        BufferedReader reader = new BufferedReader(new FileReader(filePath));
//
//        char[] buf = new char[10];
//        int numRead;
//        while ((numRead = reader.read(buf)) != -1) {
//            String readData = String.valueOf(buf, 0, numRead);
//            fileData.append(readData);
//            buf = new char[1024];
//        }
//        reader.close();
//        return fileData.toString();
//    }
//
//    //loop directory to get file list
//    public static void parseFilesInDir(List<File> files, String outputFile, String jarFilesLoc, String adbLoc, String sdkLoc, String uName, String adbDir) {
//        SwingWorker worker = new SwingWorker() {
//            @Override
//            protected Object doInBackground() throws Exception {
//
//                String filePath;
//                for (File f : files) {
//                    filePath = f.getAbsolutePath();
//                    if (f.isFile()) {
//           //             System.out.println("FILE BEING PARSED" + f);
//                        try {
//        //                    parse(f, outputFile, inputFile);
//
//                            parse(readFileToString(filePath), outputFile, f, jarFilesLoc, adbLoc, sdkLoc);
//                        } catch (Exception e) {
//                            System.err.println("Error in\t" + f.getPath());
//                            System.err.println("Error parse(readFileToString) in ParseFilesInDir: " + e.getMessage());
//                            e.printStackTrace();
//                        }
//                    }
//                }
//                System.out.println("Done Parsing Files!");
//                PVView.getInstance().updateOutLog("Done Parsing Files!\n"); //Let's see if it Breaks
//                PVView.getInstance().updateOutLog("");
//                return null;
//            }
//
//            @Override
//            protected void done(){
//                String outputPathString = new File("").getAbsoluteFile().toString() + "/parseData.txt";
//                PVView.getInstance().updateOutLog("Building module...\n");
//                new ModuleBuilder(outputPathString);
//                PVView.getInstance().updateOutLog("Building APK...\n");
//                new APKBuilder(adbLoc, sdkLoc);
//
//
//                int reply = JOptionPane.showConfirmDialog(null, "Your APK is ready, would you like to switch views ", "Submit View", JOptionPane.OK_OPTION);
//                if (reply == JOptionPane.OK_OPTION) {
//                    new DataSubmit(uName, adbDir);
//                    PVView.getInstance().setVisible(false);
//                }
//                else {
//                    JOptionPane.showMessageDialog(null, "GOODBYE");
//                }
//            }
//        };
//        worker.execute();
//    }
//
//    //Gets files in a given directory
//    @Nullable
//    public static List getFiles(String input) {
//        Path fp = Paths.get(input);
//        PrintFiles pf = new PrintFiles();
//        try {
//            Files.walkFileTree(fp, pf);
//        } catch (Exception e) {
//            System.err.println("Error walkFileTree in getFiles: " + e.getMessage());
//            e.printStackTrace();
//        }
//        if (pf.getFileL().size() == 0) {  // either empty directory or directory doesn't exist
//            return null;
//        }
//        else {
//            return pf.getFileL();
//        }
//    }
//
//    public static void printtoFile(String outputFile, String s) {
//        // Creates output file in the src directory
//        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, true)))) {
//            out.print("");
//            out.println(s);
//        } catch (IOException e) {
//            //exception handling left as an exercise for the reader
//            e.printStackTrace();
//        }
//    }
//
//    private static String getSourcePath(File file) {
//
//        String sourcePath = tryPath("/", file);
//
//        if (sourcePath.equals("!*!*!*!*!*!*!*!*!")) {
//            sourcePath = tryPath("\\", file);
//        }
//
//        return sourcePath;
//    }
//
//
//    private static String tryPath(String slash, File file) {
//        String fullPath = file.getPath();
//
//        String mainPath = "src" + slash + "main" +slash + "java";
//
//        int srcIndex = fullPath.indexOf(mainPath);
//
//        if (srcIndex == -1) {
//            // RETURN TO GUI AND ASK FOR SOURCE PATH
////            String alarmClockPath = "android";
//            String alarmClockPath = "main" + slash + "java" + slash + "me" + slash + "kuehle" + slash + "carreport";
//
//            srcIndex = fullPath.indexOf(alarmClockPath);
//            if (srcIndex == -1) {
//                return "!*!*!*!*!*!*!*!*!";
//            }
//
//            String path = fullPath.substring(0, srcIndex + alarmClockPath.length());
//
//            return path;
//        }
//        String path = fullPath.substring(0, srcIndex + mainPath.length());
//
//        return path;
//    }
//}