# Remove the secrets plugin
sed -i '/alias(libs.plugins.secrets)/d' app/build.gradle.kts

# Remove the secrets { ... } block
sed -i '/secrets {/,+4d' app/build.gradle.kts

# Add buildConfigField to defaultConfig
# Find defaultConfig { and insert after it
sed -i '/defaultConfig {/a \    val geminiKey = System.getenv("GEMINI_API_KEY") ?: "MY_GEMINI_API_KEY"\n    buildConfigField("String", "GEMINI_API_KEY", "\\\"$geminiKey\\\"")' app/build.gradle.kts
