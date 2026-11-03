# ============================================================
# ProGuard-Regeln für CCraft (Fabric Mod)
# Ziel: eigenen Code umbenennen/verschleiern, aber Mod-Loader,
# Mixins und Entry-Points unangetastet lassen, damit der Mod
# weiterhin lädt.
# ============================================================

-dontwarn
-ignorewarnings
-optimizationpasses 3
-allowaccessmodification
-dontpreverify

# --- Entry Points ---------------------------------------------------
# Trage hier die Klassen ein, die in deiner fabric.mod.json unter
# "entrypoints" stehen (main, client, server). Ohne diese Regel
# findet der Mod-Loader die Klasse nach dem Umbenennen nicht mehr
# und der Mod startet nicht.
-keep class de.consolecrafter.ccraft.CCraftMod { *; }
-keep class de.consolecrafter.ccraft.CCraftModClient { *; }
# Falls du weitere Entrypoints hast (z.B. ModMenu-Integration), hier ergänzen:
# -keep class de.consolecrafter.ccraft.CCraftModMenu { *; }

# --- Mixins -----------------------------------------------------------
# Mixin-Klassen MÜSSEN ihren vollen Namen behalten, sonst passt der
# Refmap nicht mehr zur umbenannten Klasse und es gibt einen Crash
# beim Mixin-Apply.
-keep @org.spongepowered.asm.mixin.Mixin class * { *; }
-keepclassmembers @org.spongepowered.asm.mixin.Mixin class * { *; }

# Falls du mixins.json(s) hast, die Pakete referenzieren, zusätzlich:
-keep class de.consolecrafter.ccraft.mixin.** { *; }

# --- Fabric-API / Loader-Klassen, die per Reflection genutzt werden ---
-keep class * implements net.fabricmc.api.ModInitializer { *; }
-keep class * implements net.fabricmc.api.ClientModInitializer { *; }
-keep class * implements net.fabricmc.api.DedicatedServerModInitializer { *; }

# --- GSON (nutzt Reflection über Feldnamen, z.B. in AccountStore) -----
-keepclassmembers class de.consolecrafter.ccraft.util.AccountStore { *; }
-keepattributes Signature,*Annotation*

# --- bcrypt-Library (nicht umbenennen, kommt als eigener Jar mit rein) -
-keep class at.favre.lib.crypto.bcrypt.** { *; }
-keep class at.favre.lib.bytes.** { *; }

# --- Minecraft/Yarn-Mappings: nie umbenennen, nur eigenen Code --------
-keep class net.minecraft.** { *; }
-keep class net.fabricmc.** { *; }
-keep class com.mojang.** { *; }
-keep class com.viaversion.** { *; }

# --- Eigener Code: HIER findet die eigentliche Obfuskierung statt -----
# Alles in de.consolecrafter.ccraft, was NICHT oben durch -keep
# geschützt ist (also z.B. LoginScreen, AccountStore-Interna,
# Theme, CraftConfig-Interna), wird umbenannt und zusammengepackt.
-repackageclasses 'de.consolecrafter.ccraft.o'
-flattenpackagehierarchy
-overloadaggressively
-useuniqueclassmembernames

# Stacktraces bleiben halbwegs nutzbar (Zeilen erhalten, nur Namen weg)
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable