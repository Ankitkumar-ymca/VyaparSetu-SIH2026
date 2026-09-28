import re

with open('app/src/main/java/com/example/data/repository/AiService.kt', 'r') as f:
    content = f.read()

# Add getLanguageInstruction helper
if "private fun getLanguageInstruction()" not in content:
    helper = """
    private fun getLanguageInstruction(): String {
        val isHindi = java.util.Locale.getDefault().language == "hi"
        return if (isHindi) {
            "Respond ONLY in Hindi using Devanagari script (हिन्दी). Do not use Hinglish, Roman Hindi, or English alphabets for Hindi words. English technical/product names may remain in English only when necessary."
        } else {
            "Respond ONLY in English."
        }
    }
    
    private fun getApiKey(): String {"""
    content = content.replace("    private fun getApiKey(): String {", helper)

# Update answerBusinessQuestion
old_abq_prompt = """                IMPORTANT: You MUST reply in the EXACT SAME LANGUAGE the user used in their question (English, Hindi, or Hinglish)."""
new_abq_prompt = """                ${getLanguageInstruction()}"""
content = content.replace(old_abq_prompt, new_abq_prompt)

# Update generateForecastAdvice
old_gfa_prompt = """                IMPORTANT: You MUST reply in the EXACT SAME LANGUAGE as requested, typically English or Hindi, depending on the context. But for this endpoint, assume English unless specified. Actually, just provide it in English, the app will handle translation or the user will read it. Wait, the prompt says "Generate practical recommendations based on actual detected conditions."
                Use bullet points. Do NOT invent new numbers."""
new_gfa_prompt = """                ${getLanguageInstruction()}
                Use bullet points. Do NOT invent new numbers."""
content = content.replace(old_gfa_prompt, new_gfa_prompt)

# Update generateBusinessInsights
old_gbi_prompt = """                Focus on sales trends, low stock, or outstanding balances.
                Do NOT invent numbers. Use ONLY the provided context. If there is not enough data, say "More business data is required before making a reliable recommendation."
                
                Format as a JSON array of strings: ["Observation: ... Reason: ... Recommended Action: ...", "...", "..."]"""
new_gbi_prompt = """                Focus on sales trends, low stock, or outstanding balances.
                Do NOT invent numbers. Use ONLY the provided context. If there is not enough data, say "More business data is required before making a reliable recommendation."
                ${getLanguageInstruction()}
                
                Format as a JSON array of strings: ["Observation: ... Reason: ... Recommended Action: ...", "...", "..."]"""
content = content.replace(old_gbi_prompt, new_gbi_prompt)

# Check getProductInsights
if "getProductInsights" in content:
    pass

with open('app/src/main/java/com/example/data/repository/AiService.kt', 'w') as f:
    f.write(content)
