# Default ProGuard rules for pokke-app
# Add project-specific rules here.

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
