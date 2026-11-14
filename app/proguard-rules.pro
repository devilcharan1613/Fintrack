# Tracmio v16.13 ProGuard / R8 Keep Rules

# Room & SQLite / SQLCipher
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class net.zetetic.** { *; }
-dontwarn net.zetetic.**

# AndroidX Security Crypto / Tink
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# AndroidX Biometric
-keep class androidx.biometric.** { *; }

# BroadcastReceivers & Core Models
-keep class com.tracemeo.app.*Receiver { *; }
-keep class com.tracemeo.app.data.local.* { *; }
-keep class com.tracemeo.app.data.CashCountLine { *; }
-keep class com.tracemeo.app.data.AccountAllocation { *; }
-keep class com.tracemeo.app.data.QuickSpendChip { *; }
-keep class com.tracemeo.app.data.PendingRefundItem { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.flow.**
