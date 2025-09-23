#import <Foundation/Foundation.h>

@protocol StoreKitBridgeDelegate <NSObject>
- (void)onPurchaseUpdateWithProductId:(NSString *)productId
        purchaseToken:(NSString *)purchaseToken
        orderId:(NSString *)orderId
        purchaseTime:(int64_t)purchaseTime;
- (void)onPurchaseErrorWithProductId:(NSString *)productId
        error:(NSString *)error;
@end

@interface StoreKitBridge : NSObject

@property (nonatomic, weak) id<StoreKitBridgeDelegate> delegate;

+ (instancetype)shared;

- (void)initializeWithCompletion:(void (^)(BOOL success, NSString * _Nullable error))completion;

- (void)queryProductsWithProductIds:(NSArray<NSString *> *)productIds
        completion:(void (^)(NSDictionary<NSString *, NSDictionary *> *products,
        NSArray<NSString *> *invalidIds,
NSString * _Nullable error))completion;

- (void)launchPurchaseWithProductId:(NSString *)productId
        completion:(void (^)(BOOL success, NSString * _Nullable error))completion;

- (void)restorePurchasesWithCompletion:(void (^)(NSDictionary<NSString *, NSDictionary *> *purchases,
        NSString * _Nullable error))completion;

- (void)disconnect;

@end