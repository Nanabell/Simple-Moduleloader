plugins {
    kotlin("jvm") version "1.3.61"
    java
}

group = "com.nanabell.quickstart"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven/")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("org.spongepowered:configurate-core:3.6.1")
}


tasks {
    java {
        withSourcesJar()
        withJavadocJar()
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}