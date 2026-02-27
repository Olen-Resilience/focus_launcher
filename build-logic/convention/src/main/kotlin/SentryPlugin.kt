import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import io.sentry.android.gradle.extensions.SentryPluginExtension
import io.sentry.android.gradle.extensions.SentryVariantExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import java.io.FileInputStream
import java.util.Properties

class SentryPlugin : Plugin<Project> {

    private companion object {
        private const val SENTRY_PROPERTIES_FILE = "sentry.properties"

        private const val SENTRY_DSN_ENV = "SENTRY_DSN"
        private const val SENTRY_DSN_PROPERTY = "sentry.dsn"

        private const val SENTRY_AUTH_TOKEN_ENV = "SENTRY_AUTH_TOKEN"
        private const val SENTRY_AUTH_TOKEN_PROPERTY = "sentry.auth.token"
    }

    override fun apply(target: Project) = with(target) {
        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

        pluginManager.apply {
            apply("com.android.application")
            apply(libs.findPlugin("sentry").get().get().pluginId)
        }

        // Read values – default to empty string to avoid null
        val sentryDsn = readSentryValueOf(propertyKey = SENTRY_DSN_PROPERTY, envKey = SENTRY_DSN_ENV, default = "")
        val sentryAuthToken = readSentryValueOf(propertyKey = SENTRY_AUTH_TOKEN_PROPERTY, envKey = SENTRY_AUTH_TOKEN_ENV, default = "")

        extensions.configure<ApplicationAndroidComponentsExtension> {
            onVariants { variant ->
                variant.manifestPlaceholders.put("sentryDsn", sentryDsn)
                variant.manifestPlaceholders.put("sentryEnvironment", variant.name)
            }
        }

        extensions.configure<SentryPluginExtension> {
            org.set("ms-m5")
            projectName.set("focus-launcher")
            authToken.set(sentryAuthToken)

            includeSourceContext.set(true)
            includeProguardMapping.set(true)
            tracingInstrumentation.enabled.set(false)
            ignoredBuildTypes.set(setOf("debug"))

            // Disable ProGuard mapping uploads for all dev variants
            variants.all { variant: SentryVariantExtension ->
                if (variant.name.contains("dev", ignoreCase = true)) {
                    variant.autoUploadProguardMapping.set(false)
                    logger.info("Sentry auto‑upload disabled for variant: ${variant.name}")
                }
            }
        }
    }

    /**
     * Reads a value from environment variable or sentry.properties.
     * Returns the default if no value is found.
     */
    private fun Project.readSentryValueOf(
        propertyKey: String,
        envKey: String,
        default: String
    ): String {
        // 1. Try environment variable (ignore blank)
        val envValue = providers.environmentVariable(envKey).orNull?.takeIf { it.isNotBlank() }
        if (envValue != null) return envValue

        // 2. Fallback to sentry.properties
        val propValue = readSentrySecret(key = propertyKey)
        if (propValue != null) return propValue

        // 3. Return default
        return default
    }

    private fun Project.readSentrySecret(key: String): String? {
        val secretPropertiesFile = rootProject.file(SENTRY_PROPERTIES_FILE)
        if (!secretPropertiesFile.exists()) {
            return null
        }

        val localProperties = Properties().apply { load(FileInputStream(secretPropertiesFile)) }
        return localProperties[key]?.toString()?.takeIf { it.isNotBlank() }
    }
}