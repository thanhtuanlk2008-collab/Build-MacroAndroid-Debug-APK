# Soul Knight Matrix AI — Computer Vision Pipeline

## Implemented pipeline core

Bitmap frame -> preprocessing -> entity detector -> game-state estimator -> VisionResult.

- FramePreprocessor validates frames and scales large images down to a bounded size.
- EntityDetector is the replaceable detector interface.
- TemplateMatchingDetector is a lightweight grayscale template-matching baseline.
- GameStateEstimator maps detections to a coarse phase (combat, boss fight, reward, exploration, unknown).
- MatrixVisionPipeline orchestrates the stages and measures processing time.

## Integration example

    val detector = TemplateMatchingDetector(
        templates = listOf(
            VisionTemplate("boss_reference", EntityType.BOSS, bossCrop),
            VisionTemplate("enemy_reference", EntityType.ENEMY, enemyCrop)
        )
    )
    val pipeline = MatrixVisionPipeline(
        FramePreprocessor(maxDimension = 640),
        detector,
        GameStateEstimator()
    )
    val result = pipeline.process(screenBitmap)

The caller owns the source Bitmap and must not recycle it during processing.

## Important limitations

This is a functional pipeline core, not a trained Soul Knight recognition model.
Template matching can fail when sprites animate, change scale, overlap effects,
or appear over different backgrounds. No enemy/boss/HUD accuracy is claimed.
HUD values remain unknown unless supplied by a separate parser. Real recognition
requires representative gameplay screenshots and labeled templates or a trained
TFLite model with measured precision/recall.

The pipeline does not silently capture the screen. A separate MediaProjection
adapter must request visible user consent and use a correctly configured
foreground service. Frames from that adapter can then be passed to process().

## Next stages

1. Add the user-consented MediaProjection capture adapter.
2. Add HUD region parsing for health, energy and coins, with validation.
3. Collect labeled screenshots and train/evaluate a mobile object detector.
4. Add temporal tracking, confidence smoothing and scene-transition detection.
5. Add biome and room classification after collecting labeled examples.
6. Connect GameState to Decision Engine as recommendations first; user retains
   control of touch actions.

For an initial prototype, process around 5–10 frames/second off the main thread
and measure latency and memory on the target device before increasing the rate.
