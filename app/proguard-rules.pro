# Sherpa-ONNX: the native library reads these classes' fields and calls their constructors by
# name through JNI, so they must keep their names and members.
-keep class com.k2fsa.sherpa.onnx.** { *; }

# The native speech callback calls Function1.invoke(Object) on the Kotlin lambda by name.
-keep interface kotlin.jvm.functions.Function1 { *; }
-keepclassmembers class * implements kotlin.jvm.functions.Function1 {
    public java.lang.Object invoke(java.lang.Object);
}

# kotlinx.serialization: keep generated serializers of Saywell's own models.
-keepclassmembers @kotlinx.serialization.Serializable class io.github.lobojoel2003.saywell.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# commons-compress has optional codecs Saywell never uses (only bzip2 and tar).
-dontwarn org.tukaani.xz.**
-dontwarn com.github.luben.zstd.**
-dontwarn org.brotli.dec.**
-dontwarn org.objectweb.asm.**
-dontwarn org.osgi.**
