# Biome recognition and player feedback

## Current Android baseline

- BiomeFeatureExtractor builds a compact 24-value RGB histogram from a gameplay frame.
- BiomeClassifier compares that descriptor with **player-confirmed gameplay samples only**.
- BiomeRecognitionEngine records each prediction as PENDING.
- A caller must present a correction/confirmation UI and call confirm() with the player's selected biome.
- Only rows changed from PENDING to CONFIRMED are loaded for future classification.
- Feedback is stored locally in Room. The current implementation stores feature vectors, not screenshots or player identifiers.

## Wiki reference policy

Wiki images are visual reference material and must not be treated as labeled gameplay screenshots. They are not used as training samples by this baseline. A separate asset-import step is still required to package the Wiki reference index/images in Android. The Android classifier currently improves only from player-confirmed gameplay examples.

The 11 normal Matrix biome labels are supported. Cave, Grave and Swamp are included as review-only labels until their Matrix appearance is independently verified.

## Limitations

The RGB histogram is a lightweight baseline, not a trained visual model. It ignores geometry and spatial layout, so different biomes can have similar scores. The similarity score is not a calibrated probability. Predictions always require player review; low scores return null rather than forcing a label.

No automatic online training, cloud upload, screen capture, or touch automation is performed by this component. Screen capture integration must use Android's explicit MediaProjection consent flow in a separate step.

## Database migration

Room schema version 2 adds biome_feedback and migrates existing version-1 databases without deleting macro records.
