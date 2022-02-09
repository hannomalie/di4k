plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    base
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(kotlin("stdlib"))
    ksp(project(":processor"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
    testImplementation("org.assertj:assertj-core:3.19.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    sourceSets.main.kotlin.srcDir("build/generated/ksp/main/kotlin/")
}
