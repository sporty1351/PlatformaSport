-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class ru.platformasport.club.** {
    *** Companion;
}
-keepclasseswithmembers class ru.platformasport.club.** {
    kotlinx.serialization.KSerializer serializer(...);
}
