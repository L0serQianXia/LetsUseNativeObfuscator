package me.qianxia.goodnativeobf;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author: QianXia
 * @create: 2022/08/20
 **/
public class Utils {
    public static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buffer = new byte[1024 * 4];
        int temp;
        while ((temp = in.read(buffer)) != -1) {
            out.write(buffer, 0, temp);
        }
        return out.toByteArray();
    }

    public static void copyFileStream(InputStream in, OutputStream out) throws IOException {
        if (in == null || out == null) {
            throw new RuntimeException("InputStream or OutputStream == NULL");
        }
        byte[] buffer = new byte[1024 * 4];
        int temp;
        while ((temp = in.read(buffer)) != -1) {
            out.write(buffer, 0, temp);
        }
        out.close();
    }

    public static String[] mergeArrays(String[] array1, String[] array2) {
        List<String> array1_ = new ArrayList<>(Arrays.asList(array1));
        List<String> array2_ = new ArrayList<>(Arrays.asList(array2));

        array1_.addAll(array2_);

        return (String[]) array1_.toArray(array1);
    }

    public static String getMainClazz(File jarFile) {
        String mainClazz = null;

        try {
            ZipFile jar = new ZipFile(jarFile);
            mainClazz = getMainByJar(jar);

            jar.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mainClazz;
    }

    private static String getMainByJar(ZipFile jar) {
        String mainClazz = "";

        try {
            Enumeration<? extends ZipEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                if("net/minecraft/client/main/Main.class".equals(entry.getName())) {
                    mainClazz = "net/minecraft/client/main/Main";
                    break;
                }

                if ("META-INF/MANIFEST.MF".equals(entry.getName())) {
                    Manifest manifest = new Manifest(jar.getInputStream(entry));
                    Attributes attrs = manifest.getMainAttributes();
                    mainClazz = attrs.getValue("Main-Class");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mainClazz;
    }

    public static void readMainClazz(File jarFile, String mainClazz, Map<String, ClassNode> classes, Map<ZipEntry, byte[]> zipEntryMap) {
        try {
            ZipFile jar = new ZipFile(jarFile);
            Enumeration<? extends ZipEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                if (mainClazz.equals(entry.getName().replaceAll(".class", ""))) {
                    ClassReader cr = new ClassReader(jar.getInputStream(entry));
                    ClassNode cn = new ClassNode();
                    cr.accept(cn, ClassReader.SKIP_FRAMES);
                    classes.put(cn.name, cn);
                    continue;
                }
                if (!entry.isDirectory()) {
                    zipEntryMap.put(entry, toByteArray(jar.getInputStream(entry)));
                }
            }

            jar.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getCommand(String command, WindowsBit windowsBit, String... str) {
        // ≈–∂œstr «∑Ò≥…∂‘
        if ((str.length % 2) != 0) {
            throw new RuntimeException("Wrong args");
        }

        for (int i = 0; i < str.length; i += 2) {
            String target = str[i];
            String replacement = str[i + 1];

            command = command.replace(target, replacement);
        }

        switch (windowsBit) {
        case x32:
            command = command.replaceAll("#WINDOWS_BIT#", "32");
            break;
        case x64:
            command = command.replaceAll("#WINDOWS_BIT#", "64");
            break;
        default:
            throw new RuntimeException("WTF?! WINDOWS BIT " + windowsBit.name());
        }

        return command;
    }

    public static String getFileNames(WindowsBit windowsBit, File dir) {
        File folder = new File(dir, windowsBit.name());
        File[] files = folder.listFiles((file, name) -> name.endsWith(".o"));

        StringBuilder names = new StringBuilder();
        Arrays.asList(files).forEach(file -> names.append(file.getPath().replace(System.getProperty("user.dir"), ".")).append(" "));

        return names.toString();
    }
}
