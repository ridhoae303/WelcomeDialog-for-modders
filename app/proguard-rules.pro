-dontusemixedcaseclassnames
-allowaccessmodification
-overloadaggressively
-repackageclasses ''

-adaptclassstrings
-flattenpackagehierarchy

-dontpreverify
-optimizationpasses 9

-keepclassmembers class com.takane.app.TakaneActivity {
    public static void atsuko(android.content.Context);
}

-keepclassmembers class * {
    public static void atsuko(android.content.Context);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault
