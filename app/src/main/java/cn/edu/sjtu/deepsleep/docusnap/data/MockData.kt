package cn.edu.sjtu.deepsleep.docusnap.data

import java.util.UUID
import android.util.Base64
import java.io.ByteArrayOutputStream
import android.graphics.Bitmap
import android.graphics.BitmapFactory

object MockData {
    // Create IDs first so we can reference them
    private val starbucksReceiptId = UUID.randomUUID().toString()
    private val officeInvoiceId = UUID.randomUUID().toString()
    private val employmentContractId = UUID.randomUUID().toString()
    private val expenseFormId = UUID.randomUUID().toString()
    private val visaFormId = UUID.randomUUID().toString()

    // Helper function to create mock base64 images
    private fun createMockBase64Image(color: Int, text: String): String {
        // Create bitmap with initial color
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888).apply {
            eraseColor(color)
        }

        // Create canvas and paint
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.WHITE
            textSize = 24f
        }

        // Draw text on bitmap
        canvas.drawText(text, 50f, 100f, paint)

        // Convert to byte array
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        // Return base64 string
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    val mockDocuments = listOf(
        Document(
            id = starbucksReceiptId,
            name = "Lunch Receipt - Starbucks",
            description = "A lunch receipt from Starbucks showing coffee and sandwich purchase for $12.50.",
            imageBase64s = listOf(
                createMockBase64Image(android.graphics.Color.RED, "Starbucks Receipt 1"),
                createMockBase64Image(android.graphics.Color.RED, "Starbucks Receipt 2")
            ),
            extractedInfo = mapOf(
                "Vendor" to "Starbucks Coffee",
                "Date" to "2024-01-15",
                "Time" to "12:30 PM",
                "Total Amount" to "$12.50",
                "Payment Method" to "Credit Card",
                "Items" to "Latte, Sandwich"
            ),
            tags = listOf("Food", "Expense", "Coffee"),
            relatedFileIds = listOf(officeInvoiceId, expenseFormId)
        ),
        Document(
            id = officeInvoiceId,
            name = "Office Supply Invoice",
            description = "An invoice for office supplies totaling $1,245.50 from OfficeSupply Co.",
            imageBase64s = listOf(
                createMockBase64Image(android.graphics.Color.BLUE, "Office Invoice")
            ),
            extractedInfo = mapOf(
                "Supplier" to "OfficeSupply Co.",
                "Invoice Number" to "INV-2024-0876",
                "Date" to "2024-01-10",
                "Due Date" to "2024-02-10",
                "Total Amount" to "$1,245.50",
                "Payment Status" to "Unpaid"
            ),
            tags = listOf("Business", "Invoice", "Office"),
            relatedFileIds = listOf(starbucksReceiptId, expenseFormId)
        ),
        Document(
            id = employmentContractId,
            name = "Employment Contract",
            description = "A full-time employment contract for John Doe as Software Engineer at TechCorp Inc.",
            imageBase64s = listOf(
                createMockBase64Image(android.graphics.Color.GREEN, "Contract Page 1"),
                createMockBase64Image(android.graphics.Color.GREEN, "Contract Page 2"),
                createMockBase64Image(android.graphics.Color.GREEN, "Contract Page 3")
            ),
            extractedInfo = mapOf(
                "Company" to "TechCorp Inc.",
                "Employee" to "John Doe",
                "Position" to "Software Engineer",
                "Start Date" to "2024-02-01",
                "Salary" to "$85,000/year",
                "Contract Type" to "Full-time"
            ),
            tags = listOf("Employment", "Contract", "Legal"),
            relatedFileIds = listOf(visaFormId)
        )
    )

    val mockForms = listOf(
        Form(
            id = expenseFormId,
            name = "Expense Report Form",
            description = "A company expense report form for tracking business expenses and reimbursements.",
            imageBase64s = listOf(
                createMockBase64Image(android.graphics.Color.YELLOW, "Expense Form")
            ),
            extractedInfo = mapOf(
                "Form Type" to "Expense Report",
                "Company" to "TechCorp Inc.",
                "Department" to "Engineering",
                "Fiscal Year" to "2024"
            ),
            tags = listOf("Business", "Expense", "Form"),
            formFields = listOf(
                FormField("Employee Name", "John Doe", true, srcFileId = employmentContractId),
                FormField("Department", "Engineering", true, srcFileId = employmentContractId),
                FormField("Date", "2024-01-15", true, srcFileId = starbucksReceiptId),
                FormField("Purpose", "", false, srcFileId = null),
                FormField("Amount", "$12.50", true, srcFileId = starbucksReceiptId),
                FormField("Manager Approval", "", false, srcFileId = null)
            ),
            relatedFileIds = listOf(starbucksReceiptId, officeInvoiceId)
        ),
        Form(
            id = visaFormId,
            name = "Visa Application",
            description = "A visa application form for travel to Japan with personal and travel details.",
            imageBase64s = listOf(
                createMockBase64Image(android.graphics.Color.CYAN, "Visa Form 1"),
                createMockBase64Image(android.graphics.Color.CYAN, "Visa Form 2")
            ),
            extractedInfo = mapOf(
                "Form Type" to "Visa Application",
                "Country" to "Japan",
                "Application Type" to "Tourist Visa",
                "Processing Time" to "5-7 business days"
            ),
            tags = listOf("Travel", "Visa", "Form"),
            formFields = listOf(
                FormField("Full Name", "John Doe", true, srcFileId = employmentContractId),
                FormField("Date of Birth", "1990-05-15", true, srcFileId = null),
                FormField("Passport Number", "A12345678", true, srcFileId = null),
                FormField("Travel Purpose", "", false, srcFileId = null),
                FormField("Destination", "Japan", true, srcFileId = null),
                FormField("Duration", "2 weeks", true, srcFileId = null)
            ),
            relatedFileIds = listOf(employmentContractId)
        )
    )


}