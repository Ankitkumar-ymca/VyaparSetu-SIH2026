    suspend fun login(email: String, password: String): Result<User> {
        delay(NETWORK_DELAY)
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(Exception("Email and password cannot be empty"))
        }
        val existingUser = storedUsers.find { it.email == email || it.phone == email }
        if (existingUser != null) {
            _currentUser.value = existingUser
            _currentBusiness.value = existingUser.businessId?.let { bid -> storedBusinesses.find { it.id == bid } }
            saveToPrefs()
            return Result.success(existingUser)
        }
        return Result.failure(Exception("Invalid credentials. Please try again."))
    }

    suspend fun register(name: String, email: String, phone: String, password: String): Result<User> {
        delay(NETWORK_DELAY)
        val existingUser = storedUsers.find { it.email == email || it.phone == phone }
        if (existingUser != null) {
            return Result.failure(Exception("User already exists"))
        }
        val newUser = User(
            id = UUID.randomUUID().toString(),
            name = name,
            email = email,
            phone = phone,
            role = "OWNER"
        )
        storedUsers.add(newUser)
        _currentUser.value = newUser
        saveToPrefs()
        return Result.success(newUser)
    }

    suspend fun setupBusiness(
        name: String,
        ownerName: String,
        category: String,
        phone: String,
        address: String,
        gstin: String?
    ): Result<Business> {
        delay(NETWORK_DELAY)
        val user = _currentUser.value ?: return Result.failure(Exception("User not authenticated"))
        
        val newBusiness = Business(
            id = UUID.randomUUID().toString(),
            name = name,
            ownerName = ownerName,
            category = category,
            phone = phone,
            address = address,
            gstin = gstin,
            createdAt = System.currentTimeMillis()
        )
        storedBusinesses.add(newBusiness)
        val updatedUser = user.copy(businessId = newBusiness.id)
        val userIndex = storedUsers.indexOfFirst { it.id == updatedUser.id }
        if (userIndex != -1) {
            storedUsers[userIndex] = updatedUser
        }
        _currentBusiness.value = newBusiness
        _currentUser.value = updatedUser
        saveToPrefs()
        return Result.success(newBusiness)
    }

    suspend fun logout() {
        delay(300)
        _currentUser.value = null
        _currentBusiness.value = null
        saveToPrefs()
        _products.value = emptyList()
        _customers.value = emptyList()
        _suppliers.value = emptyList()
        _stockAdjustments.value = emptyList()
        _customerPayments.value = emptyList()
        _sales.value = emptyList()
        _purchases.value = emptyList()
        _expenses.value = emptyList()
        _stockTransactions.value = emptyList()
        _ledgerEntries.value = emptyList()
    }
