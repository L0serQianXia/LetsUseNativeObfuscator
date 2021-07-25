package me.qianxia.goodnativeobf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.tree.AbstractInsnNode;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import jdk.internal.org.objectweb.asm.tree.InsnList;
import jdk.internal.org.objectweb.asm.tree.LabelNode;
import jdk.internal.org.objectweb.asm.tree.LdcInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;

/**
 * @Author: QianXia
 * @Description: Helping you compiler the source that created by
 *               NativeObfuscator
 * @Date: 2021/01/04-20:34
 */
public class GoodNativeObfuscator {
	private static int count;
	private static Map<ZipEntry, byte[]> zipEntryMap = new HashMap<>();
	private static Map<String, ClassNode> classes = new HashMap<>();
	
	public static final float VERSION = 1.21F;
	public static final String BASE_COMMAND = "g++ -m#WINDOWS_BIT# -c \"#FILE_NAME#\" -o \"#OUTPUT_FILE_NAME#\"";
	public static final String BASE_DLL_COMMAND = "g++ -m#WINDOWS_BIT# -shared #FILES# -static-libstdc++ -static-libgcc -Wl,-Bstatic,--whole-archive -lwinpthread -Wl,--no-whole-archive -Wl,-Bdynamic -o x#WINDOWS_BIT#/My#WINDOWS_BIT#DLL.dll";

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
		if(files.length == 0) {
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

		String fileNames = getFileNames(windowsBit, dir.getParentFile());
		runCommand(BASE_DLL_COMMAND, windowsBit, dir, "#FILES#", fileNames);
	}


	public static void packJar(String inputName) {
		packJar(WindowsBit.x32, inputName);
		packJar(WindowsBit.x64, inputName);
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

	public static void copyFile(String fromFile, String toFile) {
		try (FileInputStream fis = new FileInputStream(fromFile); FileOutputStream fos = new FileOutputStream(toFile)) {
			int len;
			byte[] buffer = new byte[4096];
			while ((len = fis.read(buffer)) > 0) {
				fos.write(buffer, 0, len);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String getCommand(String command, WindowsBit windowsBit, String... str) {
		// 判断str是否成对
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

	public static String[] mergeArrays(String[] array1, String[] array2) {
		List<String> array1_ = new ArrayList<>(Arrays.asList(array1));
		List<String> array2_ = new ArrayList<>(Arrays.asList(array2));

		array1_.addAll(array2_);

		return (String[]) array1_.toArray(array1);
	}

	public static void runCommand(String baseCommand, WindowsBit windowsBit, File file, String... str) {
		try {
			String filePath = file.getAbsolutePath();
			String command = getCommand(baseCommand, windowsBit,
					mergeArrays(new String[]{"#FILE_NAME#", filePath, "#OUTPUT_FILE_NAME#", filePath.replace(".cpp", ".o")}, str));

			Process process = Runtime.getRuntime().exec(command);
			process.waitFor();
			
			System.out.println("[DEBUG]" + command);
			count++;
			System.out.println("[DEBUG]" + count);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String getFileNames(WindowsBit windowsBit, File dir) {
		File folder = new File(dir, windowsBit.name());
		File[] files = folder.listFiles((file, name) -> name.endsWith(".o"));
		
		StringBuilder names = new StringBuilder();
		Arrays.asList(files).forEach(file -> names.append("\"").append(file.getPath()).append("\"").append(" "));

		return names.toString();
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
		
		String mainClazz = getMainClazz(jarFile);
		System.out.println("[INFO]Main-Class:" + mainClazz);
		
		//x64位为第二次运行 无需再次加载
		if(windowsBit != WindowsBit.x64) {
			readMainClazz(jarFile, mainClazz);
		}
		
		
		try {
			InputStream inputStream = GoodNativeObfuscator.class.getResourceAsStream("/LoadNative.class");
			InputStream inputDll = new FileInputStream(dllFile);

			// Add Native loader and DLL file
			zipEntryMap.put(new ZipEntry("LoadNative.class"), toByteArray(inputStream));
			zipEntryMap.put(new ZipEntry("META-INF/OhDear" + (windowsBit.equals(WindowsBit.x32) ? "" : "64")), toByteArray(inputDll));

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
	
	private static String getMainClazz(File jarFile) {
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
	
	private static void readMainClazz(File jarFile, String mainClazz) {
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
				zipEntryMap.put(entry, toByteArray(jar.getInputStream(entry)));
			}
			
			jar.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
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
	
	private static void saveJar(String name) {
		try {
			ZipOutputStream out = new ZipOutputStream(new FileOutputStream(name));
			
			{
				String path = System.getProperty("java.io.tmpdir")  + "fdsgbfdsfdsfhds.jar";
				InputStream inputStream = GoodNativeObfuscator.class.getResourceAsStream("/DllDependents.jar");
	            OutputStream outputStream = new FileOutputStream(path);
	            int temp;
	            byte[] data = new byte[1024];
	
	            while ((temp = inputStream.read(data)) != -1){
	                outputStream.write(data,0,temp);
	            }
	
	            new File(path).deleteOnExit();
	            
	            inputStream.close();
	            outputStream.close();
	            
	            ZipFile zipFile = new ZipFile(new File(path));
	            Enumeration<? extends ZipEntry> entries = zipFile.entries();
	            
	            while(entries.hasMoreElements()) {
	            	ZipEntry entry = entries.nextElement();
	            	if(entry.getName().contains("MANIFEST.MF") || entry.getName().contains("META-INF")) {
	            		continue;
	            	}
	            	byte[] b = toByteArray(zipFile.getInputStream(entry));
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
