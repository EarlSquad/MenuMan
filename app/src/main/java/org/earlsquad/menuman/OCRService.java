package org.earlsquad.menuman;

import android.content.Context;
import android.graphics.Rect;
import com.abbyy.mobile.rtr.Engine;
import com.abbyy.mobile.rtr.ITextCaptureService;
import com.abbyy.mobile.rtr.Language;

import java.io.IOException;

public class OCRService {

  // Licensing
  private static final String LICENSE_FILE = "AbbyyRtrSdk.license";

  private ITextCaptureService textCaptureService;

  private OCRService(ITextCaptureService textCaptureService) {
    this.textCaptureService = textCaptureService;
  }

  public static OCRService create(Context context, ITextCaptureService.Callback textCaptureCallback)
    throws IOException, Engine.LicenseException {
    Engine engine = Engine.load(context, LICENSE_FILE);
    ITextCaptureService textCaptureService = engine.createTextCaptureService(textCaptureCallback);
    return new OCRService(textCaptureService);
  }

  public void stop() {
    textCaptureService.stop();
  }

  public void setRecognitionLanguage(Language language) {
    textCaptureService.setRecognitionLanguage(language);
  }

  public void start(int width, int height, int orientation, Rect areaOfInterest) {
    textCaptureService.start(width, height, orientation, areaOfInterest);
  }

  public void submitRequestedFrame(byte[] data) {
    textCaptureService.submitRequestedFrame(data);
  }
}
