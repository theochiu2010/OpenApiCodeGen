import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    id("org.springframework.boot") version "2.7.2"
    id("io.spring.dependency-management") version "1.0.12.RELEASE"
    id("org.openapi.generator") version "5.1.1"
    id("de.undercouch.download") version "5.1.0"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
}

group = "com.ikigai"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("javax.validation:validation-api:2.0.1.Final")
    implementation("org.jetbrains.kotlin:kotlin-script-runtime:1.7.0")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

configurations {
    all {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
        exclude(group = "ch.qos.logback", module = "logback-classic")
        exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.withType<Test> {
    useJUnitPlatform()
}

sourceSets {
    main {
        java {
            srcDir("${buildDir}/kotlin/src")
        }
    }
}

tasks.compileKotlin {
    dependsOn("generateContract")
}

tasks.register<OpenApiContractDownloadTask>("downloadContract") {
    producers = listOf(
        "planet:planetAPI:1.0.0",
        "inventory:inventoryAPI:1.0.0"
    )
    consumers = listOf(
        "nrs:nrsAPI:1.0.0",
        "pet:petAPI:1.0.0"
    )
}

tasks.register<OpenApiContractCodeGenTask>("generateContract") {
    dependsOn("downloadContract")
    outputDir = "$buildDir/kotlin"
}

tasks.register("buildKotlinContractCode", GenerateTask::class) {
    val yamlFilePath = properties["url"].toString()
    val typeOfGenerator = properties["type"].toString()
    val parsedGeneratorName = if (typeOfGenerator == "client") "kotlin" else "kotlin-spring"
    if (yamlFilePath.isNotEmpty()) {
        generatorName.set(parsedGeneratorName)
        inputSpec.set("$yamlFilePath")
        outputDir.set("${project.buildDir}/kotlin")
        apiPackage.set("org.openapitools.ikigai.api")
        configOptions.put("interfaceOnly", "true")
        configOptions.put("serializationLibrary", "gson")
    }
}

tasks.register("downloadOpenApiContract", Download::class) {
    var downloadFileUrl = properties["url"].toString()
    var downloadOpenApiType = properties["type"].toString()

    if (downloadFileUrl.isNotEmpty()) {
        var fileName = downloadFileUrl.substringBefore(".yaml").substringAfter("/")
        this.src(downloadFileUrl)
        if (downloadOpenApiType == "client") {
            this.dest(File("src/main/resources/static/", "client-$fileName.yaml"))
        } else {
            this.dest(File("src/main/resources/static/", "server-$fileName.yaml"))
        }
    }
}

abstract class OpenApiContractDownloadTask @Inject constructor() : DefaultTask() {
    @Input
    lateinit var producers: List<String>

    @Input
    lateinit var consumers: List<String>

    private val GRADLEW_EXECUTABLE = "./gradlew"
    private val DOWNLOAD_OPENAPI_CONTRACT = "downloadOpenApiContract"
    private val PARAMETER = "-P"
    private val ROOT = "https://raw.githubusercontent.com/theochiu2010/OpenApiContracts/main/contracts/"

    @TaskAction
    fun doWork() {
        var downloadProcesses = mutableListOf<Process>()

        try {
            producers.forEach {
                val url = constructUrl(it)
                println("Downloading producer OpenAPI from url: $url")
                val command = "$GRADLEW_EXECUTABLE $DOWNLOAD_OPENAPI_CONTRACT ${PARAMETER}url=$url ${PARAMETER}type=server"

                var process = Runtime.getRuntime().exec("$command")
                downloadProcesses.add(process)
            }

            consumers.forEach {
                val url = constructUrl(it)
                println("Downloading consumer OpenAPI from url: $url")
                val command = "$GRADLEW_EXECUTABLE $DOWNLOAD_OPENAPI_CONTRACT ${PARAMETER}url=$url ${PARAMETER}type=client"

                var process = Runtime.getRuntime().exec("$command")
                downloadProcesses.add(process)
            }

            while(downloadProcesses.any { x -> x.isAlive }) {
                Thread.sleep(100)
                // wait until download finishes
            }
        } catch (e: RuntimeException) {
            throw GradleException("Failed to download OpenAPI contract. ", e)
        }
    }

    private fun constructUrl(url: String): String {
        var urlParts = url.split(":")
        var boundedContext = urlParts[0]
        var apiName = urlParts[1]
        var majorVersion = urlParts[2].split(".")[0]

        return "$ROOT$boundedContext/OpenAPI/$apiName/v$majorVersion.yaml"
    }
}

abstract class OpenApiContractCodeGenTask @Inject constructor() : DefaultTask() {
    @Input
    lateinit var outputDir: String

    private val GRADLEW_EXECUTABLE = "./gradlew"
    private val BUILD_CONTRACT_CODE = "buildKotlinContractCode"
    private val PARAMETER = "-P"

    @TaskAction
    fun doWork() {
        try {
            var downloadProcesses = mutableListOf<Process>()
            var openApiContracts = mutableMapOf<String, String>()
            var dir = File("src/main/resources/static/")

            dir.listFiles().forEach{ file ->
                if (file.name.endsWith(".yaml")) {
                    var fileName = file.name
                    println("Processing file: $fileName from directory")
                    openApiContracts[fileName.substringBefore(".yaml")] = "src/main/resources/static/$fileName"
                }
            }

            println("Captured open api contracts: $openApiContracts")

            openApiContracts.forEach {
                if (it.key.contains("client-")) {
                    println("Generating Client Code: ${it.key}")
                    val genCodeCommand = "$GRADLEW_EXECUTABLE $BUILD_CONTRACT_CODE ${PARAMETER}url=${it.value} ${PARAMETER}type=client"

                    val process = Runtime.getRuntime().exec("$genCodeCommand")
                    downloadProcesses.add(process)
                } else {
                    println("Generating Server Code: ${it.key}")
                    val genCodeCommand = "$GRADLEW_EXECUTABLE $BUILD_CONTRACT_CODE ${PARAMETER}url=${it.value} ${PARAMETER}type=server"

                    val process = Runtime.getRuntime().exec("$genCodeCommand")
                    downloadProcesses.add(process)
                }
            }

            while(downloadProcesses.any { x -> x.isAlive }) {
                Thread.sleep(250)
                // wait until download finishes
            }
        } catch (e: RuntimeException) {
            throw GradleException("Failed to generate OpenAPI contract code. ", e)
        }
    }
}