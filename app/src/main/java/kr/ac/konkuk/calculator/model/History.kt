package kr.ac.konkuk.calculator.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

//데이터클래스는 생성자에 변수를 입력하는 방식으로 손쉽게 작성할 수 있음
//관련 4가지 함수가 자동으로 생성됨

//이 annotation이 있어야 room의 데이터클래스가 됨
//annotation을 통해 DB TABLE을 생성하였음

//모델클래스인 Entity를 data class의 형태로 구현(annotation으로 entity라 명시)
@Entity
data class History(
    //annotation으로 column값을 선언
    @PrimaryKey val uid: Int?,
    @ColumnInfo(name = "expression") val expression: String?,
    @ColumnInfo(name = "result") val result: String?
)