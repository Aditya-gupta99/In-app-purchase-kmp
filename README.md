![](https://miro.medium.com/v2/resize:fit:1400/format:webp/1*w4ntdpQp1wBYpXSGUwO3Fw.png)
# In App Purchase Kmp Library

A Kotlin Multiplatform library that provides unified in-app purchase functionality for Android and iOS platforms. This library abstracts Google Play Billing (Android) and StoreKit 2 (iOS) into a single, easy-to-use API.

## Features

- Cross-platform in-app purchase support for Android and iOS
- Google Play Billing v6 integration for Android
- StoreKit 2 support for iOS  
- Suspend function support for async operations
- Flow-based purchase updates
- Product querying and purchase management
- Purchase acknowledgment and consumption
- Purchase restoration support
- Dependency injection ready (Koin integration included)

## Installation

Add the following dependency to your

```kotlin
    implementation("io.github.aditya-gupta99:inAppPurchase-kmp:1.0.12")
```

## Setup

### Dependency Injection Configuration

Create `IAPManager` object to inject into activity. 

**Common Payment Module:**
```kotlin
val PaymentModule = module {

    singleOf(::IAPManager)

    singleOf(::InAppPurchaseProvider)

}
```

### Android Setup

For Android, you need to provide both context and activity to the `IAPManager`. Set this up in your main activity:

```kotlin
class MainActivity : ComponentActivity() {
    private val iapManager: IAPManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        iapManager.setContext(context = this, activity = this)
        setContent {
            MainApp()
        }
    }
}
```


## Usage

### Basic Implementation
```kotlin
class InAppPurchaseProvider(
    private val inAppPurchaseManager: IAPManager
) : PaymentProvider {

    override suspend fun initialize() {
        inAppPurchaseManager.initialize()
    }

    override suspend fun makePayment() {
        val getProductState = inAppPurchaseManager.getProducts(
            listOf("your_product_id")
        )

        when (getProductState) {
            is IAPResult.Error -> {
                // Handle error
            }
            is IAPResult.Success -> {
                getProductState.data.getOrNull(0)?.let { product ->
                    inAppPurchaseManager.launchPurchaseFlow(product)
                }
            }
        }
    }
}
```

### Detailed API Usage

The following sections provide detailed examples of how to use each method from the `IAPManager` class that you saw in the basic implementation above.

#### Initialize the Library

```kotlin
val result = iapManager.initialize()
when (result) {
    is IAPResult.Success -> {
        // Library initialized successfully
    }
    is IAPResult.Error -> {
        // Handle initialization error
    }
}
```

### Query Available Products
```kotlin
val productsResult = iapManager.getProducts(listOf("product_id_1", "product_id_2"))
when (productsResult) {
    is IAPResult.Success -> {
        val products = productsResult.data
        // Display products to user
    }
    is IAPResult.Error -> {
        // Handle error
    }
}
```

### Launch Purchase Flow
```kotlin
val purchaseResult = iapManager.launchPurchaseFlow(selectedProduct)
when (purchaseResult) {
    is IAPResult.Success -> {
        // Purchase flow launched successfully
    }
    is IAPResult.Error -> {
        // Handle error
    }
}
```

### Monitor Purchase Updates
```kotlin
iapManager.getPurchaseUpdates().collect { purchase ->
    // Handle new purchase
    // Verify purchase on your server
    // Acknowledge or consume the purchase
}
```

### Acknowledge Purchase

For non-consumable products:

```kotlin
val acknowledgeResult = iapManager.acknowledgePurchase(purchase)
when (acknowledgeResult) {
    is IAPResult.Success -> {
        // Purchase acknowledged
    }
    is IAPResult.Error -> {
        // Handle error
    }
}
```

### Consume Purchase
For consumable products:

```kotlin
val consumeResult = iapManager.consumePurchase(purchase)
when (consumeResult) {
    is IAPResult.Success -> {
        // Purchase consumed, user can buy again
    }
    is IAPResult.Error -> {
        // Handle error
    }
}
```

### Get Existing Purchases
```kotlin
val purchasesResult = iapManager.getPurchases()
when (purchasesResult) {
    is IAPResult.Success -> {
        val purchases = purchasesResult.data
        // Process existing purchases
    }
    is IAPResult.Error -> {
        // Handle error
    }
}
```

### Restore Purchases
```kotlin
val restoreResult = iapManager.restorePurchases()
when (restoreResult) {
    is IAPResult.Success -> {
        val restoredPurchases = restoreResult.data
        // Handle restored purchases
    }
    is IAPResult.Error -> {
        // Handle error
    }
}
```

## 📐 Interface Overview

The `IAPManager` is the core interface of the library that provides all necessary functions to handle in-app purchases in a cross-platform manner.

| Method | Return Type | Description |
|--------|-------------|-------------|
| `initialize()` | `IAPResult<Unit>` | Initializes the billing/store session. Must be called before making purchases. |
| `getProducts(productIds: List<String>)` | `IAPResult<List<Product>>` | Retrieves details for a list of product IDs from the store. |
| `launchPurchaseFlow(product: Product)` | `IAPResult<Unit>` | Launches the native purchase UI for the given product. |
| `getPurchaseUpdates()` | `Flow<Purchase>` | A Kotlin Flow stream that emits purchase events (new, updated, restored). |
| `acknowledgePurchase(purchase: Purchase)` | `IAPResult<Unit>` | Acknowledges a non-consumable product purchase (Google Play). |
| `consumePurchase(purchase: Purchase)` | `IAPResult<Unit>` | Consumes a consumable purchase, allowing it to be purchased again. |
| `getPurchases()` | `IAPResult<List<Purchase>>` | Returns a list of active or already processed purchases. |
| `restorePurchases()` | `IAPResult<List<Purchase>>` | Restores previously made purchases (especially for iOS). |

> ⚠️ Each method returns an `IAPResult`, which can be either `Success` or `Error`, making it safe and predictable to handle results.

## 🙋‍♂️ Support

Need help with integration or found an issue?

- 📘 **Documentation**: Refer to the usage and API sections above for examples.
- 🐞 **Bug Reports**: [Submit an issue](https://github.com/Aditya-gupta99/In-app-purchase-kmp/issues) on GitHub with as much detail as possible.
- 💡 **Feature Requests**: Have an idea to improve the library? We’re open to suggestions—feel free to open a discussion or issue.
- 🤝 **Contributions**: PRs are welcome! See the [Contributing](#-contributing) section for more info.

> If you're using this in a commercial project and need priority support, feel free to reach out via GitHub or email.
