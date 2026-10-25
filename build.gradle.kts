plugins {
    java
}

group = "dev.etran"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    // Only needed if you pass -PwithBwApi (see below).
    maven("https://repo.andrei1058.dev/releases/")
}

dependencies {
    // Spigot API. The plugin targets 1.17+ (uses World#getMinHeight and modern BlockData).
    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")

    // The plugin drives Bedwars1058 through its setup commands (the setup session is not part of the
    // public API), so the API jar is optional. Enable with: ./gradlew build -PwithBwApi -PbwApiVersion=<version>
    if (project.hasProperty("withBwApi")) {
        val bwApi = (project.findProperty("bwApiVersion") ?: "22.0") as String
        compileOnly("com.andrei1058.bedwars:bedwars-api:$bwApi")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    archiveBaseName.set("BedwarsAutoSetup")
}
