#ifndef StoreKitBridge_h
#define StoreKitBridge_h

#include <Foundation/Foundation.h>

// Forward declarations
@class StoreKitResult;
@class StoreKitPurchase;

// C-compatible callback function pointer types
typedef void (*StoreKitInitCallback)(StoreKitResult* result);
typedef void (*StoreKitProductsCallback)(StoreKitResult* result);
typedef void (*StoreKitPurchaseCallback)(StoreKitResult* result);
typedef void (*StoreKitPurchaseUpdateCallback)(StoreKitPurchase* purchase);

// C function declarations
#ifdef __cplusplus
extern "C" {
#endif

void storekit_initialize(StoreKitInitCallback callback);
void storekit_get_products(const char* const* productIds, int count, StoreKitProductsCallback callback);
void storekit_launch_purchase_flow(const char* productId, StoreKitPurchaseCallback callback);
void storekit_consume_purchase(const char* purchaseToken, StoreKitPurchaseCallback callback);
void storekit_acknowledge_purchase(const char* purchaseToken, StoreKitPurchaseCallback callback);
void storekit_get_purchases(StoreKitProductsCallback callback);
void storekit_restore_purchases(StoreKitProductsCallback callback);
void storekit_set_purchase_update_callback(StoreKitPurchaseUpdateCallback callback);
void storekit_disconnect(void);

#ifdef __cplusplus
}
#endif

#endif /* StoreKitBridge_h */