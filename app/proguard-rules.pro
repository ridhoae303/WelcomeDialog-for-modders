-dontusemixedcaseclassnames
-dontpreverify

# Safe optimization for old ProGuard on AIDE.
-allowaccessmodification
-optimizationpasses 2
-optimizations !code/simplification/advanced,!code/removal/advanced,!class/merging/*

# Use only one package-obfuscation mode.
# The old file used both -repackageclasses and -flattenpackagehierarchy.
-flattenpackagehierarchy 'a'

# Required for inner/anonymous classes and enums.
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# Hide the original source-file names in release stack traces.
-renamesourcefileattribute SourceFile

# Main launcher Activity: keep its manifest-visible class name.
-keep,allowoptimization class com.ridhoae303.app.main.MainActivity {
    public <init>();
}

# Dialog Activity and its externally callable entry point.
-keep,allowoptimization class com.takane.app.TakaneActivity {
    public <init>();
    public static void Niyaniya(android.content.Context);
}

# Keep standard enum lookup methods.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
