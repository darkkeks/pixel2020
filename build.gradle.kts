plugins {
    java
    kotlin("jvm") version "1.4.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation("com.google.code.gson:gson:2.8.6")
    implementation("javax.websocket:javax.websocket-api:1.0")
    implementation("org.glassfish.tyrus.bundles:tyrus-standalone-client:1.15")

    implementation("org.apache.logging.log4j", "log4j-slf4j-impl", "2.8.2")
    implementation("org.apache.logging.log4j", "log4j-api", "2.8.2")
    implementation("org.apache.logging.log4j", "log4j-core", "2.8.2")

    testCompile("junit", "junit", "4.12")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}
