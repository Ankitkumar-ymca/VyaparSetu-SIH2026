import re

with open('app/src/main/java/com/example/presentation/purchases/PurchaseDetailScreen.kt', 'r') as f:
    text = f.read()

# Add imports
imports = """
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.presentation.utils.PdfGenerator
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
"""
text = text.replace("import java.util.*", "import java.util.*\n" + imports)

# Find the start of PurchaseDetailScreen function
setup_code = """
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null && purchase != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    PdfGenerator.generatePurchasePdf(context, purchase, supplier, outputStream)
                    Toast.makeText(context, "PDF Saved Successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }
"""
text = text.replace("val dateFormat = remember", setup_code + "\n    val dateFormat = remember")

# Replace onClick
text = text.replace('onClick = { /* TODO */ }', 'onClick = { launcher.launch("Purchase_${purchase?.referenceNumber ?: "Invoice"}.pdf") }')

with open('app/src/main/java/com/example/presentation/purchases/PurchaseDetailScreen.kt', 'w') as f:
    f.write(text)
