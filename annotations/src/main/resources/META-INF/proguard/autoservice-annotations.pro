# AutoService annotation-based ProGuard/R8 rules
# These rules preserve any class annotated with @AutoService along with their
# no-argument constructor, which is required by ServiceLoader.

# Keep runtime-visible annotations so annotation-based rules work
-keepattributes RuntimeVisibleAnnotations

# Keep service descriptor files and update them when classes are renamed
-adaptresourcefilenames META-INF/services/*
-adaptresourcefilecontents META-INF/services/*

# Keep classes annotated with our annotation
-keep @com.fueledbycaffeine.autoservice.AutoService class * {
    <init>();
}

# Keep classes annotated with Google's annotation (for compatibility)
-keep @com.google.auto.service.AutoService class * {
    <init>();
}
