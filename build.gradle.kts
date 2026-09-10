plugins {
    java
}

group = "com.autofarm.mods"
version = "1.0.0"

repositories {
    mavenCentral()
}

val hytaleServerPath = System.getenv("HYTALE_SERVER_JAR") 
    ?: "C:/Users/joao_/AppData/Roaming/Hytale/install/release/package/game/latest/Server/HytaleServer.jar"

dependencies {
    compileOnly(files(hytaleServerPath))
    testImplementation(files(hytaleServerPath))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveBaseName.set("AutoFarm")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.register<Copy>("installMod") {
    dependsOn(tasks.jar)
    from(tasks.jar.get().archiveFile)
    into("C:/Users/joao_/AppData/Roaming/Hytale/UserData/Mods")
}
