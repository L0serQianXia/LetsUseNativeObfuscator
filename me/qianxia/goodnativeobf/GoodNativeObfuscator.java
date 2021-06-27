package me.qianxia.goodnativeobf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

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
    private static final String PROGRAM_UPDATE_MESSAGE = "---NativeObfuscator编译助手---\n" +
            "2021年6月27日更新\n" +
            "1.修复含空格的目录无法编译的漏洞\n" +
            "---NativeObfuscator编译助手---\n";

    public static void main(String[] args) {
        System.out.println(PROGRAM_UPDATE_MESSAGE);

        for(int i = 0; i < args.length; ++i) {
            if ("--input".equalsIgnoreCase(args[i])) {
                inputName = args[i + 1];
            }
        }

        if (inputName == null) {
            inputName = System.getProperty("user.dir");
        }

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
            Process process = Runtime.getRuntime().exec("g++ -m64 -shared " + all64CppFiles + " -static-libstdc++ -static-libgcc -o x64/My64Shit.dll");
            process.waitFor();
        } catch (Exception var8) {
            var8.printStackTrace();
        }
        try {
            Process process = Runtime.getRuntime().exec("g++ -m32 -shared " + all32CppFiles + " -static-libstdc++ -static-libgcc -o x32/My32Shit.dll");
            process.waitFor();
        } catch (Exception var8) {
            var8.printStackTrace();
        }

        System.out.println("Finished.");
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
