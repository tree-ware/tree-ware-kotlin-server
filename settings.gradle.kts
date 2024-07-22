rootProject.name = "server"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal() // TODO #### delete
        maven { url = uri("https://jitpack.io") }
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.tree-ware.core") {
// TODO #### was used with jitpack//                useModule("org.tree-ware.tree-ware-gradle-core-plugin:org.tree-ware.core.gradle.plugin:${requested.version}")
                useModule("org.tree-ware.tree-ware-gradle-core-plugin:core-plugin:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            val treeWareKotlinCoreVersion = version("treeWareKotlinCoreVersion", "0.2.0.0-SNAPSHOT") // TODO #### drop -SNAPSHOT
            library("treeWareKotlinCore", "org.tree-ware.tree-ware-kotlin-core", "core").versionRef(
                treeWareKotlinCoreVersion
            )
            library(
                "treeWareKotlinCoreTestFixtures",
                "org.tree-ware.tree-ware-kotlin-core",
                "test-fixtures"
            ).versionRef(treeWareKotlinCoreVersion)
        }
    }
}