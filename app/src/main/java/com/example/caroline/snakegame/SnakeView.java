package com.example.caroline.snakegame;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by hzlinxuanxuan on 2015/10/8.
 */
public class SnakeView extends View {

    //该view的默认大小，并且为最小大小
    private int width = 660;
    private int height = 880;
    //将该view切割成许多格子，每个格子的大小，格子为正方形，这里记录边长
    private int cell_size = width / 40;
    private int stroke = 1;
    //画笔
    private Paint wallPaint;
    private Paint snakePaint;
    private Paint backgroundPaint;
    private Paint foodPaint;

    /**
     * 关于蛇的数据
     */
    //蛇最初的长度
    private int snakeDefaultLength = 6;
    private int snakeLength;
    /*
     * 蛇滑动的路线，路线可以用拐点来记录，拐点放在一个list中，直到蛇尾巴走过该拐点，才将该拐点删除
     * 每次蛇头拐弯的地方都要记录下来，并加载list最后面
     * 蛇头和蛇尾都算是拐点
     * 该point记录的是每个方格的左上方的点，这样方便绘画
     */
    private ArrayList<Point> path;
    //蛇默认的爬行方向
    private Orientation or;
    private boolean isOrChanged = false;

    //食物的坐标
    private Point foodPoint;

    //当前游戏状态
    private State cstate;

    private enum Orientation {
        NORTH, EAST, WEST, SOUTH
    }

    private enum State {
        Init, Play, Pause, End
    }

