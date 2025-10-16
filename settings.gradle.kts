pluginManagement { // Configure how Gradle locates plugins used in the build
    repositories { // Define the repositories that will host Gradle plugins
        google { // Use Google's Maven repository for Android/Google plugins
            content { // Restrict what groups are fetched from this repo (for speed and correctness)
                includeGroupByRegex("com\\.android.*") // Only include plugin groups starting with 'com.android'
                includeGroupByRegex("com\\.google.*") // Only include plugin groups starting with 'com.google'
                includeGroupByRegex("androidx.*") // Only include plugin groups starting with 'androidx'
            }
        }
        mavenCentral() // Also search Maven Central for plugins
        gradlePluginPortal() // And the official Gradle Plugin Portal
    }
}

dependencyResolutionManagement { // Control how project dependencies (not plugins) are resolved
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // Disallow module-level repos; enforce centralised repos here
    repositories { // The only repositories allowed for dependency resolution
        google() // Google's Maven repo for Android/Google libraries
        mavenCentral() // Maven Central for the wider JVM ecosystem
    }
}
rootProject.name = "HospiManagmenetApp" // The name shown for the root project in Gradle/IDE
include(":app") // Include the ':app' module in this multi-module build