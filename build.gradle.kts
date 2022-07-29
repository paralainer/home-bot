import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    application
}

group = "com.paralainer"
version = "1.0-SNAPSHOT"
val telegramBotVersion = "6.0.7"
val ktorVersion = "1.6.8"
val koinVersion = "3.2.0"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    implementation("com.github.kotlin-telegram-bot:kotlin-telegram-bot:$telegramBotVersion") {
        exclude("com.github.kotlin-telegram-bot.kotlin-telegram-bot", "webhook")
    }
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-java:$ktorVersion")
    implementation("io.ktor:ktor-client-gson:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")

    implementation("io.insert-koin:koin-core:$koinVersion")

    implementation("org.yaml:snakeyaml:1.30")

    testImplementation(kotlin("test"))
}

tasks {
    val fatJar = register<Jar>("fatJar") {
        dependsOn.addAll(listOf("compileJava", "compileKotlin", "processResources"))
        archiveClassifier.set("standalone") // Naming the jar
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest { attributes(mapOf("Main-Class" to application.mainClass)) }
        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) } +
            sourcesMain.output
        from(contents)
    }
    build {
        dependsOn(fatJar) // Trigger fat jar creation during build
    }
}
tasks.test {
    useJUnitPlatform()
}

tasks.distTar {
    enabled = false
}

tasks.distZip {
    enabled = false
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "13"
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.time.ExperimentalTime"
}

application {
    mainClass.set("com.paralainer.homebot.HomeBotApplicationKt")
}
