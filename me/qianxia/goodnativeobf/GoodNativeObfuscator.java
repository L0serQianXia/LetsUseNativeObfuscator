package me.qianxia.goodnativeobf;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.tree.*;

import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * @Author: QianXia
 * @Description:
 * @Date: 2021/01/04-20:34
 */
public class GoodNativeObfuscator {
    private static String inputName;
    private static String all32CppFiles = "";
    private static String all64CppFiles = "";
    private static int count;

    public static void main(String[] args) {
        for(int i = 0; i < args.length; ++i) {
            if ("--input".equalsIgnoreCase(args[i])) {
                inputName = args[i + 1];
            }
        }

        if (inputName == null) {
            inputName = System.getProperty("user.dir");
        }

        compileCpp();
        System.out.println("packing the JAR with DLL");
        packJar();

        System.out.println("Finished.");
    }

    public static void compileCpp(){
        File dir = new File(inputName);
        File[] files = dir.listFiles();

        File x32Folder = new File(dir, "x32");
        File x64Folder = new File(dir, "x64");

        x32Folder.mkdir();
        x64Folder.mkdir();

        for (File file : files) {
            if (!file.getName().endsWith(".cpp")) {
                continue;
            }

            try {
                Process process = Runtime.getRuntime().exec("g++ -m32 -c \"" + file.getName() + "\" -o x32/" + file.getName().replace(".cpp", ".o"));
                process.waitFor();
                all32CppFiles = all32CppFiles + "x32/" + file.getName().replace(".cpp", ".o ");
            } catch (Exception var10) {
                var10.printStackTrace();
            }
        }

        for (File file : files) {
            if (!file.getName().endsWith(".cpp")) {
                continue;
            }

            try {
                Process process = Runtime.getRuntime().exec("g++ -m64 -c \"" + file.getName() + "\" -o x64/" + file.getName().replace(".cpp", ".o"));
                process.waitFor();
                all64CppFiles = all64CppFiles + "x64/" + file.getName().replace(".cpp", ".o ");
            } catch (Exception var10) {
                var10.printStackTrace();
            }
        }

        dir = new File(dir.getAbsolutePath() + "\\output\\");
        files = dir.listFiles();

        for (File file : files) {
            if (file.getName().endsWith(".cpp") && !all32CppFiles.contains(file.getName())) {
                try {
                    System.out.println(inputName + "\\output\\" + file.getName());
                    System.out.println("x32/" + file.getName().replace(".cpp", ".o"));

                    Process process = Runtime.getRuntime().exec("g++ -m32 -c \"" + inputName + "\\output\\" + file.getName() + "\" -o x32/" + file.getName().replace(".cpp", ".o"));
                    process.waitFor();
                    System.out.println(++count);
                    all32CppFiles = all32CppFiles + "x32/" + file.getName().replace(".cpp", ".o ");
                } catch (Exception var9) {
                    var9.printStackTrace();
                }
            }
        }

        for (File file : files) {
            if (file.getName().endsWith(".cpp") && !all64CppFiles.contains(file.getName())) {
                try {
                    Process process = Runtime.getRuntime().exec("g++ -m64 -c \"" + inputName + "\\output\\" + file.getName() + "\" -o x64/" + file.getName().replace(".cpp", ".o"));
                    process.waitFor();
                    System.out.println(++count);
                    all64CppFiles = all64CppFiles + "x64/" + file.getName().replace(".cpp", ".o ");
                } catch (Exception var9) {
                    var9.printStackTrace();
                }
            }
        }

        System.out.println(all32CppFiles);
        System.out.println(all64CppFiles);

        try {
            Process process = Runtime.getRuntime().exec("g++ -m64 -shared " + all64CppFiles + " -static-libstdc++ -static-libgcc -o x64/My64DLL.dll");
            process.waitFor();
        } catch (Exception var8) {
            var8.printStackTrace();
        }
        try {
            Process process = Runtime.getRuntime().exec("g++ -m32 -shared " + all32CppFiles + " -static-libstdc++ -static-libgcc -o x32/My32DLL.dll");
            process.waitFor();
        } catch (Exception var8) {
            var8.printStackTrace();
        }
    }

