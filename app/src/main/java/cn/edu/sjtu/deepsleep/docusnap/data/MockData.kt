package cn.edu.sjtu.deepsleep.docusnap.data

import java.util.UUID

object MockData {
    // Create IDs first so we can reference them
    private val starbucksReceiptId = UUID.randomUUID().toString()
    private val officeInvoiceId = UUID.randomUUID().toString()
    private val employmentContractId = UUID.randomUUID().toString()
    private val expenseFormId = UUID.randomUUID().toString()
    private val visaFormId = UUID.randomUUID().toString()

    val mockDocuments = listOf(
        Document(
            id = starbucksReceiptId,
            name = "Lunch Receipt - Starbucks",
            description = "A lunch receipt from Starbucks showing coffee and sandwich purchase for $12.50.",
            imageUris = listOf("mock_image_1.jpg", "mock_image_2.jpg"),
            extractedInfo = mapOf(
                "Vendor" to "Starbucks Coffee",
                "Date" to "2024-01-15",
                "Time" to "12:30 PM",
                "Total Amount" to "$12.50",
                "Payment Method" to "Credit Card",
                "Items" to "Latte, Sandwich"
            ),
            tags = listOf("Food", "Expense", "Coffee"),
            relatedFileIds = listOf(officeInvoiceId, expenseFormId) // Related to office invoice and expense form
        ),
        Document(
            id = officeInvoiceId,
            name = "Office Supply Invoice",
            description = "An invoice for office supplies totaling $1,245.50 from OfficeSupply Co.",
            imageUris = listOf("mock_image_3.jpg"),
            extractedInfo = mapOf(
                "Supplier" to "OfficeSupply Co.",
                "Invoice Number" to "INV-2024-0876",
                "Date" to "2024-01-10",
                "Due Date" to "2024-02-10",
                "Total Amount" to "$1,245.50",
                "Payment Status" to "Unpaid"
            ),
            tags = listOf("Business", "Invoice", "Office"),
            relatedFileIds = listOf(starbucksReceiptId, expenseFormId) // Related to Starbucks receipt and expense form
        ),
        Document(
            id = employmentContractId,
            name = "Employment Contract",
            description = "A full-time employment contract for John Doe as Software Engineer at TechCorp Inc.",
            imageUris = listOf("mock_image_4.jpg", "mock_image_5.jpg", "mock_image_6.jpg"),
            extractedInfo = mapOf(
                "Company" to "TechCorp Inc.",
                "Employee" to "John Doe",
                "Position" to "Software Engineer",
                "Start Date" to "2024-02-01",
                "Salary" to "$85,000/year",
                "Contract Type" to "Full-time"
            ),
            tags = listOf("Employment", "Contract", "Legal"),
            relatedFileIds = listOf(visaFormId) // Related to visa application
        )
    )

