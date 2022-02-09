plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.google.devtools.ksp:symbol-processing-api:1.6.10-1.0.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
    testImplementation("org.assertj:assertj-core:3.19.0")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.7")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.4.7")
}

tasks.test {
    useJUnitPlatform()
}