    public static void packJar() {
        String dll32Path, dll64Path, jarFilePath = null;
        File jarFile, dll32File, dll64File;

        String here = System.getProperty("user.dir");
        dll32Path = here + "\\x32\\My32DLL.dll";
        dll64Path = here + "\\x64\\My64DLL.dll";

        File dir = new File(here).getParentFile();
        File[] files = dir.listFiles();

        for (File file : files) {
            if (file.getName().endsWith(".jar")) {
                jarFilePath = file.getAbsolutePath();
            }
        }

        jarFile = new File(jarFilePath);
        dll32File = new File(dll32Path);
        dll64File = new File(dll64Path);

        if (!jarFile.exists()) {
            System.out.println("Jar File Not Found!!!");
            return;
        }

        if (!dll32File.exists()) {
            System.out.println("32bit DLL Not Found!!!");
            return;
        }

        if (!dll64File.exists()) {
            System.out.println("64bit DLL Not Found!!!");
            return;
        }

        String mainClazz = "net/minecraft/client/main/Main";

        Map<ZipEntry, byte[]> zipEntryMap = new HashMap<>();
        Map<String, ClassNode> classes = new HashMap<>();

        try {
            ZipFile jar = new ZipFile(jarFile);
            Enumeration<? extends ZipEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if ("META-INF/MANIFEST.MF".equals(entry.getName())) {
                    byte[] buffer = new byte[1024];
                    InputStream inputStream = jar.getInputStream(entry);
                    StringBuilder sb = new StringBuilder();
                    while (inputStream.read(buffer) != -1) {
                        sb.append(new String(buffer));
                    }
                    String fileContext = sb.toString();
                    mainClazz = fileContext.substring(fileContext.indexOf("Main-Class: ") + 12);
                    mainClazz = mainClazz.replaceAll("\r\n", "");
                }
            }

            entries = jar.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (mainClazz.contains(entry.getName().replaceAll(".class", ""))) {
                    ClassReader cr = new ClassReader(jar.getInputStream(entry));
                    ClassNode cn = new ClassNode();
                    cr.accept(cn, ClassReader.SKIP_FRAMES);
                    classes.put(cn.name, cn);
                    continue;
                }
                zipEntryMap.put(entry, toByteArray(jar.getInputStream(entry)));
            }

            InputStream inputStream = GoodNativeObfuscator.class.getResourceAsStream("/OhDear.class");
            InputStream input32Dll = new FileInputStream(dll32File);
            InputStream input64Dll = new FileInputStream(dll64File);

            zipEntryMap.put(new ZipEntry("OhDear.class"), toByteArray(inputStream));
            zipEntryMap.put(new ZipEntry("META-INF/OhDear"), toByteArray(input32Dll));
            zipEntryMap.put(new ZipEntry("META-INF/OhDear64"), toByteArray(input64Dll));


            AtomicBoolean found = new AtomicBoolean(false);
            String finalMainClazz = mainClazz;
            classes.forEach((name, classNode) -> {
                if (finalMainClazz.contains(name)) {
                    classNode.methods.forEach(method -> {
                        if ("main".equals(method.name)) {
                            for (AbstractInsnNode node : method.instructions.toArray()) {
                                if (node.getOpcode() == Opcodes.INVOKESTATIC) {
                                    MethodInsnNode methodInsnNode = (MethodInsnNode) node;
                                    boolean flag = "loadLibrary".equals(methodInsnNode.name)
                                            && "java/lang/System".equals(methodInsnNode.owner)
                                            && methodInsnNode.getPrevious().getOpcode() == Opcodes.LDC
                                            && ((String)((LdcInsnNode)methodInsnNode.getPrevious()).cst).contains("native_jvm_classes");

                                    if (flag) {
                                        method.instructions.insertBefore(node.getPrevious(), new MethodInsnNode(Opcodes.INVOKESTATIC, "OhDear", "OMG", "()V"));
                                        method.instructions.remove(node.getPrevious());
                                        method.instructions.remove(node);
                                        found.set(true);
                                        break;
                                    }
                                }
                            }

                            for (AbstractInsnNode node : method.instructions.toArray()) {
                                if (found.get()) {
                                    break;
                                }
                                if (node instanceof LabelNode) {
                                    method.instructions.insertBefore(node.getNext(), new MethodInsnNode(Opcodes.INVOKESTATIC, "OhDear", "OMG", "()V"));
                                    found.set(true);
                                    break;
                                }
                            }
                        }
                    });
                }
            });

            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(jarFile));
            zipEntryMap.forEach((entry, b) -> {
                try {
                    out.putNextEntry(entry);
                    out.write(b);
                    out.closeEntry();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            classes.values().forEach(classNode -> {
                try {
                    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                    try {
                        classNode.accept(cw);
                    } catch (NegativeArraySizeException | ArrayIndexOutOfBoundsException e) {
                        System.out.println(classNode.name + "Failed to compute frames");
                        cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                        classNode.accept(cw);
                    }
                    byte[] b = cw.toByteArray();
                    ZipEntry entry = new ZipEntry(classNode.name + (classNode.name.endsWith(".class") ? "" : ".class"));
                    out.putNextEntry(entry);
                    out.write(b);
                    out.closeEntry();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            });
            jar.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] toByteArray(InputStream in) {
        try {
            ByteArrayOutputStream baros = new ByteArrayOutputStream();
            final byte[] buffer = new byte[1024];
            while (in.available() > 0) {
                int data = in.read(buffer);
                baros.write(buffer, 0, data);
            }
            return baros.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void copyFile(String fromFile, String toFile){
        try (FileInputStream fis = new FileInputStream(fromFile);
             FileOutputStream fos = new FileOutputStream(toFile)) {
            int len;
            byte[] buffer = new byte[4096];
            while ((len = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void copyDir(String fromDir,String toDir){
        File dirSource = new File(fromDir);
        File dirTo = new File(toDir);

        if (!dirSource.isDirectory()) {
            return;
        }
        if(!dirTo.exists()){
            dirTo.mkdir();
        }


        File[] files = dirSource.listFiles();

        for (File file : files) {
            String strFrom = fromDir + File.separator + file.getName();
            String strTo = toDir + File.separator + file.getName();

            if (file.isDirectory()) {
                copyDir(strFrom,strTo);
            }
            if (file.isFile()) {
                copyFile(strFrom,strTo);
            }
        }
    }
}
