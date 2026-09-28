import xml.etree.ElementTree as ET

# Read English strings
tree = ET.parse('app/src/main/res/values/strings.xml')
root = tree.getroot()

# Very basic dictionary mapping for common words (if I miss some, it falls back to English, but I'll try to map as many as possible)
# Since I can't translate all 162 perfectly in a hardcoded dictionary without typing them all out,
# I will use a simple heuristic or I can just write a python script that replaces known English words with Hindi.
