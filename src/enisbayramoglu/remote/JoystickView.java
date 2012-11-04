package enisbayramoglu.remote;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

public class JoystickView extends ImageView {

	float joystickMax = 30;
	float joystickX = 0;
	float joystickY = 0;
	Bitmap joystickbase;
	Bitmap joystick;


	
	public JoystickView(Context activity, AttributeSet attrs) {
		super(activity,attrs);
		joystickbase = BitmapFactory.decodeResource(getResources(), R.drawable.joystickbase);
		joystick =  BitmapFactory.decodeResource(getResources(), R.drawable.joystick);
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		joystickMax = getWidth()/6;
	}
	@Override
	protected void onDraw(Canvas canvas) {
	    canvas.drawBitmap(joystickbase, null, new Rect(getWidth()/6,getHeight()/6,getWidth()*5/6,getHeight()*5/6), null);
		canvas.translate(joystickX+getWidth()/2, joystickY+getHeight()/2);
	    canvas.drawBitmap(joystick, null, new Rect(-getWidth()/3,-getHeight()/3,getWidth()/3,getHeight()/3), null);
	}
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(event.getAction()==MotionEvent.ACTION_UP) {
			joystickX=0;
			joystickY=0;
		} else {
			joystickX=event.getX()-getWidth()/2;
			if(joystickX<-joystickMax) joystickX=-joystickMax;
			if(joystickX>joystickMax) joystickX=joystickMax;
			joystickY=event.getY()-getHeight()/2;
			if(joystickY<-joystickMax) joystickY=-joystickMax;
			if(joystickY>joystickMax) joystickY=joystickMax;
		}
		invalidate();
		return true;
	}
}
