plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    `maven-publish`
}

group = "com.github.sherifshabans"
version = "1.2.0"

android {
    namespace = "io.github.sherifshabans.mushaf"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // The ayah text and layout ship as plain assets; never compress them away.
    androidResources {
        noCompress += listOf("tsv", "txt")
    }

    testOptions {
        // الـharness البصري بيرسم بـSkia الحقيقية وخط المصحف الحقيقي، فمحتاج
        // موارد أندرويد (الخط والأصول) في اختبارات الوحدة.
        unitTests {
            isIncludeAndroidResources = true
            all { it.systemProperties(listOfNotNull(project.findProperty("mushaf.pages")?.let { v -> "mushaf.pages" to v }).toMap()) }
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.06.01")
    implementation(composeBom)
    api("androidx.compose.ui:ui")
    api("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-graphics")

    testImplementation("junit:junit:4.13.2")
    testImplementation(composeBom)
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("androidx.activity:activity-compose:1.10.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.compose.ui:ui-test-manifest")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.sherifshabans"
            artifactId = "mushaf-text"
            version = project.version.toString()
            afterEvaluate { from(components["release"]) }

            pom {
                name.set("mushaf-text")
                description.set(
                    "Madinah Mushaf rendered as text in Jetpack Compose: the exact " +
                        "15-line page layout, KFGQPC Hafs font, and tajweed colouring."
                )
                url.set("https://github.com/sherifshabans/mushaf-text")
            }
        }
    }
}
