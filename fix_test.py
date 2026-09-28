import re

with open('app/src/test/java/com/example/ForecastTest.kt', 'r') as f:
    content = f.read()

content = content.replace("MockDataStore.currentBusiness.value =", "MockDataStore.updateCurrentBusiness(")
content = content.replace("        )", "        )\n        )")

with open('app/src/test/java/com/example/ForecastTest.kt', 'w') as f:
    f.write(content)
