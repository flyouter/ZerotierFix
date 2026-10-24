# 扫码功能：升级备忘

> 只记录日后升级依赖/工具链时的注意事项，实现细节见代码。

## 当前版本

| 组件 | 版本 |
|---|---|
| Kotlin（AGP 内置） | 2.4.21 |
| KSP | 2.3.12 |
| Compose 编译器插件 | 2.4.21 |
| CameraX（core / camera2 / lifecycle / view） | 1.6.2 |
| zxing-cpp | 3.1.1 |

## 升级要点

1. **Kotlin 三件套**：Kotlin 版本 = Compose 编译器插件版本；KSP 用独立版本号，需支持该 Kotlin。
   三者都在根 `build.gradle` 的 buildscript classpath 显式声明，用于覆盖 AGP 内置 Kotlin。
   若软约束失效（仍按旧 Kotlin 编译、报 metadata 不兼容），备选方案是
   `android.builtInKotlin=false` + `android.newDsl=false`，app 模块显式 apply `kotlin.android`。
2. **zxing-cpp**：3.1.x 需要 Kotlin ≥ 2.3（metadata 2.3.0）。升新版本前先确认其 Kotlin metadata，
   必要时同步升 Kotlin；无法升工具链就回退 3.0.2（metadata 2.2.0）。
3. **CameraX 预览必须继续用 `camera-view` 的 `PreviewView`**，不要引入 `camera-compose`。
   `CameraXViewfinder` 在部分 legacy HAL 设备（Android 13）上黑屏：`DataSpace override not allowed for format 0x21`，
   会话配置 5 秒超时；1.5.2 / 1.6.2 均复现，是 Viewfinder 的预览通道问题，不是版本问题。
   将来若迁回，只改 `CameraBarcodePreview.kt` 的视图层，其余不用动。
4. **权限**：只用系统自带弹窗（`RequestPermission`），不要自绘权限对话框；永久拒绝时跳应用设置。
5. **相册选图**：图片解码依赖 `ImageDecoder`（minSdk 29，自动处理 EXIF 方向）与系统
   Photo Picker（`PickVisualMedia`，无需存储权限）；不引入 exifinterface 或图片库。

## 升级后验证

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep -E 'camera-|zxing|kotlin-stdlib'
```

真机手测：扫码页能出预览；扫 `https://joinzt.com/addnetwork?nwid=673017c788cb3f7d&v=1` 能回填；
横竖屏、退后台再回、反复进出扫码页不黑屏。
