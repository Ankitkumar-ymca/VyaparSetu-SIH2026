import re
with open('app/src/main/java/com/example/domain/model/Models.kt', 'r') as f:
    text = f.read()

text = text.replace("@kotlinx.serialization.Serializable\n@kotlinx.serialization.Serializable", "@kotlinx.serialization.Serializable")
text = text.replace("@androidx.room.Entity\n@androidx.room.Entity", "@androidx.room.Entity")

with open('app/src/main/java/com/example/domain/model/Models.kt', 'w') as f:
    f.write(text)
