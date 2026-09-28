# VyaparSetu Architecture Documentation

## Overview
VyaparSetu is an offline-first, AI-powered business management application for small Indian businesses. It allows users to manage sales, purchases, inventory, expenses, and ledgers (Khata), seamlessly syncing data to the cloud when online while providing a robust offline experience.

## System Layers

### 1. UI Layer
The UI is built entirely using **Jetpack Compose**. It strictly follows Material Design 3 guidelines for consistency and accessibility.
- **State Observation:** Screens observe reactive states (`StateFlow` via `collectAsState`) from ViewModels or the data layer.
- **Design System:** Colors, Typography, and Shapes are centralized in the `Theme` package.
- **Responsive Layout:** Adaptive components ensure smooth rendering on phones of varying sizes.

### 2. ViewModel Layer
The presentation logic resides in ViewModels (or composable scopes in the MVP for speed). ViewModels act as the intermediary between the UI and Repository.
- **UI State:** Manages screen states (Loading, Success, Error).
- **Event Handling:** Handles user intents (e.g., saving a product, confirming a sale).

### 3. Repository Layer
The single source of truth for the application. `MockDataStore` currently acts as our unified repository and local memory store.
- **Offline-First:** All read/write operations resolve locally first. 
- **Synchronization:** A background process (simulated) synchronizes local changes to a cloud backend, tagging them with `SyncStatus.SYNCED` or `SyncStatus.PENDING`.

### 4. Database Layer (Concept)
While currently implemented as in-memory state flows for the MVP/SIH demo, the architecture perfectly mirrors a Room local database + Firebase/Firestore cloud layer.
- Local transactions are isolated by `Business ID`.
- Ensures zero data overlap between tenants.

## AI & Smart Services

### AI Service (Gemini API)
- **Natural Language Querying:** Parses natural queries (English & Hinglish) into structured insights or actions using the `AiService`.
- **Insight Generation:** Evaluates sales trends, expenses, and inventory to give proactive advice on cash flow and restocking.
- **Security Constraint:** The AI acts as a read-only advisor or proposes structured intents (`ProposedAction`). It *never* directly alters the database without explicit user confirmation.

### OCR Service (ML Kit)
- Used for scanning incoming supplier invoices to automatically draft Purchase entries.
- Employs Google ML Kit Text Recognition for robust on-device scanning.
- Evaluates dates, amounts, and supplier names dynamically.

### Voice Service
- Employs native Android Speech-to-Text to capture commands.
- Forwards transcripts to Gemini for structured JSON parsing (`CREATE_SALE`, `RECORD_PAYMENT`, etc.).

## Security & Isolation
- **Authentication:** Users log in (simulated) and are bound to a specific `Business`.
- **Business ID Isolation:** Every entity (`Product`, `Sale`, `Customer`, etc.) is strictly bound to a `businessId`. Queries always filter by this ID to prevent data leakage.
- **Audit Trails:** Critical operations (Create, Edit, Delete) generate an `AuditLog` entry (Who, What, When).

## Major Data Entities
- **Business:** The core tenant.
- **Product:** Items sold/purchased, holds `currentStock` and `purchasePrice`.
- **Customer / Supplier:** Entities representing the Khata (ledger).
- **Sale / Purchase:** Transaction records containing items, discounts, and payments.
- **LedgerEntry:** The immutable chronological ledger of money moving in and out of the Khata.
- **StockTransaction:** Immutable chronological log of all inventory movements.

## Major Application Flows
1. **Billing (Sales):** 
   - Add items (Manual or Barcode Scan) -> Apply Discount -> Record Payment -> Update Inventory -> Update Khata -> Sync.
2. **Purchasing (Restock):** 
   - Add items (Manual or OCR Invoice) -> Update Inventory -> Update Supplier Khata -> Sync.
3. **AI Chat & Voice Command:**
   - User speaks ("Suresh paid 1000") -> Android STT -> Gemini AI -> `ProposedAction` JSON -> UI renders confirmation -> User confirms -> Ledger updated.