    public SnakeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        parseAttributes(context.obtainStyledAttributes(attrs, R.styleable.SnakeView));
        init();
    }

    private void init() {
        path = new ArrayList<>();
        Point tail = new Point(getPaddingLeft() + cell_size + stroke, getPaddingTop() + cell_size + stroke);
        Point head = new Point(getPaddingLeft() + cell_size + stroke, getPaddingTop() + (cell_size + stroke) * (snakeDefaultLength + 1));
        path.add(0, tail);
        path.add(1, head);

        foodPoint = new Point();
        generateRandomFood();

        or = Orientation.SOUTH;
        cstate = State.Init;

    }

    private void parseAttributes(TypedArray a) {
        wallPaint = new Paint();
        snakePaint = new Paint();
        backgroundPaint = new Paint();
        foodPaint = new Paint();
        wallPaint.setColor(a.getColor(R.styleable.SnakeView_wallColor, 0xffaabbcc));
        snakePaint.setColor(a.getColor(R.styleable.SnakeView_snakeColor, 0xffff0000));
        backgroundPaint.setColor(a.getColor(R.styleable.SnakeView_backgroundColor, 0xffddcc00));
        foodPaint.setColor(a.getColor(R.styleable.SnakeView_foodColor, 0xff000000));
        backgroundPaint.setStrokeWidth(1);
        // Recycle
        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int viewWidth = width + this.getPaddingLeft() + this.getPaddingRight();
        int viewHeight = height + this.getPaddingTop() + this.getPaddingBottom();

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        width = calculateSize(widthMode, viewWidth, widthSize);
        height = calculateSize(heightMode, viewHeight, heightSize);
        BackgroundDrawable drawable = new BackgroundDrawable();
        setBackground(drawable);
        setMeasuredDimension(width, height);
    }

    private int calculateSize(int mode, int defaultSize, int customSize) {
        int size;
        //specified: fill_parent or 50dp(concrete value).
        if (mode == MeasureSpec.EXACTLY) {
            size = customSize;
        }
        //wrap_content: cannot be bigger than the parent size.
        else if (mode == MeasureSpec.AT_MOST) {
            size = Math.min(defaultSize, customSize);
        }
        //Be whatever you want
        else {
            size = defaultSize;
        }
        //返回的数必须是(cell_size+stroke)的整数，包括cell的大小和stroke
        return ((size + cell_size) / (cell_size + stroke)) * (cell_size + stroke) - stroke;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //画最初的蛇,默认位置在左上方
        drawSnake(canvas);
        //画食物
        canvas.drawRect(foodPoint.x, foodPoint.y, foodPoint.x + cell_size, foodPoint.y + cell_size, foodPaint);
    }

    private void drawSnake(Canvas canvas) {
        //根据拐点画出蛇
        for (int i = 0; i < path.size() - 1; i++) {
            drawSnakeLine(canvas, path.get(i), path.get(i + 1));
        }
        //检测是否吃到食物
        checkEatFood();
        //检测是否吃到蛇身
        if (checkTouchItself()) {
            cstate = State.End;
        }
        //只有当游戏正在进行时才重复绘制
        if (cstate == State.Play) {
            //看蛇尾的点和最接近蛇尾的拐点，每次画完蛇都要更新蛇尾的点和蛇头的点
            //一般来说两个相邻的拐点是在一条直线上的，要么x或者y相同
            Point tail = path.get(0);
            Point another = path.get(1);
            if (tail.x == another.x) {
                if (Math.abs(tail.y - another.y) == (cell_size + stroke))
                    path.remove(0);
                else
                    tail.y += another.y > tail.y ? (cell_size + stroke) : -(cell_size + stroke);
            } else {
                if (Math.abs(tail.x - another.x) == (cell_size + stroke))
                    path.remove(0);
                else
                    tail.x += another.x > tail.x ? (cell_size + stroke) : -(cell_size + stroke);
            }
            if (isOrChanged) {
                Point head = path.get(path.size() - 1);
                if (or == Orientation.EAST) {
                    path.add(new Point(head.x + cell_size + stroke, head.y));
                } else if (or == Orientation.WEST) {
                    path.add(new Point(head.x - cell_size - stroke, head.y));
                } else if (or == Orientation.NORTH) {
                    path.add(new Point(head.x, head.y - cell_size - stroke));
                } else {
                    path.add(new Point(head.x, head.y + cell_size + stroke));
                }
                isOrChanged = false;
            } else {
                if (or == Orientation.EAST) {
                    path.get(path.size() - 1).x += cell_size + stroke;
                } else if (or == Orientation.WEST) {
                    path.get(path.size() - 1).x -= cell_size + stroke;
                } else if (or == Orientation.NORTH) {
                    path.get(path.size() - 1).y -= cell_size + stroke;
                } else {
                    path.get(path.size() - 1).y += cell_size + stroke;
                }
            }
            printPath();

            //检测蛇头是否碰到墙壁
            if (checkTouchWall()) {
                cstate = State.End;
            } else {
                handler.sendEmptyMessageDelayed(1, 500);
            }
        }
    }

    private boolean checkTouchItself() {
        Point head = path.get(path.size() - 1);
        Point previous, next;
        for (int i = 0; i < path.size() - 3; i++) {
            previous = path.get(i);
            next = path.get(i + 1);
            int j;
            if (previous.x == next.x && head.x == previous.x) {
                if (head.y <= Math.max(previous.y, next.y) && head.y >= Math.min(previous.y, next.y)) {
                    return true;
                }
                continue;
            } else if (previous.y == next.y && head.y == previous.y) {
                if (head.x <= Math.max(previous.x, next.x) && head.x >= Math.min(previous.x, next.x)) {
                    return true;
                }
                continue;
            }
        }
        return false;
    }

    private void checkEatFood() {
        Point head = path.get(path.size() - 1);
        if (head.x == foodPoint.x && head.y == foodPoint.y) {
            snakeLength++;
            //当吃到食物，蛇头增加
            if (or == Orientation.EAST)
                head.x += cell_size + stroke;
            else if (or == Orientation.WEST)
                head.x -= cell_size + stroke;
            else if (or == Orientation.NORTH)
                head.y -= cell_size + stroke;
            else
                head.y += cell_size + stroke;
            generateRandomFood();
        }
    }

    private void printPath() {
        for (int i = 0; i < path.size(); i++) {
            Log.d("tag", i + ":" + path.get(i).toString());
        }
    }

    private float start_x, start_y, end_x, end_y;
    private boolean isMove = false;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                start_x = event.getX();
                start_y = event.getY();
                isMove = true;
                break;
            case MotionEvent.ACTION_UP:
                if (isMove) {
                    isMove = false;
                    end_x = event.getX();
                    end_y = event.getY();
                    //水平移动
                    if (Math.abs(start_x - end_x) > Math.abs(start_y - end_y)) {
                        if (start_x > end_x)
                            turnOrientation(Orientation.WEST);
                        else
                            turnOrientation(Orientation.EAST);
                    } else {
                        if (start_y > end_y)
                            turnOrientation(Orientation.NORTH);
                        else
                            turnOrientation(Orientation.SOUTH);
                    }
                }
                break;
        }
        return true;
    }

    /**
     * @return true means already touch the wall.
     */
    private boolean checkTouchWall() {
        Point head = path.get(path.size() - 1);
        return head.y == getPaddingTop() || head.y == height - getPaddingBottom() - cell_size
                || head.x == getPaddingLeft() || head.x == width - getPaddingRight() - cell_size;
    }

    private void drawSnakeLine(Canvas canvas, Point start, Point end) {
        //竖着画蛇
        if (start.x == end.x) {
            for (int i = Math.min(end.y, start.y); i <= Math.max(end.y, start.y); i += cell_size + stroke) {
                canvas.drawRect(start.x, i, start.x + cell_size, i + cell_size, snakePaint);
            }
        }
        //横着画蛇
        else if (start.y == end.y) {
            for (int i = Math.min(end.x, start.x); i <= Math.max(end.x, start.x); i += cell_size + stroke) {
                canvas.drawRect(i, start.y, i + cell_size, start.y + cell_size, snakePaint);
            }
        }
    }

    private void turnOrientation(Orientation or) {
        Log.d("tag", "=================" + or);
        if (cstate == State.Play && or != this.or) {
            //变换的方向不可是相对的，例如当蛇正在往东移动时不能往西，只能往南和往北
            if ((this.or == Orientation.EAST && or == Orientation.WEST)
                    || (this.or == Orientation.WEST && or == Orientation.EAST)
                    || (this.or == Orientation.SOUTH && or == Orientation.NORTH)
                    || (this.or == Orientation.NORTH && or == Orientation.SOUTH))
                return;
            this.or = or;
            Log.d("tag", "current orientation :" + this.or);
            isOrChanged = true;
        }
    }

    private void generateRandomFood() {
        int x = 1 + (int) (Math.random() * ((width + stroke) / (cell_size + stroke) - 2));
        int y = 1 + (int) (Math.random() * ((height + stroke) / (cell_size + stroke) - 2));
        foodPoint.x = getPaddingLeft() + (cell_size + stroke) * x;
        foodPoint.y = getPaddingTop() + (cell_size + stroke) * y;
    }

    public void initGame() {
        cstate = State.Init;
        init();
        invalidate();

    }

    public void beginGame() {
        cstate = State.Play;
        handler.sendEmptyMessage(1);
    }

    public void pauseGame() {
        cstate = State.Pause;
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                invalidate();
            }
        }
    };

    private class BackgroundDrawable extends Drawable {

        @Override
        public void draw(Canvas canvas) {
            //上，左，右，下
            canvas.drawRect(getPaddingLeft(), getPaddingTop(), width - getPaddingRight(), getPaddingTop() + cell_size, wallPaint);
            canvas.drawRect(getPaddingLeft(), getPaddingTop(), getPaddingLeft() + cell_size, height - getPaddingBottom(), wallPaint);
            canvas.drawRect(width - getPaddingRight() - cell_size, getPaddingTop(), width - getPaddingRight(), height - getPaddingBottom(), wallPaint);
            canvas.drawRect(getPaddingLeft(), height - getPaddingBottom() - cell_size, width - getPaddingRight(), height - getPaddingBottom(), wallPaint);
            //画方格，先横着画，再竖着画
            for (int i = getPaddingTop() + cell_size; i < height - getPaddingBottom() - cell_size; i += cell_size + stroke) {
                canvas.drawLine(getPaddingLeft() + cell_size, i, width - getPaddingRight() - cell_size, i, backgroundPaint);
            }
            for (int j = getPaddingLeft() + cell_size; j < width - getPaddingRight() - cell_size; j += cell_size + stroke) {
                canvas.drawLine(j, getPaddingTop() + cell_size, j, height - getPaddingBottom() - cell_size, backgroundPaint);
            }
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return 0;
        }
    }
}
