#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class In_app_purchaseIAPResult<__covariant T>, In_app_purchaseIAPResultError, In_app_purchaseIAPResultSuccess<T>, In_app_purchaseKotlinArray<T>, In_app_purchaseKotlinEnum<E>, In_app_purchaseKotlinEnumCompanion, In_app_purchaseKotlinException, In_app_purchaseKotlinIllegalStateException, In_app_purchaseKotlinNothing, In_app_purchaseKotlinRuntimeException, In_app_purchaseKotlinThrowable, In_app_purchaseKotlinUnit, In_app_purchaseProduct, In_app_purchaseProductType, In_app_purchasePurchase;

@protocol In_app_purchaseKotlinComparable, In_app_purchaseKotlinIterator, In_app_purchaseKotlinx_coroutines_coreFlow, In_app_purchaseKotlinx_coroutines_coreFlowCollector;

NS_ASSUME_NONNULL_BEGIN
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunknown-warning-option"
#pragma clang diagnostic ignored "-Wincompatible-property-type"
#pragma clang diagnostic ignored "-Wnullability"

#pragma push_macro("_Nullable_result")
#if !__has_feature(nullability_nullable_result)
#undef _Nullable_result
#define _Nullable_result _Nullable
#endif

__attribute__((swift_name("KotlinBase")))
@interface In_app_purchaseBase : NSObject
- (instancetype)init __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
+ (void)initialize __attribute__((objc_requires_super));
@end

@interface In_app_purchaseBase (In_app_purchaseBaseCopying) <NSCopying>
@end

__attribute__((swift_name("KotlinMutableSet")))
@interface In_app_purchaseMutableSet<ObjectType> : NSMutableSet<ObjectType>
@end

__attribute__((swift_name("KotlinMutableDictionary")))
@interface In_app_purchaseMutableDictionary<KeyType, ObjectType> : NSMutableDictionary<KeyType, ObjectType>
@end

@interface NSError (NSErrorIn_app_purchaseKotlinException)
@property (readonly) id _Nullable kotlinException;
@end

__attribute__((swift_name("KotlinNumber")))
@interface In_app_purchaseNumber : NSNumber
- (instancetype)initWithChar:(char)value __attribute__((unavailable));
- (instancetype)initWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
- (instancetype)initWithShort:(short)value __attribute__((unavailable));
- (instancetype)initWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
- (instancetype)initWithInt:(int)value __attribute__((unavailable));
- (instancetype)initWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
- (instancetype)initWithLong:(long)value __attribute__((unavailable));
- (instancetype)initWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
- (instancetype)initWithLongLong:(long long)value __attribute__((unavailable));
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
- (instancetype)initWithFloat:(float)value __attribute__((unavailable));
- (instancetype)initWithDouble:(double)value __attribute__((unavailable));
- (instancetype)initWithBool:(BOOL)value __attribute__((unavailable));
- (instancetype)initWithInteger:(NSInteger)value __attribute__((unavailable));
- (instancetype)initWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
+ (instancetype)numberWithChar:(char)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
+ (instancetype)numberWithShort:(short)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
+ (instancetype)numberWithInt:(int)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
+ (instancetype)numberWithLong:(long)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
+ (instancetype)numberWithLongLong:(long long)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
+ (instancetype)numberWithFloat:(float)value __attribute__((unavailable));
+ (instancetype)numberWithDouble:(double)value __attribute__((unavailable));
+ (instancetype)numberWithBool:(BOOL)value __attribute__((unavailable));
+ (instancetype)numberWithInteger:(NSInteger)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
@end

__attribute__((swift_name("KotlinByte")))
@interface In_app_purchaseByte : In_app_purchaseNumber
- (instancetype)initWithChar:(char)value;
+ (instancetype)numberWithChar:(char)value;
@end

__attribute__((swift_name("KotlinUByte")))
@interface In_app_purchaseUByte : In_app_purchaseNumber
- (instancetype)initWithUnsignedChar:(unsigned char)value;
+ (instancetype)numberWithUnsignedChar:(unsigned char)value;
@end

