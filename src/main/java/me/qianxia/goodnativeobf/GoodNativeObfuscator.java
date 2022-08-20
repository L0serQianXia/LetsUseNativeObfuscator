package me.qianxia.goodnativeobf;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * @Author: QianXia
 * @Description: Helping you compiler the source that created by
 *               NativeObfuscator
 * @Date: 2021/01/04-20:34
 */
public class GoodNativeObfuscator {
	private static int count;
	private static final Map<ZipEntry, byte[]> zipEntryMap = new HashMap<>();
	private static final Map<String, ClassNode> classes = new HashMap<>();
	
	public static final float VERSION = 1.23F;
	public static final String BASE_COMMAND = "g++ -m#WINDOWS_BIT# -c \"#FILE_NAME#\" -o \"#OUTPUT_FILE_NAME#\"";
	public static final String BASE_DLL_COMMAND = "g++ -m#WINDOWS_BIT# -shared #FILES# -static-libstdc++ -static-libgcc -lwinpthread -Bdynamic -o x#WINDOWS_BIT#/My#WINDOWS_BIT#DLL.dll";

	public static final String NOTE = "_____________LetsUseNativeObfuscator v" + VERSION + "_____________\n\n"
			+ "   This is a tool to help you compiler the source\n" + "   that created by NativeObfuscator\n"
			+ "   (https://github.com/radioegor146/native-obfuscator)\n\n"
			+ "   https://github.com/L0serQianXia/LetsUseNativeObfuscator\n\n"
			+ "   !!!you should install Mingw-w64(http://mingw-w64.org/) first!!!\n\n"
			+ "_____________LetsUseNativeObfuscator v" + VERSION + "_____________";
	
	public static void main(String[] args) {
		System.out.println(NOTE);

		String inputName = System.getProperty("user.dir");
		System.out.println("[INFO]Compiling the CPP files");
		compileCpp(inputName);
		System.out.println("[INFO]Packing the JAR with DLL");
		packJar(inputName);
		System.out.println("[INFO]Finished.");
	}

	public static void compileCpp(String inputName) {
		File dir = new File(inputName);
		File[] files = dir.listFiles((file, name) -> name.endsWith(".cpp"));
		if(files == null || files.length == 0) {
			System.err.print("[ERROR]We did not find the CPP file!");
			System.exit(0);
		}
		
		compile(WindowsBit.x32, files, dir);
		compile(WindowsBit.x64, files, dir);
	}
	
	public static void compile(WindowsBit windowsBit, File[] files, File dir) {
		new File(dir, windowsBit.name()).mkdir();
		

		Arrays.asList(files).forEach((file) -> runCommand(BASE_COMMAND, windowsBit, file, 
				file.getName().replace(".cpp", ".o"), "x#WINDOWS_BIT#/" + file.getName().replace(".cpp", ".o")));
		
		
		dir = new File(dir, "\\output\\");
		files = dir.listFiles((file, name) -> name.endsWith(".cpp"));

		Arrays.asList(files).forEach((file) -> runCommand(BASE_COMMAND, windowsBit, file, 
				file.getName().replace(".cpp", ".o"), "x#WINDOWS_BIT#/" + file.getName().replace(".cpp", ".o"), 
				"output\\x", "x"));

		String fileNames = Utils.getFileNames(windowsBit, dir.getParentFile());
		runCommand(BASE_DLL_COMMAND, windowsBit, dir, "#FILES#", fileNames);
	}

	public static void packJar(String inputName) {
		packJar(WindowsBit.x32, inputName);
		packJar(WindowsBit.x64, inputName);
	}

