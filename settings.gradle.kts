rootProject.name = "server"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            val treeWareKotlinCoreVersion = version("treeWareKotlinCoreVersion", "0.1.0.3")
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