__attribute__((swift_name("KotlinShort")))
@interface In_app_purchaseShort : In_app_purchaseNumber
- (instancetype)initWithShort:(short)value;
+ (instancetype)numberWithShort:(short)value;
@end

__attribute__((swift_name("KotlinUShort")))
@interface In_app_purchaseUShort : In_app_purchaseNumber
- (instancetype)initWithUnsignedShort:(unsigned short)value;
+ (instancetype)numberWithUnsignedShort:(unsigned short)value;
@end

__attribute__((swift_name("KotlinInt")))
@interface In_app_purchaseInt : In_app_purchaseNumber
- (instancetype)initWithInt:(int)value;
+ (instancetype)numberWithInt:(int)value;
@end

__attribute__((swift_name("KotlinUInt")))
@interface In_app_purchaseUInt : In_app_purchaseNumber
- (instancetype)initWithUnsignedInt:(unsigned int)value;
+ (instancetype)numberWithUnsignedInt:(unsigned int)value;
@end

__attribute__((swift_name("KotlinLong")))
@interface In_app_purchaseLong : In_app_purchaseNumber
- (instancetype)initWithLongLong:(long long)value;
+ (instancetype)numberWithLongLong:(long long)value;
@end

__attribute__((swift_name("KotlinULong")))
@interface In_app_purchaseULong : In_app_purchaseNumber
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value;
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value;
@end

__attribute__((swift_name("KotlinFloat")))
@interface In_app_purchaseFloat : In_app_purchaseNumber
- (instancetype)initWithFloat:(float)value;
+ (instancetype)numberWithFloat:(float)value;
@end

__attribute__((swift_name("KotlinDouble")))
@interface In_app_purchaseDouble : In_app_purchaseNumber
- (instancetype)initWithDouble:(double)value;
+ (instancetype)numberWithDouble:(double)value;
@end

