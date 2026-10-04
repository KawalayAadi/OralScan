# OralScan (v1)

An offline Android app. It takes or uploads a photo of the mouth, passes it to an on-device model and shows the result.
v1 ships with a **placeholder model**. The placeholder runs the real preprocessing (resize to 256×256 and ImageNet normalization) and reports whether the image was passed to the model successfully. It does **not** detect anything yet.

## 1. One-time setup (Windows)
1. Install **Android Studio**, latest stable: https://developer.android.com/studio. Accept the default SDK install.
2. On your phone, turn on developer mode and USB debugging:
   - Settings → About phone → tap **Build number** 7 times
   - Settings → Developer options → turn on **USB debugging**
3. Plug the phone in over USB and tap **Allow** on the "Allow USB debugging?" prompt.

You don't need BlueStacks or any other emulator.

## 2. Open and run
1. Android Studio → **Open** → select this `OralScanApp` folder.
2. Wait for the Gradle sync to finish. The first sync downloads Gradle and the libraries, so it needs internet once.
   - If it asks to create the Gradle wrapper or upgrade the Android Gradle Plugin, accept.
3. Pick your phone in the device dropdown and press **Run ▶**.

## 3. Build an installable APK
**Build → Build App Bundle(s) / APK(s) → Build APK(s)**, then click **locate** in the popup.
The file is `app/build/outputs/apk/debug/app-debug.apk`. Copy it to any Android 8.0+ phone and tap it to install. You'll need to allow "install unknown apps" first.

## Testing without a model
Tap the **sliders icon** on the home screen to open the developer options:
- **Fake lesion mask**: draws a demo red region, so the overlay and the "show highlighted region" toggle can be tested.
- **Simulate model failure**: makes the placeholder report FAILED, so the error path can be tested.

## Project map
| Area | Files |
|---|---|
| Model boundary | `ml/LesionAnalyzer.kt`, `ml/AnalysisResult.kt` |
| Placeholder model | `ml/PlaceholderAnalyzer.kt` |
| Preprocessing (matches the notebooks) | `ml/ImagePreprocessor.kt`, `ml/MaskUtils.kt` |
| Which model is used | `ml/AnalyzerProvider.kt` ← the one line to change later |
| Screens | `ui/home`, `ui/camera`, `ui/review`, `ui/analyze`, `ui/result`, `ui/history` |
| Storage (history, images) | `data/` (Room database + private app files) |

The app is **offline by design**. The manifest removes the `INTERNET` permission, so Android blocks all network access.

## Plugging in the real model later
Suppose it's the UNet++ from `UNet_Oral_Cancer.ipynb` (`smp.UnetPlusPlus("resnet34", classes=1)`, 256×256 input):
1. In the notebook, export it:
   ```python
   model.eval()
   torch.onnx.export(model.cpu(), torch.randn(1, 3, 256, 256), "unetpp.onnx",
                     input_names=["image"], output_names=["logits"], opset_version=17)
   ```
2. Copy `unetpp.onnx` into `app/src/main/assets/models/`.
3. Add `implementation("com.microsoft.onnxruntime:onnxruntime-android:<latest>")` to `app/build.gradle.kts`.
4. Write `OnnxAnalyzer : LesionAnalyzer`:
   - input: `ImagePreprocessor.toNchwTensor(bitmap)`
   - output: `MaskUtils.sigmoid(logits)` → `MaskUtils.toOverlay(...)` for the mask, and `MaskUtils.coverage(...)` to decide "lesion detected"
5. Return it from `AnalyzerProvider.create(...)`. None of the UI needs to change.

The same `LesionAnalyzer` interface also fits Mask R-CNN (fill `regions`) or a vision LLM (fill `label`).
