package com.androidpv.java.xposed;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

/**
 * Created by Erin on 2/27/16.
 *
 * ModuleBuilder takes in the parsed code generated by Parser and constructs an Xposed module.
 */
public class ModuleBuilder {

    private File sourceFile;
    private File outputFile;

    private boolean DO_NOT_PRINT = false;

    /**
     * Constructor for ModuleBuilder. Creates a text file containing the Xposed module source code.
     *
     * @param fileName  String name of the file containing the parsed code outputted by Parser
     */
    public ModuleBuilder(String fileName) {

        System.out.println("in module builder");

        this.sourceFile = new File(fileName);
        boolean beginningOfFile = true;

        try {
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter("moduleFile.txt")));

            List<List<List<String>>> packagesAndAnonClasses = getPackagesAndAnonClasses(this.sourceFile);
            List<String> packageNamesList = packagesAndAnonClasses.get(0).get(0);
            List<List<String>> anonClassList = packagesAndAnonClasses.get(1);

            writer.println(MBConstants.MODULE_PACKAGE_NAME);
            writer.println(MBConstants.IMPORTS);
            writer.println(MBConstants.CLASS_NAME_MAIN_METHOD);
            writer.println(addMainIfClausePackages(packageNamesList));
            writer.println(MBConstants.PREFERENCES);

            // Header of code done. Now need to write hooks for each method

            BufferedReader reader = new BufferedReader(new FileReader(this.sourceFile));
            String line;

            String packageName = ""; // need to check that package name is different

