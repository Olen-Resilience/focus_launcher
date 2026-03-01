# Preserve line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ───────── Kotlin ─────────
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Lazy {
    <fields>;
}

# ───────── Coroutines ─────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ───────── Hilt / Dagger ─────────
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
}

# ───────── Circuit (Slack) ─────────
# Keep all Circuit KSP-generated factory classes contributed via @IntoSet
-keep class dev.mslalith.focuslauncher.**.*Factory { *; }
-keep class dev.mslalith.focuslauncher.**.*FactoryModule { *; }
-keep class * implements com.slack.circuit.runtime.presenter.Presenter { *; }
-keep class * implements com.slack.circuit.runtime.presenter.Presenter$Factory { *; }
-keep class * implements com.slack.circuit.runtime.ui.Ui { *; }
-keep class * implements com.slack.circuit.runtime.ui.Ui$Factory { *; }
-keep class com.slack.circuit.** { *; }
-dontwarn com.slack.circuit.**

# ───────── Room ─────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.**

# ───────── DataStore / Protobuf ─────────
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ───────── Ktor ─────────
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ───────── Sentry ─────────
-keep class io.sentry.** { *; }
-dontwarn io.sentry.**
-dontwarn edu.umd.cs.findbugs.annotations.Nullable
-dontwarn org.slf4j.impl.StaticLoggerBinder

# ───────── Parcelize ─────────
-keep class * implements android.os.Parcelable { *; }
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# ───────── Screen objects (Circuit screens are Parcelable objects) ─────────
-keep class dev.mslalith.focuslauncher.core.screens.** { *; }

# ───────── App model classes ─────────
-keep class dev.mslalith.focuslauncher.core.model.** { *; }