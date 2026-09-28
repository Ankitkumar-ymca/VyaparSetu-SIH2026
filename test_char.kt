fun main() {
    val text = "यह एक परीक्षण है"
    val devanagariCount = text.count { it in 'ऀ'..'ॿ' }
    println("Devanagari Count: $devanagariCount")
}
