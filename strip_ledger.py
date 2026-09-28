import re

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'r') as f:
    text = f.read()

# Strip all manual ledger insertions
garbage = r"\s*_ledgerEntries\.value = _ledgerEntries\.value \+ newLedger\s*kotlinx\.coroutines\.GlobalScope\.launch\(Dispatchers\.IO\) \{ dao\?\.insertLedgerEntry\(newLedger\) \}"
text = re.sub(garbage, "", text)

# Write back
with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'w') as f:
    f.write(text)
