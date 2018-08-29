package my.home.android.datareceiver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import java.util.ArrayList;

public class GraphView extends View {
    private final int SIZE = 120;

    private String mName;
    private ArrayList<Float> mDataList = new ArrayList<>(SIZE);
    private int mMaxIdx = 0;
    private double mMax = 0;
    private Paint mPaint = new Paint();

    public GraphView(Context context, String name) {
        super(context);
        mName = name;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        final int TOP_MARGIN = 40;
        int height = getHeight() - TOP_MARGIN;

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.BLACK);
        canvas.drawRect(0, TOP_MARGIN, getWidth(), TOP_MARGIN + height, mPaint);

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(3);
        mPaint.setColor(Color.BLUE);
        canvas.drawLine(0, TOP_MARGIN + height - 2, getWidth(), TOP_MARGIN + height, mPaint);
        canvas.drawLine(0, 0, 2, TOP_MARGIN + height, mPaint);
        canvas.drawLine(0, 0, getWidth(), 0, mPaint);
        canvas.drawLine(getWidth() - 1, 0, getWidth() - 2, TOP_MARGIN + height, mPaint);

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.RED);
        float pointY = (float) (height / mMax);
        float pointWidth = getWidth() / (float) SIZE;
        pointWidth = pointWidth < 1 ? 1 : pointWidth;
        for (int i = 0; i < mDataList.size(); i++) {
            canvas.drawRect(i * pointWidth,
                    TOP_MARGIN + height - (pointY * mDataList.get(i)),
                    i * pointWidth + pointWidth,
                    TOP_MARGIN + height - (pointY * mDataList.get(i) - pointWidth),
                    mPaint);
        }

        mPaint.setColor(Color.BLACK);
        mPaint.setTextSize(30);
        mPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Name : " + mName + ", Max : " + String.valueOf(mMax), 10, 30, mPaint);

        mPaint.setTextAlign(Paint.Align.RIGHT);
        if (mDataList.size() > 0) {
            canvas.drawText("Value " + mDataList.get(mDataList.size() - 1), getWidth(), 30, mPaint);
        }
    }

    public void setData(double value) {
        if (value > mMax) {
            mMax = value;
            mMaxIdx = mDataList.size();
        }

        if (mDataList.size() >= SIZE) {
            mDataList.remove(0);

            //Max로 설정되었던 값이 화면을 벗어나면 다시 Max를 산정함
            if (--mMaxIdx < 0) {
                mMax = 0;
                for (int i = 0; i < mDataList.size(); i++) {
                    float data = mDataList.get(i);
                    if (data > mMax) {
                        mMax = data;
                        mMaxIdx = i;
                    }
                }
            }
        }

        mDataList.add((float) value);
        invalidate();
    }
}
