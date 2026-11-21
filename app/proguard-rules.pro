# ---------------------------------------------------------------------------
# DepotDownloader file filter (DepotFiles.enableFileFilter) reaches these by reflection.
# Keeping them also stops R8 from assuming their default values (false / empty).
# DepotFilesTest fails if a JavaSteam upgrade renames them.
-keepclassmembers class in.dragonbra.javasteam.depotdownloader.DepotDownloader {
    private in.dragonbra.javasteam.depotdownloader.DepotDownloader$Config config;
}
-keepclassmembers class in.dragonbra.javasteam.depotdownloader.DepotDownloader$Config {
    boolean usingFileList;
    java.util.List filesToDownloadRegex;
}

# ---------------------------------------------------------------------------
# JavaSteam: instantiates handlers, callbacks and protobuf messages reflectively
# (e.g. ClientMsgProtobuf looks up newBuilder()/parseFrom() by class).
-keep class in.dragonbra.javasteam.** { *; }

# protobuf-java (full, not lite): GeneratedMessage field accessors use reflection.
-keep class com.google.protobuf.** { *; }

# BouncyCastle registers its algorithms by class name; JavaSteam needs RSA/None/PKCS1Padding
# and AES/CBC/PKCS7Padding from it (see SoundtrackApp).
-keep class org.bouncycastle.jce.provider.** { *; }
-keep class org.bouncycastle.jcajce.provider.** { *; }

# Chunk decompression: zstd-jni binds native methods by name.
-keep class com.github.luben.zstd.** { *; }

# ---------------------------------------------------------------------------
# Navigation 3 saves the back stack with a reflection-based NavKey serializer on Android.
-keep class * implements androidx.navigation3.runtime.NavKey { *; }
-keepclassmembers class * implements androidx.navigation3.runtime.NavKey {
    public static ** Companion;
    public static ** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------------------------------------------------------------------------
# Optional dependencies referenced by JavaSteam / Ktor / OkHttp / commons-lang3 that don't exist
# on Android. Filled in from R8's missing-class report.
-dontwarn org.spongycastle.**
