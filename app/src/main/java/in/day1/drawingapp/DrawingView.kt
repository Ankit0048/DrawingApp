package `in`.day1.drawingapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

// Create the Drawing view class which inherits from the View of the context
class DrawingView(context: Context, attrs: AttributeSet ): View(context, attrs){

//    Defining variable which whill our Drawing View show
    private var mDrawPath : CustomPath ?= null
    private var mCanvasBitmap : Bitmap ?= null
    private var mDrawPaint : Paint ?= null
    private var mCanvasPaint: Paint ?= null
    private var mBrushSize : Float = (0).toFloat()
    private var color = Color.BLACK
    private var canvas: Canvas?= null

    private val mPaths = ArrayList<CustomPath>()
    private val mUndoPaths = ArrayList<CustomPath>()
    init {
        setUpDrawing()
    }

//    Set up drawing in the drawing to set up the variables
    private fun setUpDrawing() {
        mDrawPaint = Paint()
        mDrawPath = CustomPath(color, mBrushSize)
        mDrawPaint!!.color = color
        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
//        mBrushSize = 20.toFloat()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
//        Set the bitmap with ARGB 8888 ie. all colors have 255 values RGB
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
//        Set the given bit map in the canvas variable
        canvas = Canvas(mCanvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas?) {
//        When ever the canvas needs to be drawn on screen
        super.onDraw(canvas)
//        On the canvas it put the bitmap
        canvas?.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)

        for (path in mPaths) {
            if(!path.isEmpty) {
//            Draw the Path on the canvas which is a custom path
                mDrawPaint?.strokeWidth = path.brushThickness
                mDrawPaint?.color = path.color
                canvas?.drawPath(path,mDrawPaint!!)
            }
        }

//            Draw the Path on the canvas which is a custom path
        if(!mDrawPath!!.isEmpty) {
            mDrawPaint?.strokeWidth = mDrawPath!!.brushThickness
            mDrawPaint?.color = mDrawPath!!.color
            canvas?.drawPath(mDrawPath!!,mDrawPaint!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
//        The value of the touch X and Y coordinate are stored
        val touchX = event?.x
        val touchY = event?.y

        when(event?.action) {
//          What should be done when the screen is pressed on the
            MotionEvent.ACTION_DOWN -> {
//                Attach the color and brush Size to the path
                mDrawPath = CustomPath(color, mBrushSize)

                mDrawPath!!.reset()
                if(touchX!=null && touchY!=null) {
                    mDrawPath!!.moveTo(touchX!!, touchY!!)
                }
            }

//            What should we do when we are moving the touch
            MotionEvent.ACTION_MOVE -> {
                if(touchX!=null && touchY!=null) {
                    mDrawPath!!.lineTo(touchX!!, touchY!!)
                }
            }

//            What should be done when we release the touch
            MotionEvent.ACTION_UP -> {
//                Whenever we remove the touch from screen it is added to the array of paths
                mPaths.add(mDrawPath!!)
                mDrawPath = CustomPath(color, mBrushSize)
            }
            else -> return false
        }
//        invalidate calls the onDraw function to draw the path stored till now
        invalidate()
        return true
    }

    fun setBrushSize(newSize: Float) {
//        Set the size of the brush size depending on the size of the display
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newSize, resources.displayMetrics)
    }

    fun setColor (newColor: String) {
//        Set the new color of the brush
        color = Color.parseColor(newColor)
        mDrawPaint!!.color= color
    }

//    removes the previous drawn line
    fun undoDraw () {
        if(mPaths.isEmpty() == false) {
            mUndoPaths.add(mPaths[mPaths.size - 1])
            mPaths.removeLast()
            invalidate()
        }
    }

//    Adds the last removed line from the drawing view
    fun redoDraw () {
        if(mUndoPaths.isEmpty() == false) {
            mPaths.add(mUndoPaths[mUndoPaths.size - 1])
            mUndoPaths.removeLast()
            invalidate()
        }
    }
//    Inner class CustomPath defined which inherits from Path of android
    internal inner class CustomPath(var color: Int, var brushThickness: Float): Path(){

    }

}