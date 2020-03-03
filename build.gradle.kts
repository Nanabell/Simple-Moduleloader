plugins {
    id("com.jfrog.bintray") version "1.8.4"
    kotlin("jvm") version "1.3.61"
    id("maven-publish")
    id("java-library")
    java
}

group = "com.nanabell.quickstart"
version = "0.5.1"

repositories {
    maven("https://dl.bintray.com/nanabell/Sponge-Minecraft")
    maven("https://repo.spongepowered.org/maven/")
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("org.spongepowered:configurate-core:3.6.1")
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    compileOnly("org.slf4j:slf4j-api:1.7.25")
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

            pom {
                name.set("Simple Moduleloader")
                description.set("A Module Loading Framework with the focus on Simplicity")

                developers {
                    developer {
                        id.set("nanabell")
                        name.set("Nanabell")
                    }
                }

                scm {
                    connection.set("scm:git@github.com:Nanabell/Simple-Moduleloader.git")
                    developerConnection.set("scm:git@github.com:Nanabell/Simple-Moduleloader.git")
                    url.set("https://github.com/Nanabell/Simple-Moduleloader")
                }
            }
        }
    }
}

bintray {
    user = project.findProperty("bintray.user").toString()
    key = project.findProperty("bintray.key").toString()
    setPublications("moduleloader")
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