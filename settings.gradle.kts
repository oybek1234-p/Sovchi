pluginManagement {
    repositories {
        maven {
            url = uri("https://www.jitpack.io")
        }
        mavenCentral()
        jcenter()
        google()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://www.jitpack.io") }
        google()
        jcenter()
        mavenCentral()
        mavenLocal()
    }
}
rootProject.name = "Sovchi"
include(":app")
 