import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val prometheusVersion = "0.5.0"
val cxfVersion = "3.2.7"
val wireMockVersion = "2.19.0"
val junitJupiterVersion = "5.3.1"

group = "no.nav.helse"
version = project.findProperty("projectVersion") ?: "0-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.3.20"
    `java-library`
    `maven-publish`
    signing
    id("org.jetbrains.dokka") version "0.9.17"
    id("io.codearte.nexus-staging") version "0.12.0"
}

buildscript {
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.2.0")
    }
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("javax.xml.ws:jaxws-api:2.3.0")
    implementation("org.apache.cxf:cxf-core:$cxfVersion")

    testCompile("com.sun.xml.ws:jaxws-rt:2.3.0")
    testCompile("org.apache.cxf:cxf-rt-frontend-jaxws:$cxfVersion")
    testCompile("org.apache.cxf:cxf-rt-transports-http:$cxfVersion")

    testCompile("no.nav.tjenestespesifikasjoner:person-v3-tjenestespesifikasjon:1.2019.01.16-21.19-afc54bed6f85")

    testCompile("com.github.tomakehurst:wiremock:$wireMockVersion")

    testCompile("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testCompile("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.named<KotlinCompile>("compileKotlin") {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "5.1.1"
}

val dokka = tasks.withType<DokkaTask> {
    outputFormat = "html"
    outputDirectory = "$buildDir/javadoc"
}

val sourcesJar by tasks.registering(Jar::class) {
    classifier = "sources"
    from(sourceSets["main"].allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(dokka)
    classifier = "javadoc"
    from(buildDir.resolve("javadoc"))
}

artifacts {
    add("archives", sourcesJar)
    add("archives", javadocJar)
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar.get())
            artifact(javadocJar.get())
            
            pom {
                name.set(project.name)
                description.set("Prometheus metrics feature for CXF Clients")
                url.set("https://github.com/navikt/cxf-prometheus-metrics")
                withXml {
                    asNode().appendNode("packaging", "jar")
                }
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        organization.set("NAV (Arbeids- og velferdsdirektoratet) - The Norwegian Labour and Welfare Administration")
                        organizationUrl.set("https://www.nav.no")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/navikt/cxf-prometheus-metrics.git")
                    developerConnection.set("scm:git:https://github.com/navikt/cxf-prometheus-metrics.git")
                    url.set("https://github.com/navikt/cxf-prometheus-metrics.git")
                }
            }
        }
    }

    repositories {
        maven {
            credentials {
                username = System.getenv("OSSRH_JIRA_USERNAME")
                password = System.getenv("OSSRH_JIRA_PASSWORD")
            }
            val version = "${project.version}"
            url = if (version.endsWith("-SNAPSHOT")) {
                uri("https://oss.sonatype.org/content/repositories/snapshots")
            } else {
                uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            }
        }
    }
}

ext["signing.gnupg.keyName"] = System.getenv("GPG_KEY_NAME")
ext["signing.gnupg.passphrase"] = System.getenv("GPG_PASSPHRASE")
ext["signing.gnupg.useLegacyGpg"] = true

signing {
    useGpgCmd()
    sign(publishing.publications["mavenJava"])
}

nexusStaging {
    username = System.getenv("OSSRH_JIRA_USERNAME")
    password = System.getenv("OSSRH_JIRA_PASSWORD")
    packageGroup = "no.nav"
}
