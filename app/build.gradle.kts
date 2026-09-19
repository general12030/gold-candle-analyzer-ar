plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android { namespace = "com.goldcandle.analyzer"; compileSdk = 35
    defaultConfig { applicationId = "com.goldcandle.analyzer"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "0.1.0" }
}

kotlin { jvmToolchain(17) }

buildFeatures { compose = true }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.2")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    kapt("androidx.room:room-compiler:2.6.1")
}
