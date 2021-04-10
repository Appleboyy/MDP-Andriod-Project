package com.example.MDP_Android.ui.main;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;

import com.example.MDP_Android.MainActivity;
import com.example.MDP_Android.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GridMap extends View {

    public GridMap(Context c) {
        super(c);
        initMap();
    }

    //     Declare Variables
    SharedPreferences sharedPreferences;

    private Paint blackPaint = new Paint();
    private Paint obstacleColor = new Paint();
    private Paint robotColor = new Paint();
    private Paint robotBase = new Paint();
    private Paint robotHead = new Paint();
    private Paint endColor = new Paint();
    private Paint startColor = new Paint();
    private Paint wayPointColor = new Paint();
    private Paint unexploredColor = new Paint();
    private Paint exploredColor = new Paint();
    private Paint arrowColor = new Paint();
    private Paint fastestPathColor = new Paint();

    private static JSONObject receivedJsonObject = new JSONObject();
    private static JSONObject backupMapInformation;
    private static String robotDirection = "None";
    private static int[] startCoordinate = new int[]{-1, -1};
    private static int[] curCoordinate = new int[]{-1, -1};
    private static int[] oldCoordinate = new int[]{-1, -1};
    private static int[] wayPointCoordinate = new int[]{-1, -1};
    private static ArrayList<String[]> arrowCoordinate = new ArrayList<>();
    private static ArrayList<int[]> obstacleCoordinate = new ArrayList<>();
    private static boolean autoUpdate = false;
    private static boolean canDrawRobot = false;
    private static boolean setWayPointStatus = false;
    private static boolean startCoordinateStatus = false;
    private static boolean setObstacleStatus = false;
    private static boolean unSetCellStatus = false;
    private static boolean setExploredStatus = false;
    private static boolean validPosition = false;
    private static final String TAG = "GridMap";
    private static final int COL = 15;
    private static final int ROW = 20;
    private static float cellSize;
    private static Cell[][] cells;
    private boolean mapDrawn = false;

    private Bitmap arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_error);

    public static String publicMDFExploration;
    public static String publicMDFObstacle;
    public static ArrayList<String> publicImagesString = new ArrayList<>();

    //    Setup page variables
    public GridMap(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initMap();
        blackPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        obstacleColor.setColor(Color.BLACK);
        robotColor.setColor(Color.BLACK);
        robotBase.setColor(Color.GREEN);
        robotBase.setStyle(Paint.Style.STROKE);
        robotHead.setColor(Color.RED);
        endColor.setColor(Color.RED);
        startColor.setColor(Color.CYAN);
        wayPointColor.setColor(Color.YELLOW);
        unexploredColor.setColor(Color.LTGRAY);
        exploredColor.setColor(Color.WHITE);
        arrowColor.setColor(Color.BLACK);
        fastestPathColor.setColor(Color.MAGENTA);

        // Get shared preferences
        sharedPreferences = getContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
    }

    private void initMap() {
        setWillNotDraw(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        showLog("Entering onDraw");
        super.onDraw(canvas);
        showLog("Redrawing map");

        //Create cell coordinates
        Log.d(TAG, "Creating Cell");

        if (!mapDrawn) {
            String[] dummyArrowCoordinate = new String[3];
            dummyArrowCoordinate[0] = "1";
            dummyArrowCoordinate[1] = "1";
            dummyArrowCoordinate[2] = "dummy";
            arrowCoordinate.add(dummyArrowCoordinate);
            this.createCell();
            this.setEndCoordinate(14, 19);
            mapDrawn = true;
        }

        drawIndividualCell(canvas);
        drawHorizontalLines(canvas);
        drawVerticalLines(canvas);
        drawGridNumber(canvas);
        if (getCanDrawRobot())
            drawRobot(canvas, curCoordinate);
        drawArrow(canvas, arrowCoordinate);

        showLog("Exiting onDraw");
    }

    private void drawIndividualCell(Canvas canvas) {
        showLog("Entering drawIndividualCell");
        for (int x = 1; x <= COL; x++)
            for (int y = 0; y < ROW; y++)
                for (int i = 0; i < this.getArrowCoordinate().size(); i++)
                    if (!cells[x][y].type.equals("image") && cells[x][y].getId() == -1) {
                        canvas.drawRect(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY, cells[x][y].paint);
                    } else {
                        Paint textPaint = new Paint();
                        textPaint.setTextSize(20);
                        textPaint.setColor(Color.WHITE);
                        textPaint.setTextAlign(Paint.Align.CENTER);
                        canvas.drawRect(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY, cells[x][y].paint);
                        canvas.drawText(String.valueOf(cells[x][y].getId()), (cells[x][y].startX + cells[x][y].endX) / 2, cells[x][y].endY + (cells[x][y].startY - cells[x][y].endY) / 4, textPaint);
                    }

        showLog("Exiting drawIndividualCell");
    }

    public void drawImageNumberCell(int x, int y, int id) {
        cells[x + 1][19 - y].setType("image");
        cells[x + 1][19 - y].setId(id);
        this.invalidate();
    }

    private void drawHorizontalLines(Canvas canvas) {
        for (int y = 0; y <= ROW; y++)
            canvas.drawLine(cells[1][y].startX, cells[1][y].startY - (cellSize / 30), cells[15][y].endX, cells[15][y].startY - (cellSize / 30), blackPaint);
    }

    private void drawVerticalLines(Canvas canvas) {
        for (int x = 0; x <= COL; x++)
            canvas.drawLine(cells[x][0].startX - (cellSize / 30) + cellSize, cells[x][0].startY - (cellSize / 30), cells[x][0].startX - (cellSize / 30) + cellSize, cells[x][19].endY + (cellSize / 30), blackPaint);
    }

    private void drawGridNumber(Canvas canvas) {
        showLog("Entering drawGridNumber");
        for (int x = 1; x <= COL; x++) {
            if (x > 9)
                canvas.drawText(Integer.toString(x - 1), cells[x][20].startX + (cellSize / 5), cells[x][20].startY + (cellSize / 1.75f), blackPaint);
            else
                canvas.drawText(Integer.toString(x - 1), cells[x][20].startX + (cellSize / 3), cells[x][20].startY + (cellSize / 1.75f), blackPaint);
        }
        for (int y = 0; y < ROW; y++) {
            if ((20 - y) > 10)
                canvas.drawText(Integer.toString(19 - y), cells[0][y].startX + (cellSize / 4f), cells[0][y].startY + (cellSize / 1.5f), blackPaint);
            else
                canvas.drawText(Integer.toString(19 - y), cells[0][y].startX + (cellSize / 3f), cells[0][y].startY + (cellSize / 1.5f), blackPaint);
        }
        showLog("Exiting drawGridNumber");
    }

    private void drawRobot(Canvas canvas, int[] curCoordinate) {
        showLog("Entering drawRobot");
        int androidRowCoordinate = this.convertRow(curCoordinate[1]);
        for (int y = androidRowCoordinate; y <= androidRowCoordinate + 1; y++)
            canvas.drawLine(cells[curCoordinate[0] - 1][y].startX, cells[curCoordinate[0] - 1][y].startY - (cellSize / 30), cells[curCoordinate[0] + 1][y].endX, cells[curCoordinate[0] + 1][y].startY - (cellSize / 30), robotBase);
        for (int x = curCoordinate[0] - 1; x < curCoordinate[0] + 1; x++)
            canvas.drawLine(cells[x][androidRowCoordinate - 1].startX - (cellSize / 30) + cellSize, cells[x][androidRowCoordinate - 1].startY, cells[x][androidRowCoordinate + 1].startX - (cellSize / 30) + cellSize, cells[x][androidRowCoordinate + 1].endY, robotBase);

        float robotHeadCX;
        float robotHeadCY;
        float robotBodyCX = (cells[curCoordinate[0]][androidRowCoordinate - 1].startX + cells[curCoordinate[0]][androidRowCoordinate - 1].endX) / 2;
        float robotBodyCY = (cells[curCoordinate[0]][androidRowCoordinate].startY + cells[curCoordinate[0]][androidRowCoordinate].endY) / 2;
        canvas.drawCircle(robotBodyCX, robotBodyCY, 30, robotColor);

        switch (this.getRobotDirection()) {
            case "up":
                robotHeadCX = (cells[curCoordinate[0]][androidRowCoordinate - 1].startX + cells[curCoordinate[0]][androidRowCoordinate - 1].endX) / 2;
                robotHeadCY = ((cells[curCoordinate[0]][androidRowCoordinate].startY + cells[curCoordinate[0]][androidRowCoordinate].endY) / 2) - 27;
                canvas.drawCircle(robotHeadCX, robotHeadCY, 5, robotHead);
                break;
            case "down":
                robotHeadCX = (cells[curCoordinate[0]][androidRowCoordinate + 1].startX + cells[curCoordinate[0]][androidRowCoordinate + 1].endX) / 2;
                robotHeadCY = ((cells[curCoordinate[0]][androidRowCoordinate + 1].startY + cells[curCoordinate[0]][androidRowCoordinate + 1].startY) / 2) + 15;
                canvas.drawCircle(robotHeadCX, robotHeadCY, 5, robotHead);
                break;
            case "right":
                robotHeadCX = (cells[curCoordinate[0] + 1][androidRowCoordinate - 1].endX + (cells[curCoordinate[0] + 1][androidRowCoordinate].endX - cells[curCoordinate[0] + 1][androidRowCoordinate - 1].endX) / 2) - 7;
                robotHeadCY = cells[curCoordinate[0] + 1][androidRowCoordinate - 1].endY + (cells[curCoordinate[0] + 1][androidRowCoordinate].endY - cells[curCoordinate[0] + 1][androidRowCoordinate - 1].endY) / 2;
                canvas.drawCircle(robotHeadCX, robotHeadCY, 5, robotHead);
                break;
            case "left":
                robotHeadCX = (cells[curCoordinate[0] - 1][androidRowCoordinate - 1].endX + (cells[curCoordinate[0] - 1][androidRowCoordinate].endX - cells[curCoordinate[0] - 1][androidRowCoordinate - 1].endX) / 2) - 15;
                robotHeadCY = cells[curCoordinate[0] - 1][androidRowCoordinate - 1].endY + (cells[curCoordinate[0] - 1][androidRowCoordinate].endY - cells[curCoordinate[0] - 1][androidRowCoordinate - 1].endY) / 2;
                canvas.drawCircle(robotHeadCX, robotHeadCY, 5, robotHead);
                break;
            default:
                Toast.makeText(this.getContext(), "Error with drawing robot (unknown direction)", Toast.LENGTH_LONG).show();
                break;
        }
        showLog("Exiting drawRobot");
    }

    private ArrayList<String[]> getArrowCoordinate() {
        return arrowCoordinate;
    }

    public String getRobotDirection() {
        return robotDirection;
    }

    public void setAutoUpdate(boolean autoUpdate) throws JSONException {
        showLog(String.valueOf(backupMapInformation));
        if (!autoUpdate) {
            showLog("manual");
            backupMapInformation = this.getReceivedJsonObject();
        } else {
            showLog("auto");
            setReceivedJsonObject(backupMapInformation);
            backupMapInformation = null;
            this.updateMapInformation();
        }
        GridMap.autoUpdate = autoUpdate;
    }

    public JSONObject getReceivedJsonObject() {
        return receivedJsonObject;
    }

    public void setReceivedJsonObject(JSONObject receivedJsonObject) {
        showLog("Entered setReceivedJsonObject");
        GridMap.receivedJsonObject = receivedJsonObject;
        backupMapInformation = receivedJsonObject;
    }

    public boolean getAutoUpdate() {
        return autoUpdate;
    }

    private void setValidPosition(boolean status) {
        validPosition = status;
    }

    public boolean getValidPosition() {
        return validPosition;
    }

    public void setUnSetCellStatus(boolean status) {
        unSetCellStatus = status;
    }

    public boolean getUnSetCellStatus() {
        return unSetCellStatus;
    }

    public void setSetObstacleStatus(boolean status) {
        setObstacleStatus = status;
    }

    public boolean getSetObstacleStatus() {
        return setObstacleStatus;
    }

    public void setExploredStatus(boolean status) {
        setExploredStatus = status;
    }

    public boolean getExploredStatus() {
        return setExploredStatus;
    }

    public void setStartCoordinateStatus(boolean status) {
        startCoordinateStatus = status;
    }

    private boolean getStartCoordinateStatus() {
        return startCoordinateStatus;
    }

    public void setWayPointStatus(boolean status) {
        setWayPointStatus = status;
    }

    public boolean getCanDrawRobot() {
        return canDrawRobot;
    }

    private void createCell() {
        showLog("Entering cellCreate");
        cells = new Cell[COL + 1][ROW + 1];
        this.calculateDimension();
        cellSize = this.getCellSize();

        for (int x = 0; x <= COL; x++)
            for (int y = 0; y <= ROW; y++)
                cells[x][y] = new Cell(x * cellSize + (cellSize / 30), y * cellSize + (cellSize / 30), (x + 1) * cellSize, (y + 1) * cellSize, unexploredColor, "unexplored");
        showLog("Exiting createCell");
    }

    public void setEndCoordinate(int col, int row) {
        showLog("Entering setEndCoordinate");
        row = this.convertRow(row);
        for (int x = col - 1; x <= col + 1; x++)
            for (int y = row - 1; y <= row + 1; y++)
                cells[x][y].setType("end");
        showLog("Exiting setEndCoordinate");
    }

    //    Set start position for robot on map [For referencing]
    public void setStartCoordinate(int col, int row) {
        showLog("Entering setStartCoordinate");
        startCoordinate[0] = col;
        startCoordinate[1] = row;
        String direction = getRobotDirection();
        if (direction.equals("None")) {
            direction = "up";
        }
        if (this.getStartCoordinateStatus())
            this.setCurCoordinate(col, row, direction);
        showLog("Exiting setStartCoordinate");
    }

    private int[] getStartCoordinate() {
        return startCoordinate;
    }

    //    Set current position for robot on map
    public void setCurCoordinate(int col, int row, String direction) {
        showLog("Entering setCurCoordinate");
        curCoordinate[0] = col;
        curCoordinate[1] = row;
        this.setRobotDirection(direction);
//        this.updateRobotAxis(col, row, direction);

        row = this.convertRow(row);
        for (int x = col - 1; x <= col + 1; x++)
            for (int y = row - 1; y <= row + 1; y++)
                cells[x][y].setType("robot");
        showLog("Exiting setCurCoordinate");
    }

    public int[] getCurCoordinate() {
        return curCoordinate;
    }

    private void calculateDimension() {
        this.setCellSize(getWidth() / (COL + 1));
    }

    public int convertRow(int row) {
        return (20 - row);
    }

    private void setCellSize(float cellSize) {
        GridMap.cellSize = cellSize;
    }

    private float getCellSize() {
        return cellSize;
    }

    //    Set previous position of robot to state its explored
    private void setOldRobotCoordinate(int oldCol, int oldRow) {
        showLog("Entering setOldRobotCoordinate");
        oldCoordinate[0] = oldCol;
        oldCoordinate[1] = oldRow;
        oldRow = this.convertRow(oldRow);
        for (int x = oldCol - 1; x <= oldCol + 1; x++)
            for (int y = oldRow - 1; y <= oldRow + 1; y++)
                cells[x][y].setType("explored");
        showLog("Exiting setOldRobotCoordinate");
    }

    private int[] getOldRobotCoordinate() {
        return oldCoordinate;
    }

    private void setArrowCoordinate(int col, int row, String arrowDirection) {
        showLog("Entering setArrowCoordinate");
        int[] obstacleCoordinate = new int[]{col, row};
        this.getObstacleCoordinate().add(obstacleCoordinate);
        String[] arrowCoordinate = new String[3];
        arrowCoordinate[0] = String.valueOf(col);
        arrowCoordinate[1] = String.valueOf(row);
        arrowCoordinate[2] = arrowDirection;
        this.getArrowCoordinate().add(arrowCoordinate);

        row = convertRow(row);
        cells[col][row].setType("arrow");
        showLog("Exiting setArrowCoordinate");
    }

    //    Set direction of robot on map
    public void setRobotDirection(String direction) {
        sharedPreferences = getContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        robotDirection = direction;
        editor.putString("direction", direction);
        editor.commit();
        this.invalidate();
        ;
    }

    //    Set WayPoint grid and show on map
    private void setWayPointCoordinate(int col, int row) throws JSONException {
        showLog("Entering setWayPointCoordinate");
        wayPointCoordinate[0] = col;
        wayPointCoordinate[1] = row;

        row = this.convertRow(row);
        cells[col][row].setType("waypoint");

        MainActivity.printMessage("waypoint", wayPointCoordinate[0] - 1, wayPointCoordinate[1] - 1);
        showLog("Exiting setWayPointCoordinate");
    }

    private int[] getWayPointCoordinate() {
        return wayPointCoordinate;
    }

    //    Set obstacle grid and show on map
    private void setObstacleCoordinate(int col, int row) {
        showLog("Entering setObstacleCoordinate");
        int[] obstacleCoord = new int[]{col, row};
        GridMap.obstacleCoordinate.add(obstacleCoord);
        row = this.convertRow(row);
        cells[col][row].setType("obstacle");
        showLog("setObstacleCoordinate: " + (col - 1) + ", " + (Math.abs(row - 19)));
        showLog("Exiting setObstacleCoordinate");
    }

    private ArrayList<int[]> getObstacleCoordinate() {
        return obstacleCoordinate;
    }

    public static void clearObstacleCoordinate() {
        obstacleCoordinate = new ArrayList<>();
    }

    private void showLog(String message) {
        Log.d(TAG, message);
    }

    private void drawArrow(Canvas canvas, ArrayList<String[]> arrowCoordinate) {
        showLog("Entering drawArrow");
        RectF rect;

        for (int i = 0; i < arrowCoordinate.size(); i++) {
            if (!arrowCoordinate.get(i)[2].equals("dummy")) {
                int col = Integer.parseInt(arrowCoordinate.get(i)[0]);
                int row = convertRow(Integer.parseInt(arrowCoordinate.get(i)[1]));
                rect = new RectF(col * cellSize, row * cellSize, (col + 1) * cellSize, (row + 1) * cellSize);
                switch (arrowCoordinate.get(i)[2]) {
                    case "up":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_up);
                        break;
                    case "right":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_right);
                        break;
                    case "down":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_down);
                        break;
                    case "left":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_left);
                        break;
                    default:
                        break;
                }
                canvas.drawBitmap(arrowBitmap, null, rect, null);
            }
            showLog("Exiting drawArrow");
        }
    }


    private class Cell {
        float startX, startY, endX, endY;
        Paint paint;
        String type;
        int id = -1;

        private Cell(float startX, float startY, float endX, float endY, Paint paint, String type) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.paint = paint;
            this.type = type;
        }

        public void setType(String type) {
            this.type = type;
            switch (type) {
                case "obstacle":
                    this.paint = obstacleColor;
                    break;
                case "robot":
                    this.paint = robotBase;
                    break;
                case "end":
                    this.paint = endColor;
                    break;
                case "start":
                    this.paint = startColor;
                    break;
                case "waypoint":
                    this.paint = wayPointColor;
                    break;
                case "unexplored":
                    this.paint = unexploredColor;
                    break;
                case "explored":
                    this.paint = exploredColor;
                    break;
                case "arrow":
                    this.paint = arrowColor;
                    break;
                case "fastestPath":
                    this.paint = fastestPathColor;
                    break;
                case "image":
                    this.paint = obstacleColor;
                default:
                    showLog("setType default: " + type);
                    break;
            }
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getId() {
            return this.id;
        }
    }

    //    When the map is clicked, Set start coordinate, waypoint coordinate, explored and obstacle grids
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        showLog("Entering onTouchEvent");
        if (event.getAction() == MotionEvent.ACTION_DOWN && this.getAutoUpdate() == false) {
            int column = (int) (event.getX() / cellSize);
            int row = this.convertRow((int) (event.getY() / cellSize));
            ToggleButton setStartPointToggleBtn = ((Activity) this.getContext()).findViewById(R.id.setStartPointToggleBtn);
            ToggleButton setWayPointToggleBtn = ((Activity) this.getContext()).findViewById(R.id.setWaypointToggleBtn);

            if (startCoordinateStatus) {
                if (canDrawRobot) {
                    int[] startCoordinate = this.getStartCoordinate();
                    if (startCoordinate[0] >= 2 && startCoordinate[1] >= 2) {
                        startCoordinate[1] = this.convertRow(startCoordinate[1]);
                        for (int x = startCoordinate[0] - 1; x <= startCoordinate[0] + 1; x++)
                            for (int y = startCoordinate[1] - 1; y <= startCoordinate[1] + 1; y++)
                                cells[x][y].setType("unexplored");
                    }
                } else
                    canDrawRobot = true;
                this.setStartCoordinate(column, row);
                startCoordinateStatus = false;
                String direction = getRobotDirection();
                if (direction.equals("None")) {
                    direction = "up";
                }
                try {
                    String updateDirection = "";
                    if (direction.equals("up")) {
                        updateDirection = "N";
                    } else if (direction.equals("left")) {
                        updateDirection = "W";
                    } else if (direction.equals("right")) {
                        updateDirection = "E";
                    } else if (direction.equals("down")) {
                        updateDirection = "S";
                    }
                    MainActivity.printMessage("SP:" + (row - 1) + ":" + (column - 1) + ":" + updateDirection);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (setStartPointToggleBtn.isChecked())
                    setStartPointToggleBtn.toggle();
                this.invalidate();
                return true;
            }
            if (setWayPointStatus) {
                int[] wayPointCoordinate = this.getWayPointCoordinate();
                if (wayPointCoordinate[0] >= 1 && wayPointCoordinate[1] >= 1)
                    cells[wayPointCoordinate[0]][this.convertRow(wayPointCoordinate[1])].setType("unexplored");
                setWayPointStatus = false;
                try {
                    this.setWayPointCoordinate(column, row);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (setWayPointToggleBtn.isChecked())
                    setWayPointToggleBtn.toggle();
                this.invalidate();
                return true;
            }
            if (setObstacleStatus) {
                this.setObstacleCoordinate(column, row);
                this.invalidate();
                return true;
            }
            if (setExploredStatus) {
                cells[column][20 - row].setType("explored");
                this.invalidate();
                return true;
            }
            if (unSetCellStatus) {
                ArrayList<int[]> obstacleCoordinate = this.getObstacleCoordinate();
                cells[column][20 - row].setType("unexplored");
                for (int i = 0; i < obstacleCoordinate.size(); i++)
                    if (obstacleCoordinate.get(i)[0] == column && obstacleCoordinate.get(i)[1] == row)
                        obstacleCoordinate.remove(i);
                this.invalidate();
                return true;
            }
        }
        showLog("Exiting onTouchEvent");
        return false;
    }

    //    Toggling to update statuses
    public void toggleCheckedBtn(String buttonName) {
        ToggleButton setStartPointToggleBtn = ((Activity) this.getContext()).findViewById(R.id.setStartPointToggleBtn);
        ToggleButton setWayPointToggleBtn = ((Activity) this.getContext()).findViewById(R.id.setWaypointToggleBtn);
        ImageButton obstacleImageBtn = ((Activity) this.getContext()).findViewById(R.id.obstacleImageBtn);
        ImageButton exploredImageBtn = ((Activity) this.getContext()).findViewById(R.id.exploredImageBtn);
        ImageButton clearImageBtn = ((Activity) this.getContext()).findViewById(R.id.clearImageBtn);

        if (!buttonName.equals("setStartPointToggleBtn"))
            if (setStartPointToggleBtn.isChecked()) {
                this.setStartCoordinateStatus(false);
                setStartPointToggleBtn.toggle();
            }
        if (!buttonName.equals("setWayPointToggleBtn"))
            if (setWayPointToggleBtn.isChecked()) {
                this.setWayPointStatus(false);
                setWayPointToggleBtn.toggle();
            }
        if (!buttonName.equals("exploredImageBtn"))
            if (exploredImageBtn.isEnabled())
                this.setExploredStatus(false);
        if (!buttonName.equals("obstacleImageBtn"))
            if (obstacleImageBtn.isEnabled())
                this.setSetObstacleStatus(false);
        if (!buttonName.equals("clearImageBtn"))
            if (clearImageBtn.isEnabled())
                this.setUnSetCellStatus(false);
    }


    //    Reset map to a blank state
    public void resetMap() {
        showLog("Entering resetMap");
        TextView robotStatusTextView = ((Activity) this.getContext()).findViewById(R.id.robotStatusTextView);
        Switch manualAutoToggleBtn = ((Activity) this.getContext()).findViewById(R.id.manualAutoToggleBtn);
        Switch phoneTiltSwitch = ((Activity) this.getContext()).findViewById(R.id.phoneTiltSwitch);
        robotStatusTextView.setText("Not Available");

        if (manualAutoToggleBtn.isChecked()) {
            manualAutoToggleBtn.toggle();
            manualAutoToggleBtn.setText("MANUAL");
        }
        this.toggleCheckedBtn("None");

        if (phoneTiltSwitch.isChecked()) {
            phoneTiltSwitch.toggle();
            phoneTiltSwitch.setText("TILT OFF");
        }

        receivedJsonObject = null;
        backupMapInformation = null;
        startCoordinate = new int[]{-1, -1};
        curCoordinate = new int[]{-1, -1};
        oldCoordinate = new int[]{-1, -1};
        robotDirection = "None";
        autoUpdate = false;
        arrowCoordinate = new ArrayList<>();
        obstacleCoordinate = new ArrayList<>();
        wayPointCoordinate = new int[]{-1, -1};
        mapDrawn = false;
        canDrawRobot = false;
        validPosition = false;
        publicImagesString = new ArrayList<>();

        showLog("Exiting resetMap");
        this.invalidate();
    }

    //    Update map information with respective robot location, explored, unexplored and obstacle grids
    public void updateMapInformation() throws JSONException {
        showLog("Entering updateMapInformation");
        JSONObject mapInformation = this.getReceivedJsonObject();
        showLog("updateMapInformation --- mapInformation: " + mapInformation);
        JSONArray infoJsonArray;
        JSONObject infoJsonObject;
        String hexStringExplored, hexStringObstacle, exploredString, obstacleString;
        BigInteger hexBigIntegerExplored, hexBigIntegerObstacle;
        String message;
        String robotCenter = null;
        int exploredCount = 9;
        List<Integer> roboCenterlist = new ArrayList<Integer>();

        if (mapInformation == null)
            return;

        showLog("LENGTH MapInfo: " + mapInformation.length());
        for (int i = 0; i < mapInformation.names().length(); i++) {
            message = "updateMapInformation Default message";
            showLog("MapInfo: " + mapInformation.names().getString(i));
            switch (mapInformation.names().getString(i)) {
                case "map":
                    showLog("map CHECK");
                    showLog("mapFIND: " + mapInformation.getString("map"));
                    hexStringExplored = mapInformation.getString("map");
                    hexBigIntegerExplored = new BigInteger(hexStringExplored, 16);
                    exploredString = hexBigIntegerExplored.toString(2);
                    showLog("updateMapInformation.exploredString: " + exploredString);
                    showLog(String.valueOf(exploredString.length()));
                    int x, y;
                    for (int j = 0; j < (exploredString.length() - 4); j++) {
                        y = 19 - (j / 15);
                        x = 1 + j - ((19 - y) * 15);
                        String s = String.valueOf(exploredString.charAt(j + 2));
                        if (s.equals("1") && !cells[x][y].type.equals("robot")) {
                            cells[x][y].setType("explored");
                            exploredCount++;
                        } else if (s.equals("0") && !cells[x][y].type.equals("robot"))
                            cells[x][y].setType("unexplored");
                    }
                    hexStringObstacle = mapInformation.getString("obstacle");
                    int length = hexStringObstacle.length() * 4;
                    hexBigIntegerObstacle = new BigInteger(hexStringObstacle, 16);
                    obstacleString = hexBigIntegerObstacle.toString(2);
                    while (obstacleString.length() < length) {
                        obstacleString = "0" + obstacleString;
                    }
                    showLog("updateMapInformation obstacleString: " + obstacleString);
                    setPublicMDFExploration(hexStringExplored);
                    setPublicMDFObstacle(hexStringObstacle);

                    int k = 0;
                    for (int row = ROW - 1; row >= 0; row--)
                        for (int col = 1; col <= COL; col++)
                            if ((cells[col][row].type.equals("explored") || (cells[col][row].type.equals("robot"))) && k < obstacleString.length()) {
                                if ((String.valueOf(obstacleString.charAt(k))).equals("1"))
                                    this.setObstacleCoordinate(col, 20 - row);
                                k++;
                            }

                    int[] wayPointCoord = this.getWayPointCoordinate();
                    if (wayPointCoord[0] >= 1 && wayPointCoord[1] >= 1)
                        cells[wayPointCoord[0]][20 - wayPointCoord[1]].setType("waypoint");
                    break;
                case "robotPosition":
                    showLog("robotPos REACHED");
                    if (canDrawRobot)
                        this.setOldRobotCoordinate(curCoordinate[0], curCoordinate[1]);
                    infoJsonArray = mapInformation.getJSONArray("robotPosition");
                    String direction;
                    if (infoJsonArray.getString(2) == "E") {
                        direction = "right";
                    } else if (infoJsonArray.getString(2) == "S") {
                        direction = "down";
                    } else if (infoJsonArray.getString(2) == "W") {
                        direction = "left";
                    } else {
                        direction = "up";
                    }
                    this.setStartCoordinate(infoJsonArray.getInt(0), infoJsonArray.getInt(1));
                    this.setCurCoordinate(infoJsonArray.getInt(0) + 2, convertRow(infoJsonArray.getInt(1)) - 1, direction);
                    canDrawRobot = true;
                    break;
                case "robotCenter":
                    showLog("robotCenter REACHED");
                    showLog(curCoordinate[0] + ", " + curCoordinate[1]);
                    if (canDrawRobot)
                        this.setOldRobotCoordinate(curCoordinate[0], curCoordinate[1]);
                    robotCenter = mapInformation.getString("robotCenter");
                    showLog("robotCenter: " + robotCenter);
                    String[] splitRobotCenter = robotCenter.replace(" ", "").split(",|\\(|\\)");
                    showLog("splits: " + splitRobotCenter[0] + ", " + splitRobotCenter[1]);

                    roboCenterlist = new ArrayList<Integer>();
                    Matcher robotCenterMatcher = Pattern.compile("\\d+").matcher(robotCenter);
                    while (robotCenterMatcher.find()) {
                        roboCenterlist.add(Integer.parseInt(robotCenterMatcher.group()));
                    }
                    this.setStartCoordinate(Integer.parseInt(String.valueOf(roboCenterlist.get(0))), Integer.parseInt(String.valueOf(roboCenterlist.get(1))));
                    canDrawRobot = true;
                    break;
                case "heading":
                    showLog("robotHeading REACHED");
                    String robotHeading = mapInformation.getString("heading");
                    showLog("robotHeading: " + robotHeading);

                    String robotHead = String.valueOf(robotHeading.charAt(0));
                    if (robotHead.contentEquals("E")) {
                        robotHead = "right";
                    } else if (robotHead.contentEquals("S")) {
                        robotHead = "down";
                    } else if (robotHead.contentEquals("W")) {
                        robotHead = "left";
                    } else {
                        robotHead = "up";
                    }
                    showLog("robotHead: " + robotHead);
                    this.setCurCoordinate(Integer.parseInt(String.valueOf(roboCenterlist.get(0))) + 1, Integer.parseInt(String.valueOf(roboCenterlist.get(1))) + 1, robotHead);
                    canDrawRobot = true;
                    break;
                case "waypoint":
                    infoJsonArray = mapInformation.getJSONArray("waypoint");
                    infoJsonObject = infoJsonArray.getJSONObject(0);
                    this.setWayPointCoordinate(infoJsonObject.getInt("x"), infoJsonObject.getInt("y"));
                    setWayPointStatus = true;
                    break;
                case "arrow":
                    infoJsonArray = mapInformation.getJSONArray("arrow");
                    for (int j = 0; j < infoJsonArray.length(); j++) {
                        infoJsonObject = infoJsonArray.getJSONObject(j);
                        if (!infoJsonObject.getString("face").equals("dummy")) {
                            this.setArrowCoordinate(infoJsonObject.getInt("x"), infoJsonObject.getInt("y"), infoJsonObject.getString("face"));
                            message = "Arrow:  (" + infoJsonObject.getInt("x") + "," + infoJsonObject.getInt("y") + "), face: " + infoJsonObject.getString("face");
                        }
                    }
                    break;
                case "move":
                    infoJsonArray = mapInformation.getJSONArray("move");
                    infoJsonObject = infoJsonArray.getJSONObject(0);
                    if (canDrawRobot)
                        moveRobot(infoJsonObject.getString("direction"));
                    message = "moveDirection: " + infoJsonObject.getString("direction");
                    break;
                case "status":
                    String msg = mapInformation.getString("status");
                    printRobotStatus(msg);
                    message = "status: " + msg;
                    break;
                default:
                    message = "Unintended default for JSONObject";
                    break;
            }
            if (!message.equals("updateMapInformation Default message"))
                MainActivity.receiveMessage(message);
        }
        showLog("Exiting updateMapInformation");
        this.invalidate();
    }

    //    To move robot in direction given
    public void moveRobot(String direction) {
        showLog("Entering moveRobot");
        setValidPosition(false);
        int[] curCoordinate = this.getCurCoordinate();
        ArrayList<int[]> obstacleCoordinate = this.getObstacleCoordinate();
        this.setOldRobotCoordinate(curCoordinate[0], curCoordinate[1]);
        int[] oldCoordinate = this.getOldRobotCoordinate();
        String robotDirection = getRobotDirection();
        String backupDirection = robotDirection;

        switch (robotDirection) {
            case "up":
                switch (direction) {
                    case "forward":
                        if (curCoordinate[1] != 19) {
                            curCoordinate[1] += 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "right";
                        break;
                    case "back":
                        if (curCoordinate[1] != 2) {
                            curCoordinate[1] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "left";
                        break;
                    default:
                        robotDirection = "error up";
                        break;
                }
                break;
            case "right":
                switch (direction) {
                    case "forward":
                        if (curCoordinate[0] != 14) {
                            curCoordinate[0] += 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "down";
                        break;
                    case "back":
                        if (curCoordinate[0] != 2) {
                            curCoordinate[0] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "up";
                        break;
                    default:
                        robotDirection = "error right";
                }
                break;
            case "down":
                switch (direction) {
                    case "forward":
                        if (curCoordinate[1] != 2) {
                            curCoordinate[1] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "left";
                        break;
                    case "back":
                        if (curCoordinate[1] != 19) {
                            curCoordinate[1] += 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "right";
                        break;
                    default:
                        robotDirection = "error down";
                }
                break;
            case "left":
                switch (direction) {
                    case "forward":
                        if (curCoordinate[0] != 2) {
                            curCoordinate[0] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "up";
                        break;
                    case "back":
                        if (curCoordinate[0] != 14) {
                            curCoordinate[0] += 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "down";
                        break;
                    default:
                        robotDirection = "error left";
                }
                break;
            default:
                robotDirection = "error moveCurCoordinate";
                break;
        }
        if (getValidPosition())
            for (int x = curCoordinate[0] - 1; x <= curCoordinate[0] + 1; x++) {
                for (int y = curCoordinate[1] - 1; y <= curCoordinate[1] + 1; y++) {
                    for (int i = 0; i < obstacleCoordinate.size(); i++) {
                        if (obstacleCoordinate.get(i)[0] != x || obstacleCoordinate.get(i)[1] != y) {
                            showLog("obsCo: " + obstacleCoordinate.get(i)[0] + ", " + obstacleCoordinate.get(i)[1]);
                            setValidPosition(true);
                        } else {
                            setValidPosition(false);
                            break;
                        }
                    }
                    if (!getValidPosition())
                        break;
                }
                if (!getValidPosition())
                    break;
            }
        if (getValidPosition())
            this.setCurCoordinate(curCoordinate[0], curCoordinate[1], robotDirection);
        else {
            if (direction.equals("forward") || direction.equals("back"))
                robotDirection = backupDirection;
            this.setCurCoordinate(oldCoordinate[0], oldCoordinate[1], robotDirection);
        }
        this.invalidate();
        showLog("Exiting moveRobot");
    }

    //    Display robot status
    public void printRobotStatus(String message) {
        TextView robotStatusTextView = ((Activity) this.getContext()).findViewById(R.id.robotStatusTextView);
        robotStatusTextView.setText(message);
    }

    public static void setPublicMDFExploration(String msg) {
        publicMDFExploration = msg;
    }

    public static void setPublicMDFObstacle(String msg) {
        publicMDFObstacle = msg;
    }

    public static String getPublicMDFExploration() {
        return publicMDFExploration;
    }

    public static String getPublicMDFObstacle() {
        return publicMDFObstacle;
    }

    public static ArrayList<String> getPublicImagesString() {
        return publicImagesString;
    }

    ;
}
