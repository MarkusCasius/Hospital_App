# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- SECURITY HARDENING RULES ---

# 1. Obfuscate the EncryptionManager more aggressively
# We keep the constructor and public methods because our app calls them,
# but we allow all *other* member names (private fields, methods) to be obfuscated.
-keep class com.example.hospimanagmenetapp.security.EncryptionManager {
    public <init>(...); # Keep constructor
    public static *** encrypt*(...); # Keep all static encrypt methods
    public static *** decrypt*(...); # Keep all static decrypt methods
}

# 2. Obfuscate the RuntimeGuard we are about to create
# We keep the public methods that our Activities will call, but the internal checks are obfuscated.
-keep class com.example.hospimanagmenetapp.security.RuntimeGuard {
    public static *** check*(...); # Keep all static check methods
    public static *** initialize(...); # Keep the initialize method
}

# 3. Add dictionary-based obfuscation to make names meaningless
# This makes it harder for an attacker to guess what a method does from its name.
-obfuscationdictionary /app/src/main/res/raw/obfuscation_dictionary.txt
-classobfuscationdictionary /app/src/main/res/raw/obfuscation_dictionary.txt