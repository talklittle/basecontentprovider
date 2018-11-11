# Changelog

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