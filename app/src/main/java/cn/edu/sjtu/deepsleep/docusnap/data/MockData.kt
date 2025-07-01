package cn.edu.sjtu.deepsleep.docusnap.data

object MockData {
    val mockDocuments = listOf(
        Document(
            id = "1",
            name = "Lunch Receipt - Starbucks",
            type = DocumentType.RECEIPT,
            extractedInfo = mapOf(
                "Vendor" to "Starbucks Coffee",
                "Date" to "2024-01-15",
                "Time" to "12:30 PM",
                "Total Amount" to "$12.50",
                "Payment Method" to "Credit Card",
                "Items" to "Latte, Sandwich"
            ),
            tags = listOf("Food", "Expense", "Coffee")
        ),
        Document(
            id = "2",
            name = "Office Supply Invoice",
            type = DocumentType.INVOICE,
            extractedInfo = mapOf(
                "Supplier" to "OfficeSupply Co.",
                "Invoice Number" to "INV-2024-0876",
                "Date" to "2024-01-10",
                "Due Date" to "2024-02-10",
                "Total Amount" to "$1,245.50",
                "Payment Status" to "Unpaid"
            ),
            tags = listOf("Business", "Invoice", "Office")
        ),
        Document(
            id = "3",
            name = "Employment Contract",
            type = DocumentType.CONTRACT,
            extractedInfo = mapOf(
                "Company" to "TechCorp Inc.",
                "Employee" to "John Doe",
                "Position" to "Software Engineer",
                "Start Date" to "2024-02-01",
                "Salary" to "$85,000/year",
                "Contract Type" to "Full-time"
            ),
            tags = listOf("Employment", "Contract", "Legal")
        )
    )

    val mockForms = listOf(
        Form(
            id = "1",
            name = "Expense Report Form",
            formFields = listOf(
                FormField("Employee Name", "John Doe", true),
                FormField("Department", "Engineering", true),
                FormField("Date", "2024-01-15", true),
                FormField("Purpose", "", false),
                FormField("Amount", "$12.50", true),
                FormField("Manager Approval", "", false)
            )
        ),
        Form(
            id = "2",
            name = "Visa Application",
            formFields = listOf(
                FormField("Full Name", "John Doe", true),
                FormField("Date of Birth", "1990-05-15", true),
                FormField("Passport Number", "A12345678", true),
                FormField("Travel Purpose", "", false),
                FormField("Destination", "Japan", true),
                FormField("Duration", "2 weeks", true)
            )
        )
    )

    val mockSearchResults = SearchResult(
        documents = mockDocuments.filter { it.name.contains("receipt", ignoreCase = true) },
        forms = mockForms.filter { it.name.contains("expense", ignoreCase = true) },
        textualInfo = listOf(
            "Starbucks receipt: $12.50 on 2024-01-15",
            "Office supplies invoice: $1,245.50 due 2024-02-10"
        )
    )
} 