# LetsUseNativeObfuscator
It is a tool that helps you compile the source that created by NativeObfuscator(https://github.com/radioegor146/native-obfuscator)  
Now tested on NativeObfuscator v1.7b & 3.3.3r  

!!!**Please install MinGW-w64 first**!!!  
https://sourceforge.net/projects/mingw-w64/files/  
We recommend use `x86_64-posix-sjlj`

# How to use
1. `java -jar native-obfuscator.jar <jarfile> <dir>` to create the CPP sources of your jar file  
2. copy `LetUsUseNativeObfuscator.jar` to `<dir>\cpp\`  
3. open cmd.exe and run `cd <dir>\cpp`
4. run `java -jar LetUsUseNativeObfuscator.jar` and it will create DLL "My32DLL.dll" & "My64DLL.dll"
5. Jar files in parent directory will be packaged with DLLs and can be run directly.
