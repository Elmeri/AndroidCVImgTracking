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
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;

public class ColorBlobDetectionActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String  TAG              = "OCVSample::Activity";
    private static final int       VIEW_MODE_TRACK    		  = 0;
    private static final int       VIEW_MODE_COLOR_DETECT     = 1;
    private static final int	   VIEW_MODE_COLOR_HSV	  	  = 2;

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
    private Mat					 linesResetMap; 
    private Size				 blurSize;
	private Mat 				 imgHSV;
	private Mat 				 imgThresh;
    private Point 				 newPoint; 
    private Point 				 lastPoint;
    private Scalar				 lineScalar;
    private Scalar				 lineResetScalar; 
    private int 				 lineCounter;
    private int					 cols;
    private int					 rows;
    private double 				 moment01;
    private double 				 moment10;
    private double 				 area;
    private double[]			 tempPos1= {0, 0};
    private double[]			 tempPos2= {0, 0};
    private double 				 posX;
    private double 				 posY;
    private double				 directionX;
    private double				 directionY;
    private Moments 			 imgMoments;          
    
    // set scalar
    // draw the line    
    
    
    //Menu items
    private MenuItem               mItemPreviewTrack;
    private MenuItem               mItemPreviewColorDetect;
    private MenuItem			   mItemPreviewColorHSV;
    
    private int                    mViewMode;
   
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
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemPreviewTrack = menu.add("Track");
        mItemPreviewColorDetect = menu.add("Detect color");
        mItemPreviewColorHSV = menu.add("Color HSV");
        return true;
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
        rows = 0;
        cols = 0;
        directionX = 0;
        directionY = 0;
        
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public boolean onTouch(View v, MotionEvent event) {
        return false; // don't need subsequent touch events
    }

    /* (non-Javadoc)
     * @see org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2#onCameraFrame(org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame)
     */
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        if (mIsColorSelected) {   	

        }
      
        
        Imgproc.blur(mRgba, mRgba, blurSize);
        Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_BGR2HSV);
        if(mViewMode != VIEW_MODE_COLOR_HSV){
        	Core.inRange(mRgba, colorThreshold_1, colorThreshold_2, mRgba);
            Imgproc.blur(mRgba, mRgba, blurSize);
        }
        if(mViewMode == VIEW_MODE_TRACK) {
            imgMoments = Imgproc.moments(mRgba);
            calculateMoments(imgMoments);
            mRgba = inputFrame.rgba();
            Core.add(mRgba, linesMat, mRgba);
        }
        
        cols = mRgba.cols();
        rows = mRgba.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int) (posX - xOffset);
        int y = (int) (posY - yOffset);

        Log.i(TAG, "Object coordinates: (" + x + ", " + y + ")");
        
        directionX = (posX - ((double) cols/2)) / ((double) cols/2);   // value from 0 to 1
        directionY = (posY - ((double) rows/2)) / ((double) rows/2);   // value from 0 to 1

        String result1 = String.format("%.4f", directionX);
        String result2 = String.format("%.4f", directionY);
        
        Log.i(TAG, "Dir x and y: (" + result1 + ", " + result2 + ")");
        
        //if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;
        
        return mRgba;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        if (item == mItemPreviewTrack) {
            mViewMode = VIEW_MODE_TRACK;
        } else if (item == mItemPreviewColorDetect) {
            mViewMode = VIEW_MODE_COLOR_DETECT;
        } else if (item == mItemPreviewColorHSV){
        	mViewMode = VIEW_MODE_COLOR_HSV;
        }
        return true;
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
      		//Log.v(ALARM_SERVICE, String.valueOf(posX));
      		lineCounter++;
      		if(lineCounter > 15) {
      			linesMat.setTo(lineResetScalar);
      			lineCounter = 0;
      		}
      		
        }       
    }

}
