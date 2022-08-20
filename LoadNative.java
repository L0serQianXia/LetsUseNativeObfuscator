import java.io.*;

import me.qianxia.DllDependent;

/**
 * @Author: QianXia
 * @Description: To load the library
 * @Date: 2020/12/30-20:42
 */
public class LoadNative {
    public static void OMG(){
        try {
            String outPath32 = System.getProperty("java.io.tmpdir") + System.currentTimeMillis() + "x32.dll";
            String outPath64 = System.getProperty("java.io.tmpdir") + System.currentTimeMillis() + "x64.dll";

            InputStream inputStream32 = LoadNative.class.getResourceAsStream("/native0/x86-windows.dll");
            InputStream inputStream64 = LoadNative.class.getResourceAsStream("/native0/x64-windows.dll");
            OutputStream outputStream32 = new FileOutputStream(outPath32);
            OutputStream outputStream64 = new FileOutputStream(outPath64);

            int temp;
            byte[] data = new byte[1024];
            while ((temp = inputStream32.read(data)) != -1){
                outputStream32.write(data,0,temp);
            }

            data = new byte[1024];
            while ((temp = inputStream64.read(data)) != -1){
                outputStream64.write(data,0, temp);
            }

            new File(outPath32).deleteOnExit();
            new File(outPath64).deleteOnExit();

            inputStream32.close();
            inputStream64.close();
            outputStream32.close();
            outputStream64.close();

            try {
                System.load(outPath32);
            } catch (UnsatisfiedLinkError e) {
                if(e.getMessage().contains("Can't find")) {
                    printDependents(outPath32);
                }

                try {
					System.load(outPath64);
				} catch (UnsatisfiedLinkError e1) {
					if(e1.getMessage().contains("Can't find")) {
                        printDependents(outPath64);
					}
					e1.printStackTrace();
				}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printDependents(String dllPath) {
        String result = DllDependent.findDependents(dllPath, true);
        System.err.println("==================Fatal Error==================");
        System.err.println("We did'nt find DLL(s):" + result);
        System.err.println("You should put them into \"C:\\Windows\\System32\"");
        System.err.println("And restart the process");
        System.err.println("==================Fatal Error==================");
        System.exit(-1);
    }
}
