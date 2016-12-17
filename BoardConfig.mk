# Architecture
TARGET_ARCH := arm
TARGET_ARCH_VARIANT := armv7-a-neon
TARGET_CPU_ABI := armeabi-v7a
TARGET_CPU_ABI2 := armeabi
TARGET_CPU_SMP := true
TARGET_CPU_VARIANT := krait

# Board
TARGET_BOARD_PLATFORM := msm8226
TARGET_BOOTLOADER_BOARD_NAME := MSM8226
TARGET_NO_BOOTLOADER := true

# Kernel
TARGET_PREBUILT_KERNEL := device/xiaomi/dior/kernel
TARGET_CUSTOM_KERNEL_HEADERS := device/xiaomi/dior/include
BOARD_KERNEL_CMDLINE := console=ttyHSL0,115200,n8 androidboot.console=ttyHSL0 androidboot.bootdevice=msm_sdcc.1 androidboot.hardware=dior
BOARD_KERNEL_PAGESIZE := 2048
BOARD_MKBOOTIMG_ARGS := --ramdisk_offset 0x01000000 --dt device/xiaomi/dior/dt.img

# Keymaster
TARGET_KEYMASTER_WAIT_FOR_QSEE := true

# Recovery
TARGET_USERIMAGES_USE_EXT4 := true
TARGET_USERIMAGES_USE_F2FS := true
BOARD_SUPPRESS_SECURE_ERASE := true

# TWRP-Specific
TW_THEME := portrait_hdpi
TW_INCLUDE_CRYPTO := true
TW_TARGET_USES_QCOM_BSP := true
RECOVERY_SDCARD_ON_DATA := true
