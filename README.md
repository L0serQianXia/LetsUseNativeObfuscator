# LetsUseNativeObfuscator
It is a tool that helps you compile the source that created by NativeObfuscator(https://github.com/radioegor146/native-obfuscator)  
!!!**you should install MinGW-w64 first**!!!

# How to use
1. `java -jar native-obfuscator.jar <jarfile> <dir>` to create the CPP sources of your jar file  
2. copy `LetUsUseNativeObfuscator.jar, jni.h, jni_md.h, jvmti.h` to `<dir>\cpp\`  
3. open cmd.exe and run `cd <dir>\cpp`  
4. put `LetUsUseNativeObfuscator.jar` under `<dir>\cpp\`
5. run `java -jar LetUsUseNativeObfuscator.jar` and it will create DLL "My32DLL.dll" & "My64DLL.dll"
6. Jar files in the parent directory will be packaged with DLLs and can be run directly.
