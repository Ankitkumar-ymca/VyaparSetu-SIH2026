import re

with open('app/src/main/java/com/example/data/repository/AiService.kt', 'r') as f:
    content = f.read()

old_pi_prompt = """                You are a smart inventory assistant.
                Analyze the following product data and provide a 1-sentence insight (e.g., 'Selling well, consider restocking soon' or 'Slow moving, maybe offer a discount')."""
new_pi_prompt = """                You are a smart inventory assistant.
                Analyze the following product data and provide a 1-sentence insight (e.g., 'Selling well, consider restocking soon' or 'Slow moving, maybe offer a discount').
                ${getLanguageInstruction()}"""

if "${getLanguageInstruction()}" not in old_pi_prompt:
    content = content.replace(old_pi_prompt, new_pi_prompt)
    with open('app/src/main/java/com/example/data/repository/AiService.kt', 'w') as f:
        f.write(content)
