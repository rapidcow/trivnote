plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":libexpr"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.register<JavaExec>("previewAppleNotesDrawing") {
    group = "scripts"
    description = "Preview Apple Notes drawing"
    mainClass = "org.rapidcow.trivnote.scripts.PreviewAppleNotesDrawing"
    classpath = sourceSets.main.get().runtimeClasspath
}
