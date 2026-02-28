plugins {
    id("focuslauncher.android.feature")
    id("focuslauncher.screen.new")
    id("focuslauncher.android.library.compose.testing")
}

android {
    namespace = "dev.mslalith.focuslauncher.screens.editfavorites"
}

dependencies {
    implementation(projects.core.ui)
    implementation(projects.core.lint)
    implementation(libs.kotlinx.collections.immutable)
}