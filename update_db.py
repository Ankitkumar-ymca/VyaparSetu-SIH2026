import re

with open('app/src/main/java/com/example/data/repository/AppDatabase.kt', 'r') as f:
    text = f.read()

text = text.replace('version = 1', 'version = 2')

with open('app/src/main/java/com/example/data/repository/AppDatabase.kt', 'w') as f:
    f.write(text)

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'r') as f:
    text = f.read()

migration_code = """        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Product ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE Customer ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE Supplier ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
            }
        }
        db = androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, "vyapar_db").addMigrations(MIGRATION_1_2).allowMainThreadQueries().build()"""

text = text.replace('db = androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, "vyapar_db").allowMainThreadQueries().build()', migration_code)

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'w') as f:
    f.write(text)
