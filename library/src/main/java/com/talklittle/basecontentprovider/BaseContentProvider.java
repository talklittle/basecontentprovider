package com.talklittle.basecontentprovider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.talklittle.basecontentprovider.ext.SQLiteContentProvider;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseContentProvider extends SQLiteContentProvider {

    public static final String PARAM_LIMIT = "limit";

    private UriMatcher mUriMatcher;
    private HashMap<String, String> mProjectionMap;

    private final ConcurrentHashMap<Uri, Boolean> mUrisToNotify = new ConcurrentHashMap<Uri, Boolean>();

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(getTableName());

        int matchId = getUriMatcher().match(uri);
        if (matchId == getItemListUriId()) {
            qb.setProjectionMap(getProjectionMap());
        }
        else if (matchId == getItemSingleUriId()) {
            qb.setProjectionMap(getProjectionMap());
            qb.appendWhere("_id=" + uri.getPathSegments().get(1));
        }
        else {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = getDefaultSortOrder();
        } else {
            orderBy = sortOrder;
        }

        String limit = uri.getQueryParameter(PARAM_LIMIT);

        // Get the database and run the query
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy, limit);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        //noinspection ConstantConditions
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    /**
     * If doing replace, override isReplace() and doReplace().
     */
    @Override
    protected Uri insertInTransaction(@NonNull Uri uri, ContentValues initialValues, boolean callerIsSyncAdapter) {
        // Validate the requested uri
        if (getUriMatcher().match(uri) != getItemListUriId()) {
            throw new IllegalArgumentException("Unsupported insert URI " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        Long now = System.currentTimeMillis();
        setDefaultRequiredColumnValues(values, now);

        SQLiteDatabase db = getWritableDatabase();

        long rowId;
        if (isReplace()) {
            rowId = doReplace(db, getTableName(), values);
        }
        else {
            synchronized (this) {
                rowId = db.insert(getTableName(), null, values);
            }
        }

        if (rowId > 0) {
            Uri rowUri = ContentUris.withAppendedId(getContentUri(), rowId);
            mUrisToNotify.put(rowUri, true);
            return rowUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    protected abstract Uri getContentUri();

    protected boolean isReplace() {
        return false;
    }

    /**
     * Do a query if needed to find duplicate record,
     * then conditionally replace() or insert() and return the rowId.
     * @return rowId
     */
    @SuppressWarnings("UnusedParameters")
    protected long doReplace(SQLiteDatabase writableDb, String tableName, ContentValues values) {
        return -1;
    }

    protected abstract void setDefaultRequiredColumnValues(ContentValues values, Long now);

    @Override
    protected int deleteInTransaction(@NonNull Uri uri, String where, String[] whereArgs, boolean callerIsSyncAdapter) {
        SQLiteDatabase db = getWritableDatabase();
        int count;
        int matchId = getUriMatcher().match(uri);
        if (matchId == getItemListUriId()) {
            synchronized (this) {
                count = db.delete(getTableName(), where, whereArgs);
            }
        }
        else if (matchId == getItemSingleUriId()) {
            String itemId = uri.getPathSegments().get(1);
            synchronized (this) {
                count = db.delete(getTableName(), "_id=" + itemId
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            }
        }
        else {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        mUrisToNotify.put(uri, true);
        return count;
    }

    @Override
    protected int updateInTransaction(@NonNull Uri uri, ContentValues values, String where, String[] whereArgs, boolean callerIsSyncAdapter) {
        SQLiteDatabase db = getWritableDatabase();
        int count;
        int matchId = getUriMatcher().match(uri);
        if (matchId == getItemListUriId()) {
            synchronized (this) {
                count = db.update(getTableName(), values, where, whereArgs);
            }
        }
        else if (matchId == getItemSingleUriId()) {
            String itemId = uri.getPathSegments().get(1);
            synchronized (this) {
                count = db.update(getTableName(), values, "_id=" + itemId
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            }
        }
        else {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        mUrisToNotify.put(uri, true);
        return count;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        int matchId = getUriMatcher().match(uri);
        if (matchId == getItemListUriId()) {
            return getItemListContentType();
        }
        else if (matchId == getItemSingleUriId()) {
            return getItemSingleContentType();
        }
        else {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    protected void notifyChange(boolean syncToNetwork) {
        Set<Uri> uris = mUrisToNotify.keySet();
        for (Uri uri : uris) {
            //noinspection ConstantConditions
            ContentResolver contentResolver = getContext().getContentResolver();
            contentResolver.notifyChange(uri, null, syncToNetwork);
        }

        mUrisToNotify.clear();
    }

    protected abstract String getItemListContentType();
    protected abstract String getItemSingleContentType();
    protected abstract int getItemListUriId();
    protected abstract int getItemSingleUriId();
    protected abstract String getDefaultSortOrder();
    protected abstract String getTableName();

    protected final UriMatcher getUriMatcher() {
        if (mUriMatcher == null) {
            mUriMatcher = createUriMatcher();
        }
        return mUriMatcher;
    }

    protected abstract UriMatcher createUriMatcher();

    protected final HashMap<String, String> getProjectionMap() {
        if (mProjectionMap == null)
            mProjectionMap = createProjectionMap();
        return mProjectionMap;
    }

    protected abstract HashMap<String, String> createProjectionMap();

    protected final SQLiteDatabase getReadableDatabase() {
        return getDatabaseHelper().getReadableDatabase();
    }

    protected final SQLiteDatabase getWritableDatabase() {
        return getDatabaseHelper().getWritableDatabase();
    }

}
