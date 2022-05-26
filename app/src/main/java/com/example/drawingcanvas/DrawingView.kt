package com.example.drawingcanvas

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import kotlin.collections.ArrayList

//DrawingView is defined as a view and not an activity which can be visible inside the main activity
//Here we want to draw something in the UI so we use a view, instead of using buttons/textViews kind of view
//inherit from class View()
/*To draw something, you need 4 basic components: A Bitmap to hold the pixels, a Canvas to host the draw calls
 (writing into the bitmap), a drawing primitive (e.g. Rect, Path, text, Bitmap), and a paint (to describe
 the colors and styles for the drawing). The android.graphics framework divides drawing into two areas:
  What to draw, handled by Canvas. How to draw, handled by Paint
 */
class DrawingView(context: Context, attrs:AttributeSet): View(context, attrs){

    //creating a nullable variable/object of type CustomPath
    private var mDrawPath: CustomPath?=null

    /*A bitmap is method by which a display space (such as a graphics image file or GIF) is defined,
     including the colour of each of its pixels (or bits)*/
    private var mCanvasBitmap: Bitmap?=null   //Bitmap holds pixel

    //Paint class holds the style, color and info about how to draw the geometries, text & bitmaps
    private var mDrawPaint: Paint?=null
    private var mCanvasPaint: Paint?=null
    private var mBrushSize:Float=0f
    private var mColor= Color.BLACK //setting black as default
    //a Canvas to host the draw calls (writing into the bitmap)
    private var canvas: Canvas?=null  //white background that we draw on
    //saving the paths in an arraylist in order to persist the lines on the view
    private val mPaths= ArrayList<CustomPath>()
    private val deletedPaths =ArrayList<CustomPath>()

    init {
        setUpDrawing()
    }

    private fun setUpDrawing(){
        mDrawPath= CustomPath(mColor, mBrushSize)
        mDrawPaint=Paint()            //now mDrawPaint is not empty
        mDrawPaint!!.color = mColor   //this is equal to mDrawPaint!!.setColor(color)
        mDrawPaint!!.style= Paint.Style.STROKE     //stroke is a line of color following a path. A fill is a color enclosed by a path
        mDrawPaint!!.strokeJoin= Paint.Join.ROUND  //Sets the style of the joints which connect line segments.
        mDrawPaint!!.strokeCap= Paint.Cap.ROUND    //end of a stroke will be rounded
        mCanvasPaint=Paint(Paint.DITHER_FLAG)  //Dithering affects how colors that are higher precision than the device are down-sampled
        //mBrushSize= 20f (avoid hardcoding)
    }

     //onSizeChange() is a method from View Class which is called when your view is first assigned a size,
    // and again if the size of your view/screen changes for any reason. We want to display our canvas here
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
         //ARGB_8888 is a 32 bit format. Each pixel is stored on 4 bytes. A:Alpha(translucency)
         mCanvasBitmap=Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
         canvas= Canvas(mCanvasBitmap!!)
    }

    //a View's onDraw() is called when: 1)The view is initially drawn 2)Whenever invalidate() is called on the view
    /*The onDraw method is called whenever android thinks that your view should be redrawn. This can be tha case
     when your view is animated, in which case onDraw is called for every frame in the animation.
     It is also called when the layout changes and your view is re-positioned on the screen.*/
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        //draw the saved paths using arraylist upon releasing the screen
        for(i in mPaths){
            mDrawPaint!!.strokeWidth= i.brushThickness
            mDrawPaint!!.color =i.color
            canvas?.drawPath(i, mDrawPaint!!)
        }
        /*drawBitmap: Draw the specified bitmap, with its top/left corner at (x,y),
        using the specified paint, transformed by the current matrix. */
        //mCanvasBitmap is the bitmap which is to be drawn with paint- mCanvasPaint which can be null
        canvas?.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)
        if(!mDrawPath!!.isEmpty){
            mDrawPaint!!.strokeWidth= mDrawPath!!.brushThickness
            mDrawPaint!!.color =mDrawPath!!.color
            canvas?.drawPath(mDrawPath!!, mDrawPaint!!)
        }
        /*we are drawing something when we are touching the screen so we need to change/fill the
        mDrawPath with a path which should be drawn.
        So we override a function onTouchEvent as onDraw is currently empty*/

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y
        when(event?.action){      //Mainly there are 3 types of actions. finger touch, finger move, finger Up
            MotionEvent.ACTION_DOWN -> {
                mDrawPath!!.brushThickness= mBrushSize
                mDrawPath!!.color= mColor

                mDrawPath!!.reset()             //clears the lines that occurs in our path
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.moveTo(touchX, touchY)
                    }
                }
            }
            MotionEvent.ACTION_MOVE ->{
                mDrawPath!!.lineTo(touchX!!, touchY!!)
            }
            MotionEvent.ACTION_UP ->{
                mPaths.add(mDrawPath!!)        //saving the path upon action up
                mDrawPath= CustomPath(mColor, mBrushSize)

            }
            else ->
                return false
        }
        //generally, invalidate() means 'redraw on screen' and results to a call of the view's onDraw() method.
        // Useful to implement a custom view, for built in widgets there is no need to call it yourself
        invalidate()
        return true
    }

     fun setBrushThickness(newThickness: Float){
        //TypedValue: Container for a dynamically typed data value.
        //applyDimension: Converts an unpacked complex data value holding a dimension to its final floating point value.
        //TYPE_DIMENSION complex unit: Value is Device Independent Pixels.
        /*displayMetrics: A structure describing general information about a display, such as its size, density, and font scaling.
        Current display metrics to use in the conversion -- supplies display density and scaling information.*/
        //we cannot directly assign newThickness to m BrushSize to allow proportionate display in different devices screen
        mBrushSize=TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newThickness, resources.displayMetrics)
        mDrawPaint!!.strokeWidth= mBrushSize
    }

    fun setColor(newColor: String){
        mColor =Color.parseColor(newColor) //Note: #ffffff is a string from colorTag
        mDrawPaint!!.color= mColor
        invalidate()
    }


    //CustomPath is a nested class made by us which inherits from path() class - android graphics
    /*The Path class encapsulates compound (multiple contour) geometric paths consisting of
    straight line segments, quadratic curves, and cubic curves.
    It can be drawn with canvas.drawPath(path, paint), either filled or stroked (based on the paint's Style),
    or it can be used for clipping or to draw text on a path.
     */
    internal inner class CustomPath(var color: Int, var brushThickness: Float) : Path(){

    }

    fun removeDrawPath(){
        if(mPaths.size>0) {
            deletedPaths.add(mPaths[mPaths.size - 1])
            mPaths.removeAt(mPaths.size - 1)
            invalidate()
        }
        if(mPaths.size==0 && mPathsCopy.size!=0){
            for(i in mPathsCopy)
            mPaths.add(i)
            mPathsCopy.clear()
            invalidate()
        }
    }
    fun redoDrawPath(){
        if(deletedPaths.size!=0) {
            mPaths.add(deletedPaths[deletedPaths.size - 1])
            deletedPaths.removeAt(deletedPaths.size - 1)
            invalidate()
        }
    }
    private val mPathsCopy = ArrayList<CustomPath>()
    fun clearScreen(){
        if(mPaths.size!=0) {
            for (i in mPaths) {
                mPathsCopy.add(i)
            }
            mPaths.clear()
            invalidate()
        }
    }
}