__attribute__((swift_name("KotlinBoolean")))
@interface In_app_purchaseBoolean : In_app_purchaseNumber
- (instancetype)initWithBool:(BOOL)value;
+ (instancetype)numberWithBool:(BOOL)value;
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("IAPManager")))
@interface In_app_purchaseIAPManager : In_app_purchaseBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)acknowledgePurchasePurchase:(In_app_purchasePurchase *)purchase completionHandler:(void (^)(In_app_purchaseIAPResult<In_app_purchaseKotlinUnit *> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("acknowledgePurchase(purchase:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)consumePurchasePurchase:(In_app_purchasePurchase *)purchase completionHandler:(void (^)(In_app_purchaseIAPResult<In_app_purchaseKotlinUnit *> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("consumePurchase(purchase:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)disconnectWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("disconnect(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getProductsProductIds:(NSArray<NSString *> *)productIds completionHandler:(void (^)(In_app_purchaseIAPResult<NSArray<In_app_purchaseProduct *> *> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getProducts(productIds:completionHandler:)")));
- (id<In_app_purchaseKotlinx_coroutines_coreFlow>)getPurchaseUpdates __attribute__((swift_name("getPurchaseUpdates()")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getPurchasesWithCompletionHandler:(void (^)(In_app_purchaseIAPResult<NSArray<In_app_purchasePurchase *> *> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getPurchases(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)initializeWithCompletionHandler:(void (^)(In_app_purchaseIAPResult<In_app_purchaseKotlinUnit *> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("initialize(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)purchaseProductPurchase:(In_app_purchasePurchase *)purchase completionHandler:(void (^)(In_app_purchaseIAPResult<In_app_purchasePurchase *> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("purchaseProduct(purchase:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)restorePurchasesWithCompletionHandler:(void (^)(In_app_purchaseIAPResult<NSArray<In_app_purchasePurchase *> *> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("restorePurchases(completionHandler:)")));
@end

__attribute__((swift_name("IAPResult")))
@interface In_app_purchaseIAPResult<__covariant T> : In_app_purchaseBase
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("IAPResultError")))
@interface In_app_purchaseIAPResultError : In_app_purchaseIAPResult<In_app_purchaseKotlinNothing *>
- (instancetype)initWithMessage:(NSString *)message code:(In_app_purchaseInt * _Nullable)code __attribute__((swift_name("init(message:code:)"))) __attribute__((objc_designated_initializer));
- (In_app_purchaseIAPResultError *)doCopyMessage:(NSString *)message code:(In_app_purchaseInt * _Nullable)code __attribute__((swift_name("doCopy(message:code:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) In_app_purchaseInt * _Nullable code __attribute__((swift_name("code")));
@property (readonly) NSString *message __attribute__((swift_name("message")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("IAPResultSuccess")))
@interface In_app_purchaseIAPResultSuccess<T> : In_app_purchaseIAPResult<T>
- (instancetype)initWithData:(T _Nullable)data __attribute__((swift_name("init(data:)"))) __attribute__((objc_designated_initializer));
- (In_app_purchaseIAPResultSuccess<T> *)doCopyData:(T _Nullable)data __attribute__((swift_name("doCopy(data:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) T _Nullable data __attribute__((swift_name("data")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Product")))
@interface In_app_purchaseProduct : In_app_purchaseBase
- (instancetype)initWithId:(NSString *)id title:(NSString *)title description:(NSString *)description price:(NSString *)price priceCurrencyCode:(NSString *)priceCurrencyCode priceAmountMicros:(int64_t)priceAmountMicros type:(In_app_purchaseProductType *)type __attribute__((swift_name("init(id:title:description:price:priceCurrencyCode:priceAmountMicros:type:)"))) __attribute__((objc_designated_initializer));
- (In_app_purchaseProduct *)doCopyId:(NSString *)id title:(NSString *)title description:(NSString *)description price:(NSString *)price priceCurrencyCode:(NSString *)priceCurrencyCode priceAmountMicros:(int64_t)priceAmountMicros type:(In_app_purchaseProductType *)type __attribute__((swift_name("doCopy(id:title:description:price:priceCurrencyCode:priceAmountMicros:type:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *description_ __attribute__((swift_name("description_")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *price __attribute__((swift_name("price")));
@property (readonly) int64_t priceAmountMicros __attribute__((swift_name("priceAmountMicros")));
@property (readonly) NSString *priceCurrencyCode __attribute__((swift_name("priceCurrencyCode")));
@property (readonly) NSString *title __attribute__((swift_name("title")));
@property (readonly) In_app_purchaseProductType *type __attribute__((swift_name("type")));
@end

__attribute__((swift_name("KotlinComparable")))
@protocol In_app_purchaseKotlinComparable
@required
- (int32_t)compareToOther:(id _Nullable)other __attribute__((swift_name("compareTo(other:)")));
@end

__attribute__((swift_name("KotlinEnum")))
@interface In_app_purchaseKotlinEnum<E> : In_app_purchaseBase <In_app_purchaseKotlinComparable>
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) In_app_purchaseKotlinEnumCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(E)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) int32_t ordinal __attribute__((swift_name("ordinal")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ProductType")))
@interface In_app_purchaseProductType : In_app_purchaseKotlinEnum<In_app_purchaseProductType *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) In_app_purchaseProductType *consumable __attribute__((swift_name("consumable")));
@property (class, readonly) In_app_purchaseProductType *nonConsumable __attribute__((swift_name("nonConsumable")));
@property (class, readonly) In_app_purchaseProductType *subscription __attribute__((swift_name("subscription")));
+ (In_app_purchaseKotlinArray<In_app_purchaseProductType *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<In_app_purchaseProductType *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Purchase")))
@interface In_app_purchasePurchase : In_app_purchaseBase
- (instancetype)initWithProductId:(NSString *)productId purchaseToken:(NSString *)purchaseToken orderId:(NSString *)orderId purchaseTime:(int64_t)purchaseTime isAcknowledged:(BOOL)isAcknowledged __attribute__((swift_name("init(productId:purchaseToken:orderId:purchaseTime:isAcknowledged:)"))) __attribute__((objc_designated_initializer));
- (In_app_purchasePurchase *)doCopyProductId:(NSString *)productId purchaseToken:(NSString *)purchaseToken orderId:(NSString *)orderId purchaseTime:(int64_t)purchaseTime isAcknowledged:(BOOL)isAcknowledged __attribute__((swift_name("doCopy(productId:purchaseToken:orderId:purchaseTime:isAcknowledged:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL isAcknowledged __attribute__((swift_name("isAcknowledged")));
@property (readonly) NSString *orderId __attribute__((swift_name("orderId")));
@property (readonly) NSString *productId __attribute__((swift_name("productId")));
@property (readonly) int64_t purchaseTime __attribute__((swift_name("purchaseTime")));
@property (readonly) NSString *purchaseToken __attribute__((swift_name("purchaseToken")));
@end

__attribute__((swift_name("KotlinThrowable")))
@interface In_app_purchaseKotlinThrowable : In_app_purchaseBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(In_app_purchaseKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(In_app_purchaseKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));

/**
 * @note annotations
 *   kotlin.experimental.ExperimentalNativeApi
*/
- (In_app_purchaseKotlinArray<NSString *> *)getStackTrace __attribute__((swift_name("getStackTrace()")));
- (void)printStackTrace __attribute__((swift_name("printStackTrace()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) In_app_purchaseKotlinThrowable * _Nullable cause __attribute__((swift_name("cause")));
@property (readonly) NSString * _Nullable message __attribute__((swift_name("message")));
- (NSError *)asError __attribute__((swift_name("asError()")));
@end

__attribute__((swift_name("KotlinException")))
@interface In_app_purchaseKotlinException : In_app_purchaseKotlinThrowable
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(In_app_purchaseKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(In_app_purchaseKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((swift_name("KotlinRuntimeException")))
@interface In_app_purchaseKotlinRuntimeException : In_app_purchaseKotlinException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(In_app_purchaseKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(In_app_purchaseKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((swift_name("KotlinIllegalStateException")))
@interface In_app_purchaseKotlinIllegalStateException : In_app_purchaseKotlinRuntimeException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(In_app_purchaseKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(In_app_purchaseKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
__attribute__((swift_name("KotlinCancellationException")))
@interface In_app_purchaseKotlinCancellationException : In_app_purchaseKotlinIllegalStateException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(In_app_purchaseKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(In_app_purchaseKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinUnit")))
@interface In_app_purchaseKotlinUnit : In_app_purchaseBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)unit __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) In_app_purchaseKotlinUnit *shared __attribute__((swift_name("shared")));
- (NSString *)description __attribute__((swift_name("description()")));
@end

__attribute__((swift_name("Kotlinx_coroutines_coreFlow")))
@protocol In_app_purchaseKotlinx_coroutines_coreFlow
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)collectCollector:(id<In_app_purchaseKotlinx_coroutines_coreFlowCollector>)collector completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("collect(collector:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinNothing")))
@interface In_app_purchaseKotlinNothing : In_app_purchaseBase
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinEnumCompanion")))
@interface In_app_purchaseKotlinEnumCompanion : In_app_purchaseBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) In_app_purchaseKotlinEnumCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinArray")))
@interface In_app_purchaseKotlinArray<T> : In_app_purchaseBase
+ (instancetype)arrayWithSize:(int32_t)size init:(T _Nullable (^)(In_app_purchaseInt *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (T _Nullable)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (id<In_app_purchaseKotlinIterator>)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(T _Nullable)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((swift_name("Kotlinx_coroutines_coreFlowCollector")))
@protocol In_app_purchaseKotlinx_coroutines_coreFlowCollector
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)emitValue:(id _Nullable)value completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("emit(value:completionHandler:)")));
@end

__attribute__((swift_name("KotlinIterator")))
@protocol In_app_purchaseKotlinIterator
@required
- (BOOL)hasNext __attribute__((swift_name("hasNext()")));
- (id _Nullable)next __attribute__((swift_name("next()")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
