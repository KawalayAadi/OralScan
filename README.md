# OralScan (v1)

An offline Android app. The clinician picks a patient, **verifies the patient's face**, then takes or uploads a photo of the mouth. The photo goes to an on-device model and the result is saved to that patient's history.

Face verification is integrated from [Face_Recognition_App](https://github.com/vedikathorat08/Face_Recognition_App) (a Siamese network, see *Face verification* below).
The oral lesion model is still a **placeholder**. The placeholder runs the real preprocessing (resize to 256×256 and ImageNet normalization) and reports whether the image was passed to the model successfully. It does **not** detect anything yet.

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

## Scan flow
**New scan → pick or add a patient → face verification → oral photo → review → analysis → result**, saved under the patient.
- **New patients**: enter details, then take 3–5 reference face photos (the face inside the circle guide).
- **Returning patients**: one face photo is compared against their reference photos.
- **Not verified?** Retry, or continue with the scan marked *Face NOT verified*. Skipping is recorded as well.
- **"Scan without a patient"** still exists for quick tests.

## Face verification
The Siamese model is used exactly the way `faceid.py` uses it:
- **Preprocessing**: the face crop is resized to 100×100 RGB and scaled to 0–1 (`preprocess()`).
- **Scoring**: one model call per reference photo, `model([face, reference])`, gives a score from 0 to 1.
- **Decision**: count the scores above the *match threshold* (`detection_threshold`). The patient is verified if that fraction is above the *verification threshold* (`verification_threshold`). Both default to 0.2, as in `faceid.py`, and can be tuned live in Developer options.

**To add the real model:**
1. Get `siamesemodelv4.h5`. It isn't in the GitHub repo because `.gitignore` excludes `*.h5`.
2. In the Python environment where `faceid.py` runs, run:
   `python tools/convert_face_model.py siamesemodelv4.h5`
   The script writes `face_siamese.tflite` (float16, roughly 75–80 MB) and checks that it matches the Keras model.
3. Copy the file to `app/src/main/assets/models/face_siamese.tflite` and rebuild.
   The app switches from the placeholder automatically, and Developer options shows which model is loaded.

Without the file, a **placeholder matcher** is used. It compares tiny grayscale thumbnails, so it is **not** real face recognition, and the app labels it as such.

## Testing without a model
Tap the **sliders icon** on the home screen to open the developer options:
- **Fake lesion mask**: draws a demo red region, so the overlay and the "show highlighted region" toggle can be tested.
- **Simulate model failure**: makes the placeholder report FAILED, so the error path can be tested.
- **Face thresholds**: the match and verification thresholds, plus the name of the face model that's loaded.

> The database is still a prototype. A schema change wipes the local test data instead of migrating it (`AppDatabase.kt`). Add proper migrations before storing real patient data.

## Project map
| Area | Files |
|---|---|
| Model boundary | `ml/LesionAnalyzer.kt`, `ml/AnalysisResult.kt` |
| Placeholder model | `ml/PlaceholderAnalyzer.kt` |
| Preprocessing (matches the notebooks) | `ml/ImagePreprocessor.kt`, `ml/MaskUtils.kt` |
| Which model is used | `ml/AnalyzerProvider.kt` ← the one line to change later |
| Face verification | `ml/face/` (`TfliteFaceVerifier`, `FacePreprocessor`, decision rule in `FaceVerifier.kt`) |
| Which face model is used | `ml/face/FaceVerifierProvider.kt` (automatic, based on the asset) |
| Screens | `ui/home`, `ui/patients`, `ui/face`, `ui/camera`, `ui/review`, `ui/analyze`, `ui/result`, `ui/history` |
| Storage (patients, faces, scans) | `data/` (Room database + private app files) |
| Model conversion | `tools/convert_face_model.py` |

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
