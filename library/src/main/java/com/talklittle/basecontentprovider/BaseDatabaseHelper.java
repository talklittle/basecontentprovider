package com.talklittle.basecontentprovider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public abstract class BaseDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "BaseDatabaseHelper";

    public BaseDatabaseHelper(Context context, String databaseName, int databaseVersion) {
        super(context, databaseName, null, databaseVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(getCreateTableSql());

        String[] indexSqls = getCreateIndexSql();
        if (indexSqls != null) {
            for (String indexSql : indexSqls) {
                db.execSQL(indexSql);
            }
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + getTableName());
        onCreate(db);
    }

    protected abstract String getCreateTableSql();

    protected String[] getCreateIndexSql() {
        return null;
    }

    protected abstract String getTableName();

}
