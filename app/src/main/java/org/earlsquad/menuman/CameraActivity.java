package org.earlsquad.menuman;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.abbyy.mobile.rtr.Engine;
import com.abbyy.mobile.rtr.ITextCaptureService;
import com.abbyy.mobile.rtr.Language;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class CameraActivity extends Activity {

  ///////////////////////////////////////////////////////////////////////////////
  // Some application settings that can be changed to modify application behavior:
  // The camera zoom. Optically zooming with a good camera often improves results
  // even at close range and it might be required at longer ranges.
  private static final int cameraZoom = 2;
  // The default behavior in this sample is to start recognition when application is started or
  // resumed. You can turn off this behavior or remove it completely to simplify the application
  private static final boolean startRecognitionOnAppStart = true;
  // Area of interest specified through margin sizes relative to camera preview size
  private static final int areaOfInterestMargin_PercentOfWidth = 4;
  private static final int areaOfInterestMargin_PercentOfHeight = 25;
  // Camera permission request code for Android 6.0 and higher
  private static final int CAMERA_PERMISSION_REQUEST_CODE = 42;
  ///////////////////////////////////////////////////////////////////////////////
  // Text displayed on start button
  private static final String BUTTON_TEXT_START = "Start";
  private static final String BUTTON_TEXT_STOP = "Pause";
  private static final String BUTTON_TEXT_STARTING = "Starting...";
  // A subset of available languages shown in the UI. See all available languages in Language enum.
  // To show all languages in the UI you can substitute the list below with:
  // Language[] languages = Language.values();
  private Language[] languages = {
    Language.ChineseSimplified,
    Language.ChineseTraditional,
    Language.English,
    Language.French,
    Language.German,
    Language.Italian,
    Language.Japanese,
    Language.Korean,
    Language.Polish,
    Language.PortugueseBrazilian,
    Language.Russian,
    Language.Spanish
  };
  private OCRService ocrService;
  // The camera and the preview surface
  private Camera camera;
  private SurfaceViewWithOverlay surfaceViewWithOverlay;
  private SurfaceHolder previewSurfaceHolder;
  // Actual preview size and orientation
  private Camera.Size cameraPreviewSize;
  private int orientation;
  // Auxiliary variables
  private boolean inPreview = false; // Camera preview is started
  private boolean stableResultHasBeenReached; // Stable result has been reached
  private boolean
      startRecognitionWhenReady; // Start recognition next time when ready (and reset this flag)
  private Handler handler = new Handler(); // Posting some delayed actions;
  private boolean cameraPermissionRequested =
      false; // Camera permission request has been sent to user (Android 6+)
  // UI components
  private Button startButton; // The start button
  private TextView warningTextView; // Show warnings from recognizer
  private TextView errorTextView; // Show errors from recognizer
  // To communicate with the Text Capture Service we will need this callback:
  private ITextCaptureService.Callback textCaptureCallback =
      new ITextCaptureService.Callback() {

        @Override
        public void onRequestLatestFrame(byte[] buffer) {
          // The service asks to fill the buffer with image data for the latest frame in NV21
          // format.
          // Delegate this task to the camera. When the buffer is filled we will receive
          // Camera.PreviewCallback.onPreviewFrame (see below)
          camera.addCallbackBuffer(buffer);
        }

        @Override
        public void onFrameProcessed(
            ITextCaptureService.TextLine[] lines,
            ITextCaptureService.ResultStabilityStatus resultStatus,
            ITextCaptureService.Warning warning) {
          // Frame has been processed. Here we process recognition results. In this sample we
          // stop when we get stable result. This callback may continue being called for some time
          // even after the service has been stopped while the calls queued to this thread (UI
          // thread)
          // are being processed. Just ignore these calls:
          if (!stableResultHasBeenReached) {
            if (resultStatus.ordinal() >= 3) {
              // The result is stable enough to show something to the user
              surfaceViewWithOverlay.setLines(lines, resultStatus);
            } else {
              // The result is not stable. Show nothing
              surfaceViewWithOverlay.setLines(
                  null, ITextCaptureService.ResultStabilityStatus.NotReady);
            }

            // Show the warning from the service if any. The warnings are intended for the user
            // to take some action (zooming in, checking recognition language, etc.)
            warningTextView.setText(warning != null ? warning.name() : "");

            if (resultStatus == ITextCaptureService.ResultStabilityStatus.Stable) {
              // Stable result has been reached.
              // TODO: search for menu item
              /*
              // TODO: move menuAPI to a field
              // This is how you use MenuAPI
              String apiKey = getString(R.string.customsearch_api_key);
              String cx = getString(R.string.customsearch_cx);
              MenuAPI menuAPI = new MenuAPI(apiKey, cx);
              menuAPI.searchThumbnail("spaghetti");  // should return url of thumbnail
              */
            }
          }
        }

        @Override
        public void onError(Exception e) {
          // An error occurred while processing. Log it. Processing will continue
          Log.e(getString(R.string.app_name), "Error: " + e.getMessage());
          if (BuildConfig.DEBUG) {
            // Make the error easily visible to the developer
            String message = e.getMessage();
            if (message == null) {
              message = "Unspecified error while creating the service. See logcat for details.";
            } else {
              if (message.contains("ChineseJapanese.rom")) {
                message =
                    "Chinese, Japanese and Korean are available in EXTENDED version only. Contact us for more information.";
              }
              if (message.contains("Russian.edc")) {
                message =
                    "Cyrillic script languages are available in EXTENDED version only. Contact us for more information.";
              } else if (message.contains(".trdic")) {
                message =
                    "Translation is available in EXTENDED version only. Contact us for more information.";
              }
            }
            errorTextView.setText(message);
          }
        }
      };

  // This callback will be used to obtain frames from the camera
  private Camera.PreviewCallback cameraPreviewCallback =
      new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
          // The buffer that we have given to the camera in
          // ITextCaptureService.Callback.onRequestLatestFrame
          // above have been filled. Send it back to the Text Capture Service
          ocrService.submitRequestedFrame(data);
        }
      };
  // Simple autofocus callback
  private Camera.AutoFocusCallback simpleCameraAutoFocusCallback =
      new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
          onAutoFocusFinished(success, camera);
        }
      };
  // Start recognition when autofocus completes (used when continuous autofocus is not enabled)
  private Camera.AutoFocusCallback startRecognitionCameraAutoFocusCallback =
      new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
          onAutoFocusFinished(success, camera);
          startRecognition();
        }
      };
  // Enable 'Start' button and switching to continuous focus mode (if possible) when autofocus
  // completes
  private Camera.AutoFocusCallback finishCameraInitialisationAutoFocusCallback =
      new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
          onAutoFocusFinished(success, camera);
          startButton.setText(BUTTON_TEXT_START);
          startButton.setEnabled(true);
          if (startRecognitionWhenReady) {
            startRecognition();
            startRecognitionWhenReady = false;
          }
        }
      };
  // This callback is used to configure preview surface for the camera
  private SurfaceHolder.Callback surfaceCallback =
      new SurfaceHolder.Callback() {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
          // When surface is created, store the holder
          previewSurfaceHolder = holder;
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
          // When surface is changed (or created), attach it to the camera, configure camera and
          // start preview
          if (camera != null) {
            setCameraPreviewDisplayAndStartPreview();
          }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
          // When surface is destroyed, clear previewSurfaceHolder
          previewSurfaceHolder = null;
        }
      };
  // Autofocus by tap
  private View.OnClickListener clickListener =
      new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          // if BUTTON_TEXT_STARTING autofocus is already in progress, it is incorrect to interrupt
          // it
          if (!startButton.getText().equals(BUTTON_TEXT_STARTING)) {
            autoFocus(simpleCameraAutoFocusCallback);
          }
        }
      };

  private void onAutoFocusFinished(boolean success, Camera camera) {
    if (isContinuousVideoFocusModeEnabled(camera)) {
      setCameraFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
    } else {
      if (!success) {
        autoFocus(simpleCameraAutoFocusCallback);
      }
    }
  }

  // Start autofocus (used when continuous autofocus is disabled)
  private void autoFocus(Camera.AutoFocusCallback callback) {
    if (camera != null) {
      try {
        setCameraFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        camera.autoFocus(callback);
      } catch (Exception e) {
        Log.e(getString(R.string.app_name), "Error: " + e.getMessage());
      }
    }
  }

  // Checks that FOCUS_MODE_CONTINUOUS_VIDEO supported
  private boolean isContinuousVideoFocusModeEnabled(Camera camera) {
    return camera
        .getParameters()
        .getSupportedFocusModes()
        .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
  }

  // Sets camera focus mode and focus area
  private void setCameraFocusMode(String mode) {
    // Camera sees it as rotated 90 degrees, so there's some confusion with what is width and what
    // is height)
    int width = 0;
    int height = 0;
    int halfCoordinates = 1000;
    int lengthCoordinates = 2000;
    Rect area = surfaceViewWithOverlay.getAreaOfInterest();
    switch (orientation) {
      case 0:
      case 180:
        height = cameraPreviewSize.height;
        width = cameraPreviewSize.width;
        break;
      case 90:
      case 270:
        //noinspection SuspiciousNameCombination
        width = cameraPreviewSize.height;
        //noinspection SuspiciousNameCombination
        height = cameraPreviewSize.width;
        break;
    }

    camera.cancelAutoFocus();
    Camera.Parameters parameters = camera.getParameters();
    // Set focus and metering area equal to the area of interest. This action is essential because
    // by defaults camera
    // focuses on the center of the frame, while the area of interest in this sample application is
    // at the top
    List<Camera.Area> focusAreas = new ArrayList<>();
    Rect areasRect;

    switch (orientation) {
      case 0:
        areasRect =
            new Rect(
                -halfCoordinates + area.left * lengthCoordinates / width,
                -halfCoordinates + area.top * lengthCoordinates / height,
                -halfCoordinates + lengthCoordinates * area.right / width,
                -halfCoordinates + lengthCoordinates * area.bottom / height);
        break;
      case 180:
        areasRect =
            new Rect(
                halfCoordinates - area.right * lengthCoordinates / width,
                halfCoordinates - area.bottom * lengthCoordinates / height,
                halfCoordinates - lengthCoordinates * area.left / width,
                halfCoordinates - lengthCoordinates * area.top / height);
        break;
      case 90:
        areasRect =
            new Rect(
                -halfCoordinates + area.top * lengthCoordinates / height,
                halfCoordinates - area.right * lengthCoordinates / width,
                -halfCoordinates + lengthCoordinates * area.bottom / height,
                halfCoordinates - lengthCoordinates * area.left / width);
        break;
      case 270:
        areasRect =
            new Rect(
                halfCoordinates - area.bottom * lengthCoordinates / height,
                -halfCoordinates + area.left * lengthCoordinates / width,
                halfCoordinates - lengthCoordinates * area.top / height,
                -halfCoordinates + lengthCoordinates * area.right / width);
        break;
      default:
        throw new IllegalArgumentException();
    }

    focusAreas.add(new Camera.Area(areasRect, 800));
    if (parameters.getMaxNumFocusAreas() >= focusAreas.size()) {
      parameters.setFocusAreas(focusAreas);
    }
    if (parameters.getMaxNumMeteringAreas() >= focusAreas.size()) {
      parameters.setMeteringAreas(focusAreas);
    }

    parameters.setFocusMode(mode);

    // Commit the camera parameters
    camera.setParameters(parameters);
  }

  // Attach the camera to the surface holder, configure the camera and start preview
  private void setCameraPreviewDisplayAndStartPreview() {
    try {
      camera.setPreviewDisplay(previewSurfaceHolder);
    } catch (Throwable t) {
      Log.e(getString(R.string.app_name), "Exception in setPreviewDisplay()", t);
    }
    configureCameraAndStartPreview(camera);
  }

  // Stop preview and release the camera
  private void stopPreviewAndReleaseCamera() {
    if (camera != null) {
      camera.setPreviewCallbackWithBuffer(null);
      stopPreview();
      camera.release();
      camera = null;
    }
  }

  // Stop preview if it is running
  private void stopPreview() {
    if (inPreview) {
      camera.stopPreview();
      inPreview = false;
    }
  }

  // Show error on startup if any
  private void showStartupError(String message) {
    new AlertDialog.Builder(this)
        .setTitle("ABBYY RTR SDK")
        .setMessage(message)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .show()
        .setOnDismissListener(
            new DialogInterface.OnDismissListener() {
              @Override
              public void onDismiss(DialogInterface dialog) {
                CameraActivity.this.finish();
              }
            });
  }

  // Load ABBYY RTR SDK engine and configure the text capture service
  private boolean createTextCaptureService() {
    // Initialize the engine and text capture service
    try {
      // The 'Abbyy RTR SDK Engine' and 'Text Capture Service' to be used in this sample application
      ocrService = OCRService.create(this, textCaptureCallback);

      return true;
    } catch (java.io.IOException e) {
      // Troubleshooting for the developer
      Log.e(getString(R.string.app_name), "Error loading ABBYY RTR SDK:", e);
      showStartupError(
          "Could not load some required resource files. Make sure to configure "
              + "'assets' directory in your application and specify correct 'license file name'. See logcat for details.");
    } catch (Engine.LicenseException e) {
      // Troubleshooting for the developer
      Log.e(getString(R.string.app_name), "Error loading ABBYY RTR SDK:", e);
      showStartupError(
          "License not valid. Make sure you have a valid license file in the "
              + "'assets' directory and specify correct 'license file name' and 'application id'. See logcat for details.");
    } catch (Throwable e) {
      // Troubleshooting for the developer
      Log.e(getString(R.string.app_name), "Error loading ABBYY RTR SDK:", e);
      showStartupError("Unspecified error while loading the engine. See logcat for details.");
    }

    return false;
  }

  // Start recognition
  private void startRecognition() {
    // Do not switch off the screen while text capture service is running
    previewSurfaceHolder.setKeepScreenOn(true);
    // Get area of interest (in coordinates of preview frames)
    Rect areaOfInterest = new Rect(surfaceViewWithOverlay.getAreaOfInterest());
    // Clear error message
    errorTextView.setText("");
    // Start the service
    ocrService.start(
        cameraPreviewSize.width, cameraPreviewSize.height, orientation, areaOfInterest);
    // Change the text on the start button to 'Stop'
    startButton.setText(BUTTON_TEXT_STOP);
    startButton.setEnabled(true);
  }

  // Stop recognition
  private void stopRecognition() {
    // Disable the 'Stop' button
    startButton.setEnabled(false);

    // Stop the service asynchronously to make application more responsive. Stopping can take some
    // time
    // waiting for all processing threads to stop
    new AsyncTask<Void, Void, Void>() {
      protected Void doInBackground(Void... params) {
        ocrService.stop();
        return null;
      }

      protected void onPostExecute(Void result) {
        if (previewSurfaceHolder != null) {
          // Restore normal power saving behaviour
          previewSurfaceHolder.setKeepScreenOn(false);
        }
        // Change the text on the stop button back to 'Start'
        startButton.setText(BUTTON_TEXT_START);
        startButton.setEnabled(true);
      }
    }.execute();
  }

  // Clear recognition results
  private void clearRecognitionResults() {
    stableResultHasBeenReached = false;
    surfaceViewWithOverlay.setLines(null, ITextCaptureService.ResultStabilityStatus.NotReady);
    surfaceViewWithOverlay.setFillBackground(false);
  }

  // Returns orientation of camera
  private int getCameraOrientation() {
    Display display = getWindowManager().getDefaultDisplay();
    int orientation = 0;
    switch (display.getRotation()) {
      case Surface.ROTATION_0:
        break;
      case Surface.ROTATION_90:
        orientation = 90;
        break;
      case Surface.ROTATION_180:
        orientation = 180;
        break;
      case Surface.ROTATION_270:
        orientation = 270;
        break;
    }
    for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
      Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
      Camera.getCameraInfo(i, cameraInfo);
      if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
        return (cameraInfo.orientation - orientation + 360) % 360;
      }
    }
    // If Camera.open() succeed, this point of code never reached
    return -1;
  }

  private void configureCameraAndStartPreview(Camera camera) {
    // Setting camera parameters when preview is running can cause crashes on some android devices
    stopPreview();

    // Configure camera orientation. This is needed for both correct preview orientation
    // and recognition
    orientation = getCameraOrientation();
    camera.setDisplayOrientation(orientation);

    // Configure camera parameters
    Camera.Parameters parameters = camera.getParameters();

    // Select preview size. The preferred size for Text Capture scenario is 1080x720. In some
    // scenarios you might
    // consider using higher resolution (small text, complex background) or lower resolution (better
    // performance, less noise)
    cameraPreviewSize = null;
    for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
      if (size.height <= 720 || size.width <= 720) {
        if (cameraPreviewSize == null) {
          cameraPreviewSize = size;
        } else {
          int resultArea = cameraPreviewSize.width * cameraPreviewSize.height;
          int newArea = size.width * size.height;
          if (newArea > resultArea) {
            cameraPreviewSize = size;
          }
        }
      }
    }
    parameters.setPreviewSize(cameraPreviewSize.width, cameraPreviewSize.height);

    // Zoom
    parameters.setZoom(cameraZoom);
    // Buffer format. The only currently supported format is NV21
    parameters.setPreviewFormat(ImageFormat.NV21);
    // Default focus mode
    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

    // Done
    camera.setParameters(parameters);

    // The camera will fill the buffers with image data and notify us through the callback.
    // The buffers will be sent to camera on requests from recognition service (see implementation
    // of ITextCaptureService.Callback.onRequestLatestFrame above)
    camera.setPreviewCallbackWithBuffer(cameraPreviewCallback);

    // Clear the previous recognition results if any
    clearRecognitionResults();

    // Width and height of the preview according to the current screen rotation
    int width = 0;
    int height = 0;
    switch (orientation) {
      case 0:
      case 180:
        width = cameraPreviewSize.width;
        height = cameraPreviewSize.height;
        break;
      case 90:
      case 270:
        //noinspection SuspiciousNameCombination
        width = cameraPreviewSize.height;
        //noinspection SuspiciousNameCombination
        height = cameraPreviewSize.width;
        break;
    }

    // Configure the view scale and area of interest (camera sees it as rotated 90 degrees, so
    // there's some confusion with what is width and what is height)
    surfaceViewWithOverlay.setScaleX(surfaceViewWithOverlay.getWidth(), width);
    surfaceViewWithOverlay.setScaleY(surfaceViewWithOverlay.getHeight(), height);
    // Area of interest
    int marginWidth = (areaOfInterestMargin_PercentOfWidth * width) / 100;
    int marginHeight = (areaOfInterestMargin_PercentOfHeight * height) / 100;
    surfaceViewWithOverlay.setAreaOfInterest(
        new Rect(marginWidth, marginHeight, width - marginWidth, height - marginHeight));

    // Start preview
    camera.startPreview();

    setCameraFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
    autoFocus(finishCameraInitialisationAutoFocusCallback);

    inPreview = true;
  }

  // Initialize recognition language spinner in the UI with available languages
  private void initializeRecognitionLanguageSpinner() {
    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    final Spinner languageSpinner = findViewById(R.id.recognitionLanguageSpinner);

    // Make the collapsed spinner the size of the selected item
    ArrayAdapter<String> adapter =
        new ArrayAdapter<String>(CameraActivity.this, R.layout.spinner_item) {
          @NonNull
          @Override
          public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            RelativeLayout.LayoutParams params =
                new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            view.setLayoutParams(params);
            return view;
          }
        };

    // Stored preference
    final String recognitionLanguageKey = "RecognitionLanguage";
    String selectedLanguage = preferences.getString(recognitionLanguageKey, "English");

    // Fill the spinner with available languages selecting the previously chosen language
    int selectedIndex = -1;
    for (int i = 0; i < languages.length; i++) {
      String name = languages[i].name();
      adapter.add(name);
      if (name.equalsIgnoreCase(selectedLanguage)) {
        selectedIndex = i;
      }
    }
    if (selectedIndex == -1) {
      adapter.insert(selectedLanguage, 0);
      selectedIndex = 0;
    }

    languageSpinner.setAdapter(adapter);

    if (selectedIndex != -1) {
      languageSpinner.setSelection(selectedIndex);
    }

    // The callback to be called when a language is selected
    languageSpinner.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String recognitionLanguage = (String) parent.getItemAtPosition(position);
            if (ocrService != null) {
              // Reconfigure the recognition service each time a new language is selected
              // This is also called when the spinner is first shown
              ocrService.setRecognitionLanguage(Language.valueOf(recognitionLanguage));
              clearRecognitionResults();
            }
            if (!preferences
                .getString(recognitionLanguageKey, "")
                .equalsIgnoreCase(recognitionLanguage)) {
              // Store the selection in preferences
              SharedPreferences.Editor editor = preferences.edit();
              editor.putString(recognitionLanguageKey, recognitionLanguage);
              editor.apply();
            }
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });
  }

  // The 'Start' and 'Stop' button
  public void onStartButtonClick(View view) {
    if (startButton.getText().equals(BUTTON_TEXT_STOP)) {
      stopRecognition();
    } else {
      clearRecognitionResults();
      startButton.setEnabled(false);
      startButton.setText(BUTTON_TEXT_STARTING);
      if (!isContinuousVideoFocusModeEnabled(camera)) {
        autoFocus(startRecognitionCameraAutoFocusCallback);
      } else {
        startRecognition();
      }
    }
  }

  // Camera permission request handler for Android 6.0 and higher
  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    switch (requestCode) {
      case CAMERA_PERMISSION_REQUEST_CODE:
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          if (camera == null) {
            camera = Camera.open();
          }
        } else {
          showStartupError("Camera is essential for this application.");
        }
        break;
      default:
        throw new IllegalArgumentException();
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_camera);

    // Retrieve some ui components
    warningTextView = findViewById(R.id.warningText);
    errorTextView = findViewById(R.id.errorText);
    startButton = findViewById(R.id.startButton);

    // Initialize the recognition language spinner
    initializeRecognitionLanguageSpinner();

    // Manually create preview surface. The only reason for this is to
    // avoid making it public top level class
    RelativeLayout layout = (RelativeLayout) startButton.getParent();

    surfaceViewWithOverlay = new SurfaceViewWithOverlay(this);
    RelativeLayout.LayoutParams params =
        new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
    surfaceViewWithOverlay.setLayoutParams(params);
    // Add the surface to the layout as the bottom-most view filling the parent
    layout.addView(surfaceViewWithOverlay, 0);

    // Create text capture service
    if (createTextCaptureService()) {
      // Set the callback to be called when the preview surface is ready.
      // We specify it as the last step as a safeguard so that if there are problems
      // loading the engine the preview will never start and we will never attempt calling the
      // service
      surfaceViewWithOverlay.getHolder().addCallback(surfaceCallback);
    }

    layout.setOnClickListener(clickListener);
  }

  @Override
  public void onResume() {
    super.onResume();
    // Reinitialize the camera, restart the preview and recognition if required
    startButton.setEnabled(false);
    clearRecognitionResults();
    startRecognitionWhenReady = startRecognitionOnAppStart;

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        == PackageManager.PERMISSION_GRANTED) {
      if (camera == null) {
        camera = Camera.open();
      }
    } else {
      if (!cameraPermissionRequested) {
        ActivityCompat.requestPermissions(
            this, new String[] {Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
      }
      // After permission dialog is dismissed, onResume will be invoked again
      cameraPermissionRequested = true;
    }
    if (previewSurfaceHolder != null && camera != null) {
      setCameraPreviewDisplayAndStartPreview();
    }
  }

  @Override
  public void onPause() {
    // Clear all pending actions
    handler.removeCallbacksAndMessages(null);
    // Stop the text capture service
    if (ocrService != null) {
      ocrService.stop();
    }
    startButton.setText(BUTTON_TEXT_START);
    // Clear recognition results
    clearRecognitionResults();
    stopPreviewAndReleaseCamera();
    super.onPause();
  }

}
