package kr.ac.konkuk.calculator

import androidx.room.Database
import androidx.room.RoomDatabase
import kr.ac.konkuk.calculator.dao.HistoryDao
import kr.ac.konkuk.calculator.model.History

//AppDatabase라는 추상클래스를 만들어서 DB를 먼저 선언
//안에 들어가는 Dao는 인터페이스형식으로 하나하나 구현을 해줌(interface위에 annotation으로 Dao라고 명시해줘야함)
@Database(entities = [History::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}