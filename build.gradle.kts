plugins {
    id("java")
    id("org.openjfx.javafxplugin") version "0.1.0"
}

javafx {
    modules("javafx.controls", "javafx.fxml", "javafx.media")
}


group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}