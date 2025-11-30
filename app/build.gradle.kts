import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)

}

android {
    namespace = "lorry.dossiertau"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "lorry.dossiertau"
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = listOf("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode",
            "-XXLanguage:+NestedTypeAliases"
        )
    }

    buildFeatures {
        compose = true
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-receivers")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE-notice.md"
        }
    }

    tasks.withType<Test> {
        jvmArgs("-XX:+EnableDynamicAgentLoading")
        // Pour diagnostiquer : "-Djdk.instrument.traceUsage"
    }
}

dependencies {
    // --- Unit tests (testImplementation) ---
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("app.cash.turbine:turbine:1.1.0")        // test des Flow
    testImplementation("io.mockk:mockk:1.14.6")
    //tester si méthode existe
    testImplementation("org.jetbrains.kotlin:kotlin-reflect:1.9.0")


    // --- Android instrumented tests (androidTestImplementation) ---
    androidTestImplementation(libs.androidx.junit)              // AndroidX JUnit 1.2.1
    androidTestImplementation(platform(libs.androidx.compose.bom)) // BOM Compose 2025.04.01
    androidTestImplementation(libs.androidx.ui.test.junit4)     // Tests UI Compose
    debugImplementation(libs.androidx.ui.test.manifest)         // Manifeste de test Compose
    androidTestImplementation("io.mockk:mockk-android:1.13.12")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.56.2")
    androidTestImplementation("app.cash.turbine:turbine:1.1.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    //tester si méthode existe
    androidTestImplementation("org.jetbrains.kotlin:kotlin-reflect:2.0.21")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.work.runtime.ktx)
//    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.compose.livedata)
    implementation(libs.androidx.lifecycle.service)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.accompanist.flowlayout)
    implementation("me.saket.cascade:cascade:2.3.0")
    implementation("me.saket.cascade:cascade-compose:2.3.0")
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)

//    implementation(libs.compressor)

    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("org.mp4parser:isoparser:1.9.27")
    implementation("com.github.mjeanroy:exiftool-lib:2.6.0")
    implementation("com.robertlevonyan.compose:buttontogglegroup:1.2.0")

    implementation("com.github.yalantis:ucrop:2.2.9-native")
    implementation("commons-net:commons-net:3.11.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.github.skydoves:colorpicker-compose:1.1.2")
    implementation(libs.kotlinx.serialization.json)
    implementation("com.google.android.material:material:1.12.0")
    //memoEditor
    implementation("com.mohamedrejeb.richeditor:richeditor-compose:1.0.0-rc13")
    implementation("com.github.Shivamdhuria:palette:0.0.4")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.appcompat:appcompat:1.6.1")

    //pull to refresh
    //https://github.com/MateriiApps/pullrefresh
    implementation("dev.materii.pullrefresh:pullrefresh:1.4.0-beta03")

    //FAB matérial 3
    //https://github.com/ch4rl3x/SpeedDialFloatingActionButton
//    implementation ("de.charlex.compose:speeddial-bottomappbar-material3:1.1.1")

    //FAB material 2
    //https://github.com/ch4rl3x/SpeedDialFloatingActionButton
//    implementation("de.charlex.compose:speeddial:1.1.1")

    //speed dial
    //https://github.com/leinardi/FloatingActionButtonSpeedDial
    implementation("com.leinardi.android:speed-dial.compose:1.0.0-alpha04")

    //générer un thème
    //https://github.com/KvColorPalette/KvColorPalette-Android
    implementation("com.github.KvColorPalette:KvColorPalette-Android:3.1.0")

    implementation("com.arkivanov.decompose:decompose:2.1.1")
    implementation("com.arkivanov.decompose:extensions-compose-jetbrains:2.1.1")

    //options
    //https://github.com/peter-tackage/kotlin-options
    implementation("com.github.peter-tackage.kotlin-options:kotlin-options:0.10")

    val room1 = "2.7.0"
    implementation("androidx.room:room-runtime:$room1")
    implementation("androidx.room:room-ktx:$room1")   // coroutines, Flow, transactions suspend
    ksp("androidx.room:room-compiler:$room1") // annotation processor (remplace kapt

    implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.21")

    /////////////////////////////////
    // programmation fonctionnelle //
    /////////////////////////////////
    implementation("io.arrow-kt:arrow-core:1.2.4")

    //////////////////////////////
    // injection de dépendances //
    //////////////////////////////
    implementation("io.insert-koin:koin-android:4.2.0-alpha1")
    implementation("io.insert-koin:koin-test:4.2.0-alpha1")
    implementation("io.insert-koin:koin-core:4.2.0-alpha1")

    ///////////////
    // WriterAPI //
    ///////////////
    //github -> https://jitpack.io/#lowley/WriterAPI
//    implementation("com.github.lowley:periscope:v1.0.17")
//    implementation("io.github.lowley:periscope:1.0.4")

    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")

    ////////////////////////////////////////
    // constraintLayout dans MainActivity //
    ////////////////////////////////////////
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
// To use constraintlayout in compose
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.1")


}