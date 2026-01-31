# AutoService annotation-based ProGuard/R8 rules
# These rules preserve any class annotated with @AutoService along with their
# no-argument constructor, which is required by ServiceLoader.

# Keep the annotations themselves so R8 can process annotation-based rules
-keep class com.fueledbycaffeine.autoservice.AutoService
-keep class com.google.auto.service.AutoService

# Keep classes annotated with our annotation
-keep @com.fueledbycaffeine.autoservice.AutoService class * {
    <init>();
}

# Keep classes annotated with Google's annotation (for compatibility)
-keep @com.google.auto.service.AutoService class * {
    <init>();
}
