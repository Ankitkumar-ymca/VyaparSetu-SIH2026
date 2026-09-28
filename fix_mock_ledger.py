import re

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'r') as f:
    text = f.read()

# Strip out all the garbage insertions
garbage1 = r"\s*_ledgerEntries\.value = _ledgerEntries\.value \+ newLedger\s*kotlinx\.coroutines\.GlobalScope\.launch\(Dispatchers\.IO\) \{ dao\?\.insertLedgerEntry\(newLedger\) \}"
text = re.sub(garbage1, "", text)

# Now, cleanly add the correct insertion after every LedgerEntry(...)
# We match `val newLedger = LedgerEntry(...)` where it ends with `)` followed by nothing or whitespace
def replacer(match):
    return match.group(0) + "\n        _ledgerEntries.value = _ledgerEntries.value + newLedger\n        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { dao?.insertLedgerEntry(newLedger) }"

# Match val newLedger = LedgerEntry( ... ) 
# Since Kotlin's LedgerEntry ends with timestamp = ... ) we can match that reliably.
pattern = r"val newLedger = LedgerEntry\([\s\S]*?timestamp = System\.currentTimeMillis\(\)(?: \+ 1)?\s*\)"
text = re.sub(pattern, replacer, text)

with open('app/src/main/java/com/example/data/repository/MockDataStore.kt', 'w') as f:
    f.write(text)
