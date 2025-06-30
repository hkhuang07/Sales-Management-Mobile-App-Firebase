plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.salesmanagement"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.salesmanagement"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled= true

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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("androidx.preference:preference:1.2.0") // Use the latest stable version
    implementation("com.google.firebase:firebase-messaging:23.0.0") // Hoặc phiên bản mới nhất


    // Thêm các dependencies của Firebase Firestore
    //Firebase platform BOM (recommended for managing Firebase versions)

    // Kiểm tra phiên bản mới nhất của Firebase BOM trên trang chủ Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.0.0")) // Đổi sang cú pháp Kotlin DSL: dùng ngoặc tròn và ngoặc kép


    // Firebase Cloud Firestore SDK
    implementation("com.google.firebase:firebase-firestore") // Đổi sang cú pháp Kotlin DSL: dùng ngoặc tròn và ngoặc kép

    // FirebaseUI for Firestore
    implementation("com.firebaseui:firebase-ui-firestore:8.0.2") // Hoặc phiên bản mới nhất
    // Thư viện tải ảnh Glide (rất khuyến nghị)
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation ("androidx.cardview:cardview:1.0.0")

    // Các dịch vụ Firebase khác (ví dụ nếu bạn cần xác thực hoặc lưu trữ file)
    implementation("com.google.firebase:firebase-auth")
    // Google Sign-In (Optional, if you plan to use Google login)
    implementation("com.google.android.gms:play-services-auth:21.0.0") // Use the latest version

    // Firebase Storage
    implementation("com.google.firebase:firebase-storage:21.0.0")

}