	public static void runCommand(String baseCommand, WindowsBit windowsBit, File file, String... str) {
		try {
			String filePath = file.getCanonicalPath().replace(System.getProperty("user.dir"), ".");
			String command = Utils.getCommand(baseCommand, windowsBit,
					Utils.mergeArrays(new String[]{"#FILE_NAME#", filePath, "#OUTPUT_FILE_NAME#", filePath.replace(".cpp", ".o")}, str));

			System.out.println("[DEBUG] Running: " + command);
			Process process = Runtime.getRuntime().exec(command);
			process.waitFor();

			System.out.println("[DEBUG]" + ++count);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void packJar(WindowsBit windowsBit, String inputName) {
		String dllPath, jarFilePath = null;
		File jarFile, dllFile;

		String unformatted = inputName + "\\x#WINDOWS_BIT#\\My#WINDOWS_BIT#DLL.dll";
		dllPath = unformatted.replaceAll("#WINDOWS_BIT#", windowsBit.name().replace("x", ""));

		File dir = new File(inputName).getParentFile();
		
		File[] files = dir.listFiles((file, name) -> name.endsWith(".jar"));
		for(File file : files) {
			jarFilePath = file.getAbsolutePath();
		}
		
		jarFile = new File(jarFilePath);
		dllFile = new File(dllPath);

		if (!jarFile.exists() || !dllFile.exists()) {
			System.err.println("Jar File or DLL File not Found!!!");
			return;
		}
		
		String mainClazz = Utils.getMainClazz(jarFile);
		System.out.println("[INFO]Main-Class:" + mainClazz);
		
		//x64位为第二次运行 无需再次加载
		if(windowsBit != WindowsBit.x64) {
			Utils.readMainClazz(jarFile, mainClazz, classes, zipEntryMap);
		}

		try {
			InputStream inputStream = GoodNativeObfuscator.class.getResourceAsStream("/LoadNative.class");
			InputStream inputDll = new FileInputStream(dllFile);

			// Add Native loader and DLL file
			zipEntryMap.put(new ZipEntry("LoadNative.class"), Utils.toByteArray(inputStream));
			zipEntryMap.put(new ZipEntry("native0/" + (windowsBit.name().equals("x32") ? "x86" : "x64") + "-windows.dll"), Utils.toByteArray(inputDll));

			boolean hasSTATIC = false;
			// Add loadLibrary() to Main Class
			for(ClassNode node : classes.values()){
				if(!mainClazz.equals(node.name)) {
					return;
				}
				for(MethodNode method : node.methods) {
					if("<clinit>".equals(method.name)) {
						hasSTATIC = true;
					}
				}
				
				for(MethodNode method : node.methods) {
					// 非Minecraft的Jar文件无需处理
					if("main".equals(method.name) && "native0/Bootstrap".equals(mainClazz)) {
						// 入口方法被混淆native
						if(method.instructions.size() == 0) {
							return;
						}
						for(AbstractInsnNode insnNode : method.instructions.toArray()) {
							if(!(insnNode instanceof MethodInsnNode)) {
								continue;
							}
							MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
							// remove loadLibrary() in main
							boolean flag = "loadLibrary".equals(methodInsnNode.name)
									&& "java/lang/System".equals(methodInsnNode.owner)
									&& methodInsnNode.getPrevious().getOpcode() == Opcodes.LDC
									&& ((String) ((LdcInsnNode) methodInsnNode.getPrevious()).cst).contains("native_jvm_classes");
							if(flag) {
								method.instructions.remove(methodInsnNode.getPrevious());
								method.instructions.remove(methodInsnNode);
								if(!hasSTATIC) {
									addLoadLibrary(method);
								}
							}
						}
					}
					
					if("<clinit>".equals(method.name)) {
						addLoadLibrary(method);
					}
				}
			}
			
			if(windowsBit == WindowsBit.x64) {
				saveJar(jarFilePath + "QIANXIA");
				jarFile.renameTo(new File(jarFilePath.replace(".jar", "_BACKUP.jar")));
				new File(jarFilePath + "QIANXIA").renameTo(new File(jarFilePath));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void addLoadLibrary(MethodNode method) {
		InsnList list = new InsnList();
		list.add(new LabelNode());
		list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "LoadNative", "OMG", "()V", false));

		method.instructions.insertBefore(method.instructions.getFirst(), list);
	}

	private static void saveJar(String name) {
		try {
			ZipOutputStream out = new ZipOutputStream(new FileOutputStream(name));

			// 将检测DLL缺失的相关类打包入Jar
			{
				String path = System.getProperty("java.io.tmpdir")  + "fdsgbfdsfdsfhds.jar";
	            Utils.copyFileStream(GoodNativeObfuscator.class.getResourceAsStream("/DllDependents.jar"), new FileOutputStream(path));
	            new File(path).deleteOnExit();

	            ZipFile zipFile = new ZipFile(new File(path));
	            Enumeration<? extends ZipEntry> entries = zipFile.entries();
	            
	            while(entries.hasMoreElements()) {
	            	ZipEntry entry = entries.nextElement();
	            	if(entry.getName().contains("MANIFEST.MF") || entry.getName().contains("META-INF")) {
	            		continue;
	            	}
	            	byte[] b = Utils.toByteArray(zipFile.getInputStream(entry));
	            	out.putNextEntry(entry);
	            	out.write(b);
	            	out.closeEntry();
	            }
	            
			}
            
			zipEntryMap.forEach((entry, b) -> {
				try {
					out.putNextEntry(entry);
					out.write(b);
					out.closeEntry();
				} catch (Exception e) {
					if(e.getMessage().contains("LoadNative")) {
						return;
					}
					e.printStackTrace();
				}
			});

			classes.values().forEach(classNode -> {
				try {
					ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
					try {
						classNode.accept(cw);
					} catch (Exception e) {
						System.out.println("[WARNING]" + classNode.name + " COMPUTE_MAXS");
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
			
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
