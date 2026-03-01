dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    // REMOVED: versionCatalogs block - root project already defines it
}

rootProject.name = "build-logic"
include(":convention")
