plugins { id("com.android.application") }
android {
 namespace = "app.aethertv.cinema.probe"
 compileSdk = 36
 defaultConfig { applicationId = "app.aethertv.cinema.probe"; minSdk=23; targetSdk=34; versionCode=1; versionName="1" }
 buildTypes { release { signingConfig=signingConfigs.getByName("debug") } }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17;targetCompatibility=JavaVersion.VERSION_17 }
}
