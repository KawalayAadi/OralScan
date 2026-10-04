Put the exported on-device model here (e.g. unetpp.onnx or model.tflite).
Files here are packaged uncompressed (see androidResources.noCompress in app/build.gradle.kts).
Then point AnalyzerProvider (ml/AnalyzerProvider.kt) at the new analyzer.
