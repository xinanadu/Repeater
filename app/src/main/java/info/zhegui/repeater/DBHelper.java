package info.zhegui.repeater;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Administrator on 2015/8/16.
 */
public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "repeater.db";
    private static final int DB_VERSION = 1;
    private final String TBL_NAME = "pieces";
    private final String COL_ID = "_id";
    /**
     * to which item this belongs
     */
    private final String COL_PATH = "path";
    private final String COL_START = "start";
    private final String COL_END = "end";
    private final String COL_ALIAS = "alias";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL("create table " + TBL_NAME + " (" + COL_ID + " integer primary key autoincrement," +
                    COL_PATH + " text," +
                    COL_START + " text," +
                    COL_END + " text," +
                    COL_ALIAS + " text" +
                    ");");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }


}
