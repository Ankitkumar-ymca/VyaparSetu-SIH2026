sed -i 's/package com.example.data.repository/package com.example.data.repository\n\nimport android.content.Context\nimport android.content.SharedPreferences\nimport kotlinx.serialization.json.Json\nimport kotlinx.serialization.encodeToString\nimport kotlinx.serialization.decodeFromString/g' app/src/main/java/com/example/data/repository/MockDataStore.kt

cat << 'INNEREOF' > store_patch.kt
    private var prefs: SharedPreferences? = null
    private var storedUsers: MutableList<User> = mutableListOf()
    private var storedBusinesses: MutableList<Business> = mutableListOf()
    
    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences("mock_data_store", Context.MODE_PRIVATE)
        loadFromPrefs()
    }

    private fun loadFromPrefs() {
        val prefs = prefs ?: return
        val usersJson = prefs.getString("users", "[]") ?: "[]"
        val businessesJson = prefs.getString("businesses", "[]") ?: "[]"
        try {
            storedUsers = Json.decodeFromString(usersJson)
            storedBusinesses = Json.decodeFromString(businessesJson)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val currentUserId = prefs.getString("currentUserId", null)
        if (currentUserId != null) {
            val user = storedUsers.find { it.id == currentUserId }
            if (user != null) {
                _currentUser.value = user
                if (user.businessId != null) {
                    _currentBusiness.value = storedBusinesses.find { it.id == user.businessId }
                }
            }
        }
    }

    private fun saveToPrefs() {
        val prefs = prefs ?: return
        prefs.edit().apply {
            putString("users", Json.encodeToString(storedUsers))
            putString("businesses", Json.encodeToString(storedBusinesses))
            putString("currentUserId", _currentUser.value?.id)
            apply()
        }
    }
INNEREOF

sed -i -e '/object MockDataStore {/r store_patch.kt' app/src/main/java/com/example/data/repository/MockDataStore.kt
