# Changelog

## 0.6.1 (2021-04-16)

* Migrate to Maven Central repository hosting.
* (0.6.1) Fixed invalid "unspecified" jar dependency.

## 0.5.0 (2020-08-30)

* Throw an UnsupportedOperationException from BaseDatabaseHelper.onUpgrade() instead of deleting data by default.

## 0.4.0 (2020-05-17)

* Support "limit" query parameter in content Uri

## 0.3.0 (2018-11-10)

* Disable write-ahead logging on Android P (9.0). P enables it by default, breaking the contract
  in the framework docs such as
  https://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.html#enableWriteAheadLogging()
* Target API 28
* Minimum API 14

## 0.2.0 (2015-11-26)

* BaseContentProvider extends SQLiteContentProvider

## 0.1.1 (2015-04-20)

* Remove appcompat-v7 dependency

## 0.1.0 (2015-04-19)

* Initial release