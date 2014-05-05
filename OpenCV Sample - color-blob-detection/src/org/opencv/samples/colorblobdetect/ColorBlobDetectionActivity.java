package org.opencv.samples.colorblobdetect;

import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;



import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;

public class ColorBlobDetectionActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String  TAG              = "OCVSample::Activity";

    private boolean              mIsColorSelected = false;
    private Mat                  mRgba;
    private Scalar               mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private Scalar				 colorThreshold_1;
    private Scalar 				 colorThreshold_2;
    private ColorBlobDetector    mDetector;
    private Mat                  mSpectrum;
    private Size                 SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;
    private double 				 lastX = -1;
    private double				 lastY = -1;
    private Mat 				 linesMat;
    private Mat					 linesResetMap; //turha
    private Size				 blurSize;
	private Mat 				 imgHSV;
	private Mat 				 imgThresh;
    private Point 				 newPoint; 
    private Point 				 lastPoint;
    private Scalar				 lineScalar;
    private Scalar				 lineResetScalar; //turha
    private int 				 lineCounter;
    private double 				 moment01;
    private double 				 moment10;
    private double 				 area;
    private double[]			 tempPos1= {0, 0};
    private double[]			 tempPos2= {0, 0};
    private double 				 posX;
    private double 				 posY;
    private Moments 			 imgMoments;          
    
    // set scalar
    // draw the line    
    

    //Added variable
    private Mat 				 frame;
    
    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(ColorBlobDetectionActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public ColorBlobDetectionActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.color_blob_detection_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        linesMat = new Mat(height, width, CvType.CV_8UC4);
        linesResetMap = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255,0,0,255);
        //Color thresholds for defining the detection range
        colorThreshold_1 = new Scalar(120,120,50);
        colorThreshold_2 = new Scalar(180,2556,256);
        blurSize = new Size(3,3); //Used for gaussian blur
        imgHSV = new Mat(); //Create new HSV matrix
        imgThresh = new Mat(); //ImgThreshold matrix for detecting the objects
        //Points defined for line drawing
        newPoint = new Point(); 
        lastPoint = new Point(); 
        //Scalars defined for line drawing (blur)
        double[] temp2 = {0,0,255};
        lineScalar = new Scalar(temp2);
        double[] temp3 = {0,0,0};
        lineResetScalar = new Scalar(temp3);
        lineCounter = 0; //Counter that checks amount of lines drawn in linesMat matrix
        //Variables for calculating the middle point
    	moment01 = 0;
        moment10 = 0;
        area = 0;
        //Line positions
        posX = 0;
        posY = 0;
        
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x>4) ? x-4 : 0;
        touchedRect.y = (y>4) ? y-4 : 0;

        touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width*touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        mDetector.setHsvColor(mBlobColorHsv);

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

        mIsColorSelected = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();

        return false; // don't need subsequent touch events
    }

    /* (non-Javadoc)
     * @see org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2#onCameraFrame(org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame)
     */
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        if (mIsColorSelected) {   	
        	//Normal Code
            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();
            Log.e(TAG, "Contours count: " + contours.size());
            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);
            //Ends     
            //Custom code starts
//            frame=mRgba.clone();	
//    		Imgproc.blur(frame, frame,blurSize); //smooth the original image using Gaussian kernel	
//    		Imgproc.cvtColor(frame, imgHSV, Imgproc.COLOR_BGR2HSV); //Change the color format from BGR to HSV
//    		Core.inRange(imgHSV, colorThreshold_1, colorThreshold_2, imgThresh);		
//    		Imgproc.blur(imgThresh, imgThresh,blurSize); 
//            imgMoments = Imgproc.moments(imgThresh); 
//            calculateMoments(imgMoments); //Also draws line to linesMat
//            Core.add(mRgba, linesMat, mRgba); //Adds linesMat and mRgba together
            //Custom code ends 
            //Color blob code continues
            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
            colorLabel.setTo(mBlobColorRgba);
            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);
        }
      
        
        Imgproc.blur(mRgba, mRgba, blurSize);
        Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_BGR2HSV);
        Core.inRange(mRgba, colorThreshold_1, colorThreshold_2, mRgba);
        Imgproc.blur(mRgba, mRgba, blurSize);
        imgMoments = Imgproc.moments(mRgba);
        calculateMoments(imgMoments);
        mRgba = inputFrame.rgba();
        Core.add(mRgba, linesMat, mRgba);
        
        return mRgba;
    }
    

    /**
     * Custom function based on the c++ example. Calculates mid point of moments based on imgMoments
     * parameter. It also draws the line from last calculated image position to 
     * current position. 
     * @param imgMoments moments 
     */
    private void calculateMoments(Moments imgMoments){
    	// Check http://stackoverflow.com/questions/8895749/cvgetspatialmoment-in-opencv-2-0
    	moment01 = imgMoments.get_m01();
        moment10 = imgMoments.get_m10();
        area = imgMoments.get_m00(); //Changed this from get_mu11(); 
        if(area>1000){
          // calculate the position of the ball
          posX = (moment10/area);
          posY = (moment01/area);
      		if(lastX>=0 && lastY>=0 && posX>=0 && posY>=0)
              {
                  // Draw a yellow line from the previous point to the current point
                  // set Points for drawing the line
                  tempPos1[0] = posX;
                  tempPos1[1] = posY;
                  newPoint.set(tempPos1);
                  tempPos2[0] = lastX;
                  tempPos2[1] = lastY;
                  lastPoint.set(tempPos2);                  
                  // draw the line                  
                  try{
                	  Core.line(linesMat, newPoint, lastPoint, lineScalar, 4);
                  } finally { 
                	  
                  }
              }
      		lastX = posX;
      		lastY = posY;
      		Log.v(ALARM_SERVICE, String.valueOf(posX));
      		lineCounter++;
      		if(lineCounter > 15) {
      			linesMat.setTo(lineResetScalar);
      			lineCounter = 0;
      		}
      		
        }       
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }
}
