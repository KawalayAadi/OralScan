"""
Convert the Face_Recognition_App Siamese model (siamesemodelv4.h5) to TensorFlow Lite for OralScan.

Run this in the SAME Python environment where faceid.py already loads the model
(same TensorFlow/Keras version), so the .h5 file loads exactly as it does there.

    python convert_face_model.py path/to/siamesemodelv4.h5
    python convert_face_model.py path/to/siamesemodelv4.h5 --no-fp16   # full float32 (about 2x bigger)

Output: face_siamese.tflite. Copy it to
    OralScanApp/app/src/main/assets/models/face_siamese.tflite
and rebuild the app. It is picked up automatically (see FaceVerifierProvider.kt).
"""
import argparse
import os

import numpy as np
import tensorflow as tf
from tensorflow.keras.layers import Layer


# Identical to app/layers.py in Face_Recognition_App.
class L1Dist(Layer):
    def __init__(self, **kwargs):
        super().__init__()

    def call(self, input_embedding, validation_embedding):
        return tf.math.abs(input_embedding - validation_embedding)


def load(path):
    model = tf.keras.models.load_model(path, custom_objects={"L1Dist": L1Dist}, compile=False)
    shapes = [tuple(i.shape) for i in model.inputs]
    print("Loaded", path)
    print("  inputs :", shapes)
    print("  output :", tuple(model.outputs[0].shape))
    if len(shapes) != 2:
        raise SystemExit("Expected a Siamese model with 2 inputs (input_img, validation_img).")
    return model


def convert(model, fp16):
    size = model.inputs[0].shape[1]
    spec = [tf.TensorSpec([1, size, size, 3], tf.float32, name="input_img"),
            tf.TensorSpec([1, size, size, 3], tf.float32, name="validation_img")]

    @tf.function(input_signature=spec)
    def serve(input_img, validation_img):
        return model([input_img, validation_img], training=False)

    converter = tf.lite.TFLiteConverter.from_concrete_functions([serve.get_concrete_function()], model)
    if fp16:
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        converter.target_spec.supported_types = [tf.float16]
    return converter.convert(), size


def check(model, tflite_bytes, size):
    """Compares Keras vs TFLite scores on random pairs and on an identical pair."""
    interpreter = tf.lite.Interpreter(model_content=tflite_bytes)
    interpreter.allocate_tensors()
    inputs = interpreter.get_input_details()
    output = interpreter.get_output_details()[0]
    print("TFLite inputs:", [(d["name"], list(d["shape"])) for d in inputs])

    rng = np.random.default_rng(0)
    worst = 0.0
    for trial in range(5):
        a = rng.random((1, size, size, 3), dtype=np.float32)
        b = a if trial == 0 else rng.random((1, size, size, 3), dtype=np.float32)
        keras_score = float(model.predict([a, b], verbose=0)[0][0])
        interpreter.set_tensor(inputs[0]["index"], a)
        interpreter.set_tensor(inputs[1]["index"], b)
        interpreter.invoke()
        lite_score = float(interpreter.get_tensor(output["index"])[0][0])
        worst = max(worst, abs(keras_score - lite_score))
        print(f"  pair {trial}: keras={keras_score:.4f}  tflite={lite_score:.4f}")
    print(f"Max difference: {worst:.4f}", "(OK)" if worst < 0.02 else "(check this!)")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("h5", help="path to siamesemodelv4.h5")
    parser.add_argument("--out", default="face_siamese.tflite")
    parser.add_argument("--no-fp16", action="store_true", help="keep float32 weights (larger file)")
    args = parser.parse_args()

    model = load(args.h5)
    tflite_bytes, size = convert(model, fp16=not args.no_fp16)
    with open(args.out, "wb") as f:
        f.write(tflite_bytes)
    print(f"Wrote {args.out} ({os.path.getsize(args.out) / 1e6:.1f} MB)")
    check(model, tflite_bytes, size)


if __name__ == "__main__":
    main()
