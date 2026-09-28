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
