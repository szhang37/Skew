package dizzy.med.jhu.edu.mjs.Skew1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class DrawLine extends View {

	Paint paint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	Paint paint2 = new Paint(Paint.ANTI_ALIAS_FLAG);

	// public MyCustomView(Context context, AttributeSet attrs) { super(context,
	// attrs); }
	public DrawLine(Context context, AttributeSet attrs) {
		super(context, attrs);
		// this.color = color;
		// this.xpos = xpos;
		paint1.setColor(Global.red);
		paint2.setColor(Global.blue);
		paint1.setStrokeWidth((float) 3);
		paint2.setStrokeWidth((float) 3);
		paint1.setAlpha(225);
		paint2.setAlpha(225);
	}

	@Override
	public void onDraw(Canvas canvas) {
		// paint1.setColor(Color.RED);
		paint1.setColor(Global.red);
		paint2.setColor(Global.blue);
		int absolutePlus = 200;
		if (Global.color == Color.RED) {
			canvas.drawLine(100+absolutePlus, Global.ypos - Global.yang, 400+absolutePlus, Global.ypos
					, paint1);
			canvas.drawLine(400+absolutePlus,499,700+absolutePlus,499, paint2);
		} else {
			canvas.drawLine(100+absolutePlus, 501, 400+absolutePlus, 501, paint1);
			canvas.drawLine(400+absolutePlus, Global.ypos, 700+absolutePlus, Global.ypos
					+ Global.yang, paint2);
		}
		// canvas.drawLine(100, 500, 700, 500, paint1);
		// canvas.drawLine(100, Global.ypos-Global.yang, 700,
		// Global.ypos+Global.yang, paint2);
	}
}
