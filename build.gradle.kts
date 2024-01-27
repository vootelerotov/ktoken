import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    `maven-publish`
    `signing`
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
}

group = "io.github.vootelerotov.ktoken"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
    testImplementation("org.assertj:assertj-core:3.25.1")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("ktoken")
                description.set("Generate RSA SecureID 128-bit (AES) tokens, like Stoken.")
                url.set("https://github.com/vootelerotov/ktoken/")

                licenses {
                    license {
                        name.set("GNU Lesser General Public License v2.1")
                        url.set("https://www.gnu.org/licenses/old-licenses/lgpl-2.1.en.html")
                    }
                }

                developers {
                    developer {
                        id.set("vootelerotov")
                        name.set("Vootele Rotov")
                        email.set("vootele.rotov@gmail.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/vootelerotov/ktoken.git")
                    developerConnection.set("scm:git:git://github.com/vootelerotov/ktoken.git")
                    url.set("https://github.com/vootelerotov/ktoken")
                }
            }
         }
    }

}

signing {
    sign(publishing.publications["mavenJava"])
}


nexusPublishing {
    repositories {
        sonatype {  //only for users registered in Sonatype after 24 Feb 2021
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(project.properties["osshr.login"] as String)
            password.set(project.properties["osshr.password"] as String)
        }
    }
}
