package com.hellowo.mindpainter;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.hellowo.mindpainter.utils.AnimationUtil;
import com.hellowo.mindpainter.utils.ByteUtil;

public class QuestionDialog extends Dialog {
    MainActivity activity;
    CardView rootLy;
    TextView roundText;
    TextView subText;
    TextView QuestionText;
    Button newQuestionBtn;
    Button startButton;
    LottieAnimationView animationView;

    boolean isMyTurn;
    int round;

    public QuestionDialog(MainActivity activity, int round, boolean isMyTurn) {
        super(activity);
        this.activity = activity;
        this.isMyTurn = isMyTurn;
        this.round = round;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_question);
        setLayout();

        roundText.setText("R O U N D " + round);

        if(isMyTurn) {
            subText.setText(R.string.your_turn_question);
            QuestionText.setText(Question.selectNewQuestion(0));
            animationView.setVisibility(View.GONE);
            newQuestionBtn.setVisibility(View.VISIBLE);
            startButton.setVisibility(View.VISIBLE);

            newQuestionBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    QuestionText.setText(Question.selectNewQuestion(0));
                    AnimationUtil.startScaleShowAnimation(QuestionText);
                }
            });

            startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    long startTime = System.currentTimeMillis() + 3000;
                    GameMessage msg = MainActivity.makeMessage(
                            200, null, 0,
                            Question.question_level,
                            Question.question_num,
                            startTime
                    );
                    activity.broadcastScore(true, ByteUtil.toByteArray(msg));
                    activity.setQuestion();
                    newQuestionBtn.setVisibility(View.GONE);
                    startButton.setVisibility(View.GONE);
                    animationView.setVisibility(View.VISIBLE);
                    animationView.setAnimation("Loading 1.json");
                    animationView.loop(true);
                    animationView.playAnimation();
                    selectedQuestion(startTime);
                }
            });
        }else {
            subText.setText(R.string.not_your_turn_question);
            QuestionText.setVisibility(View.GONE);
            animationView.setVisibility(View.VISIBLE);
            newQuestionBtn.setVisibility(View.GONE);
            startButton.setVisibility(View.GONE);
            animationView.setAnimation("Loading 1.json");
            animationView.loop(true);
            animationView.playAnimation();
        }
    }

    public void selectedQuestion(long startTime) {
        subText.setText(R.string.start_soon);
        AnimationUtil.startScaleShowAnimation(subText);
        subText.postDelayed(new Runnable() {
            @Override
            public void run() {
                activity.startDrawing();
                dismiss();
            }
        }, startTime - System.currentTimeMillis());
    }

    private void setLayout() {
        rootLy = (CardView)findViewById(R.id.rootLy);
        roundText = (TextView)findViewById(R.id.roundText);
        subText = (TextView)findViewById(R.id.subText);
        QuestionText = (TextView)findViewById(R.id.QuestionText);
        newQuestionBtn = (Button)findViewById(R.id.newQuestionBtn);
        startButton = (Button)findViewById(R.id.startButton);
        animationView = (LottieAnimationView)findViewById(R.id.animationView);
        rootLy.setVisibility(View.INVISIBLE);
    }

    @Override
    public void show() {
        super.show();
        rootLy.postDelayed(new Runnable() {
            @Override
            public void run() {
                rootLy.setVisibility(View.VISIBLE);
                AnimationUtil.startScaleShowAnimation(rootLy);
            }
        }, 250);
    }
}