    val mockForms = listOf(
        Form(
            id = expenseFormId,
            name = "Expense Report Form",
            description = "A company expense report form for tracking business expenses and reimbursements.",
            imageUris = listOf("mock_form_1.jpg"),
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
            imageUris = listOf("mock_form_2.jpg", "mock_form_3.jpg"),
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

    // Helper functions to get related files (documents and forms)
    fun getRelatedFiles(fileId: String): List<Any> {
        val document = mockDocuments.find { it.id == fileId }
        val form = mockForms.find { it.id == fileId }
        
        val relatedIds = document?.relatedFileIds ?: form?.relatedFileIds ?: emptyList()
        
        return relatedIds.mapNotNull { relatedId ->
            mockDocuments.find { it.id == relatedId } ?: mockForms.find { it.id == relatedId }
        }
    }
    
    fun getRelatedDocuments(fileId: String): List<Document> {
        val document = mockDocuments.find { it.id == fileId }
        val form = mockForms.find { it.id == fileId }
        
        val relatedIds = document?.relatedFileIds ?: form?.relatedFileIds ?: emptyList()
        
        return relatedIds.mapNotNull { relatedId ->
            mockDocuments.find { it.id == relatedId }
        }
    }
    
    fun getRelatedForms(fileId: String): List<Form> {
        val document = mockDocuments.find { it.id == fileId }
        val form = mockForms.find { it.id == fileId }
        
        val relatedIds = document?.relatedFileIds ?: form?.relatedFileIds ?: emptyList()
        
        return relatedIds.mapNotNull { relatedId ->
            mockForms.find { it.id == relatedId }
        }
    }

    // Mock search entities with relevance scores (higher score = more relevant)
    val mockSearchEntities = listOf(
        SearchEntity.TextEntity(
            text = "Starbucks receipt: $12.50 on 2024-01-15",
            srcFileId = starbucksReceiptId,
            relevanceScore = 0.95f
        ),
        SearchEntity.DocumentEntity(
            document = mockDocuments.find { it.id == starbucksReceiptId }!!, // Starbucks receipt
            relevanceScore = 0.92f
        ),
        SearchEntity.TextEntity(
            text = "Office supplies invoice: $1,245.50 due 2024-02-10",
            srcFileId = officeInvoiceId,
            relevanceScore = 0.88f
        ),
        SearchEntity.FormEntity(
            form = mockForms.find { it.id == expenseFormId }!!, // Expense Report Form
            relevanceScore = 0.85f
        ),
        SearchEntity.DocumentEntity(
            document = mockDocuments.find { it.id == officeInvoiceId }!!, // Office Supply Invoice
            relevanceScore = 0.82f
        ),
        SearchEntity.TextEntity(
            text = "HR Department: hr@company.com",
            srcFileId = employmentContractId,
            relevanceScore = 0.75f
        ),
        SearchEntity.TextEntity(
            text = "Total Amount: $12.50",
            srcFileId = starbucksReceiptId,
            relevanceScore = 0.70f
        ),
        SearchEntity.TextEntity(
            text = "Invoice Number: INV-2024-0876",
            srcFileId = officeInvoiceId,
            relevanceScore = 0.68f
        ),
        SearchEntity.FormEntity(
            form = mockForms.find { it.id == visaFormId }!!, // Visa Application
            relevanceScore = 0.65f
        )
    )

    val mockSearchResults = SearchResult(
        entities = mockSearchEntities.sortedByDescending { 
            when (it) {
                is SearchEntity.TextEntity -> it.relevanceScore
                is SearchEntity.DocumentEntity -> it.relevanceScore
                is SearchEntity.FormEntity -> it.relevanceScore
            }
        }
    )

    // Text info generated from document and form extracted info
    // Each item represents a key-value pair that was extracted from a document's or form's extractedInfo
    val MockTextInfo = generateFrequentlyUsedTextInfoFromFiles()

    // Helper function to get text info grouped by category and usage
    fun getFrequentTextInfo(): Map<String, List<TextInfo>> {
        return MockTextInfo.groupBy { it.category }
            .mapValues { (_, textInfos) ->
                textInfos.sortedByDescending { it.usageCount }
            }
    }

    // Helper function to generate text info from document and form extracted info
    // This demonstrates how the system could automatically create text info
    private fun generateFrequentlyUsedTextInfoFromFiles(): List<TextInfo> {
        val generated = mutableListOf<TextInfo>()
        
        // Generate from documents
        mockDocuments.forEach { document ->
            document.extractedInfo.forEach { (key, value) ->
                // Determine category based on key or document type
                val category = when {
                    key.contains("Amount") || key.contains("Total") || key.contains("Price") || key.contains("Vendor") || key.contains("Invoice") -> "Recent Expenses"
                    key.contains("Company") || key.contains("Employee") || key.contains("Position") -> "Important Contacts"
                    key.contains("Date") || key.contains("Start") || key.contains("End") || key.contains("Contract") -> "Travel Information"
                    else -> "General Information"
                }
                
                generated.add(
                    TextInfo(
                        key = key,
                        value = value,
                        category = category,
                        srcFileId = document.id,
                        usageCount = (1..10).random(), // Random usage count for demo
                        lastUsed = "2024-01-${(15..20).random()}"
                    )
                )
            }
        }
        
        // Generate from forms
        mockForms.forEach { form ->
            form.extractedInfo.forEach { (key, value) ->
                // Determine category based on key or form type
                val category = when {
                    key.contains("Form Type") -> "Form Information"
                    key.contains("Company") || key.contains("Department") -> "Business Information"
                    key.contains("Country") || key.contains("Travel") -> "Travel Information"
                    else -> "General Information"
                }
                
                generated.add(
                    TextInfo(
                        key = key,
                        value = value,
                        category = category,
                        srcFileId = form.id,
                        usageCount = (1..8).random(), // Random usage count for demo
                        lastUsed = "2024-01-${(15..20).random()}"
                    )
                )
            }
        }
        
        return generated
    }
} 