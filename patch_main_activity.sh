sed -i 's/super.onCreate(savedInstanceState)/super.onCreate(savedInstanceState)\n    MockDataStore.init(applicationContext)/g' app/src/main/java/com/example/MainActivity.kt
