# Wallpaper service must survive R8: it is instantiated by the system.
-keep class com.ascii.wallpaper.engine.AsciiWallpaperService { *; }
-keep class com.ascii.wallpaper.engine.AsciiWallpaperService$Engine { *; }
