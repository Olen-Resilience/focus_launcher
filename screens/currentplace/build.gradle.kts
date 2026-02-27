plugins {
    id("focuslauncher.android.feature")
    id("focuslauncher.screen.new")
    id("focuslauncher.android.library.compose.testing")
}

android {
    namespace = "dev.mslalith.focuslauncher.screens.currentplace"
}

dependencies {
    implementation(projects.core.lint)
    implementation(libs.osmdroid)
}