            while ((line = reader.readLine()) != null) {
                String[] splitString = line.split(MBConstants.PARSED_FILE_SEPARATOR);
                for (int i = 0; i < splitString.length; i++) {
                    splitString[i] = splitString[i].trim();
                }

                if (!splitString[MBConstants.PACKAGE_INDEX].equals(packageName)) {
                    if (!beginningOfFile) {
                        writer.println(MBConstants.END_OF_IF_CLAUSE);
                    }
                    beginningOfFile = false;
                    writer.println(addPackageNameCheck(splitString[MBConstants.PACKAGE_INDEX]));
                    packageName = splitString[MBConstants.PACKAGE_INDEX];
                    System.out.println(packageName);
                }

                String findHook = addFindHook(splitString, anonClassList);

                if (!DO_NOT_PRINT) {

                    writer.println(findHook);
                    writer.println(addHook(splitString[MBConstants.METHOD_INDEX], MBConstants.BEFORE_STRING,
                            MBConstants.METHOD_START_TIME));
                    writer.println(addHook(splitString[MBConstants.METHOD_INDEX], MBConstants.AFTER_STRING,
                            MBConstants.METHOD_END_TIME));
                }

                DO_NOT_PRINT = false;

            }
            writer.println(MBConstants.END_OF_CODE);
            reader.close();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.sourceFile.deleteOnExit();
        System.out.println("Module built.");
    }  // End of Constructor


    /**
     * This method takes in each bit of information from each line of the parsed code outputted by Parser and
     * constructs the findAndHookMethod/Constructor clause.
     *
     * @param methodInfo  each line of the parsed file outputted by Parser split into a String array
     * @return  the findAndHookMethod/Constructor as String
     */
    private String addFindHook(String[] methodInfo, List<List<String>> anonClassList) {

        DO_NOT_PRINT = false;

        StringBuilder hookMethodBuilder = new StringBuilder();

        String packageName = methodInfo[MBConstants.PACKAGE_INDEX];
        String className = methodInfo[MBConstants.CLASS_INDEX];
        String parent = methodInfo[MBConstants.PARENT_INDEX];
        boolean anonClassBoolean = Boolean.parseBoolean(methodInfo[MBConstants.ANON_CLASS_INDEX]);
        String imports = methodInfo[MBConstants.IMPORT_INDEX];
        String methodName = methodInfo[MBConstants.METHOD_INDEX];
        String parameters = methodInfo[MBConstants.PARAMETERS_INDEX];
        String modifiers = methodInfo[MBConstants.MODIFIERS_INDEX];
        boolean isConstructor = Boolean.parseBoolean(methodInfo[MBConstants.CONSTRUCTOR_BOOL_INDEX]);
        boolean nestedClassBoolean = false;
        boolean tryNeededBoolean = false;
        int numOfAnon = 0;

        if (modifiers.contains("abstract")) {
            DO_NOT_PRINT = true;
        }
        if (!modifiers.contains("private")) {
            if (!modifiers.contains("public")) {
                if (!modifiers.contains("protected")) {
                    DO_NOT_PRINT = true;
                }
            }
        }

        String classNameWithoutParent = className;

        // if nested class, must append to classname
        if (!className.equals(parent)) {
            if (anonClassBoolean) {
                int anonIter = 0;
                while ((anonIter < anonClassList.size()) && (!anonClassList.get(anonIter).get(0).equals(className))) {
                    anonIter++;
                }
                if (anonIter == anonClassList.size()) {
                    System.out.println("We didn't find anonymous class for " + className);
                    System.out.println("Using parent");
                    className = className + "$" + parent;
                }
                else {
                    numOfAnon = anonClassList.get(anonIter).size() - 1;
                }
            }
            else {
                nestedClassBoolean = true;
                className = className + "$" + parent;
            }
        }

        if (isConstructor) {
            if (nestedClassBoolean) {
                // replaces methodName with call to super instance
                methodName = packageName + "." + classNameWithoutParent;
            }
            else if (numOfAnon != 0) {
                if (numOfAnon == 1) {
                    className = className + "$" + 1;
                }
                else {
                    tryNeededBoolean = true;
                }
            }
            String findHookConstructor = MBConstants.FIND_HOOK_CONSTRUCTOR_STRING + packageName + "." + className
                    + MBConstants.LPPARAM_CLASS_LOADER_STRING + methodName + "\"";
            hookMethodBuilder.append(findHookConstructor);
        }
        else {
            if (numOfAnon != 0) {
                if (numOfAnon == 1) {
                    className = className + "$" + 1;
                }
                else {
                    tryNeededBoolean = true;
                }
            }
            String findHookMethodPt1 = MBConstants.FIND_HOOK_METHOD_STRING + packageName + "." + className
                    + MBConstants.LPPARAM_CLASS_LOADER_STRING + methodName + "\"";
            hookMethodBuilder.append(findHookMethodPt1);
        }

        if (!parameters.equals("[]")) {
            // we have parameters

            // remove brackets
            parameters = parameters.substring(1, parameters.length()-1);

            // convert parameters into list, splitting on ,
            String[] parameterArray = parameters.split(",");

            for (String parameter : parameterArray) {
                String paramString = ", \"" + parameter.trim() + "\"";
                hookMethodBuilder.append(paramString);
            }
        }
        hookMethodBuilder.append(MBConstants.END_OF_FIND_HOOK_METHOD);

        return hookMethodBuilder.toString();
    }


    /**
     * This method returns the proper subsection of findAndHookMethod, either "beforeHookMethod" or "afterHookMethod".
     *
     * @param method  the method we are analyzing
     * @param timeInstance  specifies whether we are running "beforeHookMethod" or "afterHookMethod"
     * @param methodTime  specifies whether we are capturing methodStart or methodEnd - matches timeInstance
     * @return  the "beforeHookMethod" or "afterHookMethod"
     */
    private String addHook(String method, String timeInstance, String methodTime) {
        String hook = MBConstants.ADD_HOOK_METHOD_BEGINNING + timeInstance
                + MBConstants.ADD_HOOK_METHOD_END + method + methodTime;
        return hook;
    }


    /**
     * This method returns the IF clause for a given package specified in the module. If we pass this IF clause, we
     * start finding and hooking the methods in that package.
     *
     * @param packageName  the package name of the specific IF clause we are writing
     * @return  the IF clause checking if we are in the correct package to execute the correct methods
     */
    private String addPackageNameCheck(String packageName) {
        String ifClause = MBConstants.PACKAGE_NAME_IF_BEGINNING + packageName + MBConstants.PACKAGE_NAME_IF_END;
        return ifClause;
    }


    /** NEEDS EDIT
     * This method generates the String checking that we are working in the correct package. It is not for each
     * individual package but rather prevents the module from proceeding if we are not in a package the module
     * recognizes.
     *
     * @param
     * @return  the main IF clause containing the package names. Returned as a String
     */
    private String addMainIfClausePackages(List<String> packageNamesList) {
        StringBuilder ifClause = new StringBuilder();
        ifClause.append(MBConstants.MAIN_PACKAGE_IF_CLAUSE_BEGINNING + MBConstants.MAIN_LPPARAM_PACKAGENAME_EQUALS);

        int i = 0;
        while (i < packageNamesList.size()) {
            ifClause.append(packageNamesList.get(i));
            i++;
            if (i != packageNamesList.size()) {
                ifClause.append(MBConstants.MAIN_PACKAGE_IF_CLAUSE_OR + MBConstants.MAIN_LPPARAM_PACKAGENAME_EQUALS);
            }
        }
        ifClause.append(MBConstants.MAIN_PACKAGE_IF_CLAUSE_END);

        return ifClause.toString();
    }


    /**  NEEDS EDIT
     * This method does an initial runthrough of the parsed source code outputted by Parser to gather all of the
     * package names. The package names are required for the main IF clause of the module.
     *
     * @param file  the file containing the output of Parser
     * @return  a list of all the packages in the source code
     */
    private List<List<List<String>>> getPackagesAndAnonClasses(File file) {
        List<List<String>> packageNamesList = new ArrayList<>(); // make List<List<String>> in order to return both
                                                                // packageNamesList and anonClassesList

        List<List<String>> anonClassesList = new ArrayList<>();  // [[parent, anonClass1, anonClass2, ...]]

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;

            String packageName = ""; // need to check that package name is different

            while ((line = reader.readLine()) != null) {
                String[] splitString = line.split(MBConstants.PARSED_FILE_SEPARATOR);
                if (!splitString[MBConstants.PACKAGE_INDEX].equals(packageName)) {
                    packageName = splitString[MBConstants.PACKAGE_INDEX];
                    packageNamesList.add(Arrays.asList(packageName));
                }
                if (Boolean.parseBoolean(splitString[MBConstants.ANON_CLASS_INDEX])) {
                    anonClassesList =
                            addAnonClass(splitString[MBConstants.CLASS_INDEX], splitString[MBConstants.PARENT_INDEX],
                                    anonClassesList);
                }
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<List<List<String>>> packagesAndAnonClasses = new ArrayList<>();
        packagesAndAnonClasses.add(packageNamesList);
        packagesAndAnonClasses.add(anonClassesList);

        return packagesAndAnonClasses;
    }


    /** NEEDS EDIT
     * Helper method
     *
     * @param className
     * @param anonClass
     * @param anonClassList
     * @return
     */
    private List<List<String>> addAnonClass(String className, String anonClass, List<List<String>> anonClassList) {
        if (anonClassList.isEmpty()) {
            anonClassList.add(Arrays.asList(className, anonClass));
        }
        else {
            for (int i = 0; i < anonClassList.size(); i++) {
                if (anonClassList.get(i).get(0).equals(className)) {
                    // parent already has an anon class. make sure it's not the same one
                    if (!anonClassList.get(i).contains(anonClass)) {
                        // does not include this anonClass. Add it
                        anonClassList.get(i).add(anonClass);
                    }
                }
                else {
                    // this parent has not been added
                    anonClassList.add(Arrays.asList(className, anonClass));
                }
            }
        }
        return anonClassList;
    }

}
