// Copyright 2013 The Flutter Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.camera.features.autofocus;

import android.annotation.SuppressLint;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import androidx.annotation.NonNull;
import io.flutter.plugins.camera.CameraProperties;
import io.flutter.plugins.camera.features.CameraFeature;

/** Controls the auto focus configuration on the {@see anddroid.hardware.camera2} API. */
public class AutoFocusFeature extends CameraFeature<FocusMode> {
  @NonNull private FocusMode currentSetting = FocusMode.auto;
  private final boolean isFixedFocus;

  // When switching recording modes this feature is re-created with the appropriate setting here.
  private final boolean recordingVideo;

  /**
   * Creates a new instance of the {@see AutoFocusFeature}.
   *
   * @param cameraProperties Collection of the characteristics for the current camera device.
   * @param recordingVideo Indicates whether the camera is currently recording video.
   */
  public AutoFocusFeature(@NonNull CameraProperties cameraProperties, boolean recordingVideo) {
    super(cameraProperties);
    this.recordingVideo = recordingVideo;
    final Float minFocusDistance = cameraProperties.getLensInfoMinimumFocusDistance();
    this.isFixedFocus = minFocusDistance == null || minFocusDistance == 0;
    if (isFixedFocus) {
      currentSetting = FocusMode.fixed;
    }
  }

  @NonNull
  @Override
  public String getDebugName() {
    return "AutoFocusFeature";
  }

  @NonNull
  @SuppressLint("KotlinPropertyAccess")
  @Override
  public FocusMode getValue() {
    return isFixedFocus ? FocusMode.fixed : currentSetting;
  }

  @Override
  public void setValue(@NonNull FocusMode value) {
    if (isFixedFocus) {
      this.currentSetting = FocusMode.fixed;
      return;
    }
    this.currentSetting = value;
  }

  public boolean isFixedFocus() {
    return isFixedFocus;
  }

  @Override
  public boolean checkIsSupported() {
    if (isFixedFocus) {
      return false;
    }
    int[] modes = cameraProperties.getControlAutoFocusAvailableModes();

    return !(modes.length == 0
        || (modes.length == 1 && modes[0] == CameraCharacteristics.CONTROL_AF_MODE_OFF));
  }

  @Override
  public void updateBuilder(@NonNull CaptureRequest.Builder requestBuilder) {
    if (isFixedFocus || currentSetting == FocusMode.fixed) {
      requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
      return;
    }
    if (!checkIsSupported()) {
      return;
    }

    switch (currentSetting) {
      case locked:
        // When locking the auto-focus the camera device should do a one-time focus and afterwards
        // set the auto-focus to idle. This is accomplished by setting the CONTROL_AF_MODE to
        // CONTROL_AF_MODE_AUTO.
        requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        break;
      case auto:
        requestBuilder.set(
            CaptureRequest.CONTROL_AF_MODE,
            recordingVideo
                ? CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                : CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        break;
      default:
        break;
    }
  }
}
