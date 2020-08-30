package com.talklittle.basecontentprovider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

public abstract class BaseDatabaseHelper extends SQLiteOpenHelper {

    public BaseDatabaseHelper(Context context, String databaseName, int databaseVersion) {
        super(context, databaseName, null, databaseVersion);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Android P (9.0) enables write-ahead logging by default, breaking the contract in the
            // framework docs, so revert it to disabled
            db.disableWriteAheadLogging();
        }
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
        throw new UnsupportedOperationException("Database upgrade not implemented");
    }

    /**
     * Can be called from {@link #onUpgrade(SQLiteDatabase, int, int)}
     * to **DELETE ALL DATA IN THE TABLE** when performing upgrades.
     * @param db The SQLiteDatabase from {@link #onUpgrade(SQLiteDatabase, int, int)}
     */
    protected final void deleteAndRecreateTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + getTableName());
        onCreate(db);
    }

    protected abstract String getCreateTableSql();

    protected String[] getCreateIndexSql() {
        return null;
    }

    protected abstract String getTableName();

}
