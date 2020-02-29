plugins {
    id("com.jfrog.bintray") version "1.8.4"
    kotlin("jvm") version "1.3.61"
    id("maven-publish")
    id("java-library")
    java
}

group = "com.nanabell.quickstart"
version = "0.5.0"

repositories {
    maven("https://dl.bintray.com/nanabell/Sponge-Minecraft")
    maven("https://repo.spongepowered.org/maven/")
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    compileOnly("org.slf4j:slf4j-api:1.7.25")
    compileOnly("org.spongepowered:configurate-core:3.6.1")
}


tasks {
    java {
        withJavadocJar()
        withSourcesJar()
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

publishing {
    publications {
        create<MavenPublication>("moduleloader") {
            from(components["java"])
        }
    }
}

bintray {
    user = project.findProperty("bintrayUser").toString()
    key = project.findProperty("bintrayKey").toString()
    publish = false
    pkg.apply {
        repo = "Sponge-Minecraft"
        name = "simple-moduleloader"
        version.apply {
            name = "v${project.version}"
            vcsTag = "v${project.version}"
        }
    }
}