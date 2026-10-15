// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.gradle.versions)
    alias(libs.plugins.version.catalog.update)
}

// Configuration for automated version updates
versionCatalogUpdate {
    sortByKey = true
    keep {
        // Keep plugins and existing definitions organized
        keepUnusedVersions = true
    }
}
