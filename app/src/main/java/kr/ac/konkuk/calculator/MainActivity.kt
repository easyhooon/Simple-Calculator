package kr.ac.konkuk.calculator

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.room.Room
import kr.ac.konkuk.calculator.model.History
import org.w3c.dom.Text
import java.lang.NumberFormatException
import kotlin.math.exp

class MainActivity : AppCompatActivity() {

    private val expressionTextView: TextView by lazy {
        findViewById<TextView>(R.id.tv_expression)
    }

    private val resultTextView: TextView by lazy {
        findViewById<TextView>(R.id.tv_result)
    }

    private val historyLayout: View by lazy {
        findViewById<View>(R.id.historyLayout)
    }

    private val historyLinearLayout: LinearLayout by lazy {
        findViewById<LinearLayout>(R.id.historyLinearLayout)
    }

    lateinit var db: AppDatabase

    private var isOperator = false
    private var hasOperator = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //builder가 Appdatabase를 반환(.build())
        db = Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            "historyDB"
        ).build()
    }

    //뷰 바인딩
    @RequiresApi(Build.VERSION_CODES.M)
    fun buttonClicked(v: View) {
        when (v.id) {
            R.id.btn0 -> numberButtonClicked("0")
            R.id.btn1 -> numberButtonClicked("1")
            R.id.btn2 -> numberButtonClicked("2")
            R.id.btn3 -> numberButtonClicked("3")
            R.id.btn4 -> numberButtonClicked("4")
            R.id.btn5 -> numberButtonClicked("5")
            R.id.btn6 -> numberButtonClicked("6")
            R.id.btn7 -> numberButtonClicked("7")
            R.id.btn8 -> numberButtonClicked("8")
            R.id.btn9 -> numberButtonClicked("9")
            R.id.btn_plus -> operatorButtonClicked("+")
            R.id.btn_minus -> operatorButtonClicked("-")
            R.id.btn_divider -> operatorButtonClicked("/")
            R.id.btn_modulo -> operatorButtonClicked("%")
            R.id.btn_multi -> operatorButtonClicked("*")
        }
    }

    private fun numberButtonClicked(number: String) {
        //operator를 입력하다가 들어온 경우 띄어쓰기 필요
        if (isOperator) {
            expressionTextView.append(" ")
        }

        isOperator = false

        val expressionText = expressionTextView.text.split(" ")

        //숫자의 자릿수 제한
        //숫자가 들어왔을 때 숫자만 있을 경우 그 숫자가 last이고 15자리 이상이면 컷
        //숫자 이후 연산자가 들어온뒤에 두번째 숫자가 들어온 경우 그 숫자가 last, 15자리 이상이면 컷
        if(expressionText.isNotEmpty() && expressionText.last().length >= 15){
            Toast.makeText(this, "15자리 까지만 사용할 수 있습니다.", Toast.LENGTH_SHORT).show()
            return
        } else if (expressionText.last().isEmpty() && number ==  "0"){
            Toast.makeText(this, "0은 제일 앞에 올 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        expressionTextView.append(number)
//        resultTextView 실시간으로 계산 결과를 넣어야하는 기능
        resultTextView.text = calculateExpression()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun operatorButtonClicked(operator: String) {
        //연산자가 제일 앞에 들어올 경우 무시하는 로직
        if(expressionTextView.text.isEmpty()) {
            return
        }

        when {
            //연산자가 이미 있는 경우 수정을 하려고 뒤로가기버튼을 누르지 않고 바로 연산자를 눌렀을 때, 허용을 해주기 위한 로직
            isOperator -> {
                val text = expressionTextView.text.toString()
                //맨끝에서부터 한자리만 지워주고 operator를 지워줌
                expressionTextView.text = text.dropLast(1) + operator
            }
            //이미 오퍼레이터를 사용한 경우
            hasOperator-> {
                Toast.makeText(this, "연산자는 한번만 사용할 수 있습니다.", Toast.LENGTH_SHORT).show()
                return
            }
            else -> {
                expressionTextView.append(" $operator")
            }
        }
        //마지막 자리에 입력된 것이 연산자일 경우 초록색으로 따로 칠해줄 로직
        val ssb = SpannableStringBuilder(expressionTextView.text)
        ssb.setSpan(
            ForegroundColorSpan(getColor(R.color.green)),
            expressionTextView.text.length -1,
            expressionTextView.text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        expressionTextView.text = ssb

        //연산자가 들어온 것 이므로
        isOperator = true
        hasOperator = true
    }

    fun resultButtonClicked(v: View) {
        //사용자가 액션을 한 것이므로 토스트메세지로 예외를 출력해줘야함
        val expressionTexts = expressionTextView.text.split(" ")

        //비었거나 숫자만 들어온 경우
        if ( expressionTextView.text.isEmpty() || expressionTexts.size == 1){
            return
        }

        if ( expressionTexts.size != 3 && hasOperator) {
            Toast.makeText(this, "아직 완성되지 않은 수식입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        //혹시 모르니
        if (expressionTexts[0].isNumber().not() || expressionTexts[2].isNumber().not()){
            return
        }

        val expressionText = expressionTextView.text.toString()
        val resultText = calculateExpression()

        //DB에 넣어주는 부분
        //메인쓰레드가 아닌 새로운 스레드에서 해줘야됨(네트워크 통신, DB작업 등등의 무거운 작업들 )

        //이 스레드가 먼저 실행될지 메인스레드가 먼저실행될지 알 수 없음
        //따라서 위처럼 expressionText와 같은 변수에 저장을 해놓은 것
        Thread(Runnable {
            db.historyDao().insertHistory(History(null, expressionText, resultText))
        }).start()

        //계산결과가 위로 올라오도록 구현 해줌
        resultTextView.text = ""
        expressionTextView.text = resultText

        isOperator = false
        hasOperator= false

    }

    private fun calculateExpression(): String {
        val expressionTexts = expressionTextView.text.split(" ")

        //예외처리
        if(hasOperator.not() || expressionTexts.size != 3) {
            return ""
        } else if (expressionTexts[0].isNumber().not() || expressionTexts[2].isNumber().not()){
            return ""
        }

        val exp1 = expressionTexts[0].toBigInteger()
        val exp2 = expressionTexts[2].toBigInteger()
        val op = expressionTexts[1]

        return when(op) {
            "+" -> (exp1 + exp2).toString()
            "-" -> (exp1 - exp2).toString()
            "*" -> (exp1 * exp2).toString()
            "/" -> (exp1 / exp2).toString()
            "%" -> (exp1 % exp2).toString()
            else -> ""
        }
    }

    fun clearButtonClicked(v: View) {
        expressionTextView.text = ""
        resultTextView.text = ""
        isOperator = false
        hasOperator = false
    }

    fun historyButtonClicked(v: View) {
        historyLayout.isVisible = true
        //우선 삭제하고
        historyLinearLayout.removeAllViews()

        //디비에서 모든 기록 가져오기
        //뷰에 모든 기록 할당하기

        //최신에 저장한 것을 위에 보이도록 리스트를 뒤집음
        //UI작업을 해주려면 메인스레드(UI스레드로)의 전환이 필요
        Thread(Runnable {
            db.historyDao().getAll().reversed().forEach{
                //하나하나씩 꺼내서 LinearLayout에 넣음
                //뷰를 만드는법 LayoutInflater

                //UI스레드를 여는 방법(뷰작업을 위해 UI스레드로 다시 돌아오는 방법)
                //runOnUiThread라는 것이 handler의 안에 있는 내용을 post하는 식으로 구현이 되어있음!
                runOnUiThread {
                    //인자 순서대로 (레이아웃 파일, root, attachToRoot)
                    //root가 historyLinearLayout이지만 어차피 addView를 통해 붙힐 것 이기 때문에 null
                    val historyView = LayoutInflater.from(this).inflate(R.layout.history_row, null, false)
                    //텍스트를 설정 후
                    historyView.findViewById<TextView>(R.id.tv_expression).text = it.expression
                    historyView.findViewById<TextView>(R.id.tv_result).text = "= ${it.result}"

                    //LinearLayout에 뷰를 올리는 것을 코드로 진행(코드를 통한 view Attach)
                    historyLinearLayout.addView(historyView)
                }
            }
        }).start()


    }

    fun closeHistoryButtonClicked(v: View) {
        historyLayout.isVisible = false
    }

    fun historyClearButtonClicked(v: View) {
        //뷰에서 모든 기록 삭제
        //DB에서 모든 기록 삭제

        historyLinearLayout.removeAllViews()
        Thread(Runnable {
            db.historyDao().deleteAll()
        }).start()
    }
}
//확장함수 (마치 그 객체에 원래 있었던 맴버함수인 것 처럼 사용가능
//클래스이름.함수이름 으로 확장함수를 생성가능 
fun String.isNumber(): Boolean {
    return try {
        this.toBigInteger()
        true
    } catch (e: NumberFormatException) {
        false
    }
}