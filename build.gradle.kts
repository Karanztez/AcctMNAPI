plugins {
    kotlin("jvm") version "2.1.20"
    `java-library`
    `maven-publish`
}

group = "com.github.Karanztez"
version = "1.0.6"
description = "Public API for other plugins to integrate with AcctMN"

base {
    archivesName.set("acctmn-api")
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly(kotlin("stdlib"))
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        javaParameters.set(true)
    }
}

java {
    withSourcesJar()
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "AcctMN API",
            "Implementation-Version" to version,
            "Implementation-Vendor" to "GordyX",
            "Automatic-Module-Name" to "acctmn.api"
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("gpr") {
            groupId = "com.github.Karanztez"
            artifactId = "AcctMNAPI"
            version = "1.0.6"
            from(components["java"])
            pom {
                name.set("AcctMN API")
                description.set(project.description)
                url.set("https://github.com/Karanztez/AcctMNAPI")
                licenses {
                    license {
                        name.set("MIT")
                    }
                }
                developers {
                    developer {
                        id.set("GordyX")
                        name.set("GordyX")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Karanztez/AcctMNAPI")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
