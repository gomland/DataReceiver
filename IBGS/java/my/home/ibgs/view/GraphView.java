package my.home.ibgs.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.widget.ImageView;

import my.home.ibgs.tools.Constant;

/**
 * Created by Shin on 2017-02-01.
 */
public class GraphView extends ImageView{
    public static final int A = 0;
    public static final int B = 1;
    public static final int C = 2;
    public static final int SENSOR_MAX = 3;

    private float MAX = Constant.DEFAULT_MAX;  //뷰에 최대 표현 수치 값
    private final float VIEW_X_OFFSET = 0.2f; //A, C점의 left,right 마진 : 너비의 20% (B점은 항상 중간)
    private final int VIEW_Y_OFFSET = 100;  //뷰의 top 마진

    private int data[], offset[];

    public GraphView(Context context, AttributeSet attr){
        super(context, attr);

        initView();
    }

    private void initView(){
        data = new int[3];
        offset = new int[3];
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();

        drawBase(canvas);

        Path trianglePath = new Path();
        Point point, defaultPoint;

        //A 점
        defaultPoint = drawPoint(canvas, (int)(width * VIEW_X_OFFSET), (int)MAX-data[B], offset[B], "B");
        trianglePath.moveTo(defaultPoint.x, defaultPoint.y);

        //B 점
        point = drawPoint(canvas, (int)(width/2), (int)MAX-data[A], offset[A], "A");
        trianglePath.lineTo(point.x, point.y);

        //C 점
        point = drawPoint(canvas, (int)(width - width * VIEW_X_OFFSET), (int)MAX-data[C], offset[C], "C");
        trianglePath.lineTo(point.x, point.y);
        trianglePath.lineTo(defaultPoint.x, defaultPoint.y);

        drawTriangle(canvas, trianglePath);
    }

    //그래프 베이스 라인을 그림
    private void drawBase(Canvas canvas){
        int width = getWidth();
        int max = (int)(MAX / (MAX / width)) + VIEW_Y_OFFSET;

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(5);
        paint.setAlpha(50);

        canvas.drawLine((int)(width * 0.1), VIEW_Y_OFFSET, (int)(width - width * 0.1), VIEW_Y_OFFSET, paint);
        canvas.drawLine((int)(width * 0.1), max, (int)(width - width * 0.1), max, paint);

        paint.setTextSize(width * 0.04f);
        paint.setAlpha(255);
        paint.setColor(Color.BLUE);
        canvas.drawText(String.valueOf((int)MAX), (int)(width * 0.005f), VIEW_Y_OFFSET, paint);
        canvas.drawText("0", (int)(width * 0.1) - width*0.05f, max, paint);
        canvas.drawText("최대 기준값 : " + String.valueOf((int)MAX), (int)(width - width * 0.1 - width*0.3f) , max + width*0.06f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setPathEffect(new DashPathEffect(new float[]{5,5}, 1));
        paint.setStrokeWidth(2);
        paint.setColor(Color.DKGRAY);

        Path path = new Path();
        path.moveTo((int)(width * VIEW_X_OFFSET), VIEW_Y_OFFSET);
        path.lineTo((int)(width * VIEW_X_OFFSET), max);
        path.moveTo((int)(width/2), VIEW_Y_OFFSET);
        path.lineTo((int)(width/2), max);
        path.moveTo((int)(width - width * VIEW_X_OFFSET), VIEW_Y_OFFSET);
        path.lineTo((int)(width - width * VIEW_X_OFFSET), max);
        canvas.drawPath(path, paint);
    }

    //해당 위치에 적색 동그라미를 그림
    private Point drawPoint(Canvas canvas, int xPos, int yPos, int offset, String name){
        float width = getWidth();
        float scale = MAX / width;

        Point point = new Point();
        point.set(xPos,  (int)((float)(yPos + offset)/scale) + VIEW_Y_OFFSET);

        Paint paint = new Paint();
        paint.setColor(Color.RED);
        canvas.drawCircle(point.x, point.y, width * 0.01f, paint);

        paint.setColor(Color.BLACK);
        paint.setTextSize(width * 0.05f);
        canvas.drawText(name + ":" + String.valueOf((int)MAX - yPos - offset), point.x + width * 0.05f, point.y - 3, paint);

        return point;
    }

    //동그라미을 이어주는 직선을 생성
    private void drawTriangle(Canvas canvas, Path trianglePath){
        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8);
        paint.setAlpha(80);

        canvas.drawPath(trianglePath, paint);
    }

    //표시해야되는 데이터값 설정
    public void setData(int key, int value){
        data[key] = value;
    }

    //오프셋 설정
    public void setOffset(int key, int value){
        offset[key] = -value;
    }

    public void setMax(float max){
        MAX = max;
        invalidate();
    }

    //다시 그리기
    public void update(){
        invalidate();
    }
}
