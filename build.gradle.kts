plugins {
    kotlin("jvm") version "1.6.10" apply(false)
    id("com.google.devtools.ksp") version "1.6.10-1.0.2" apply(false)
}

group = "de.hanno.di4k"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}
