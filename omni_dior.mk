$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base.mk)

PRODUCT_COPY_FILES += \
    device/xiaomi/dior/kernel:kernel \
    device/xiaomi/dior/dt.img:dt.img

PRODUCT_DEVICE := dior
PRODUCT_NAME := omni_dior
PRODUCT_BRAND := Xiaomi
PRODUCT_MODEL := HM NOTE1 LTE
PRODUCT_MANUFACTURER := Xiaomi
