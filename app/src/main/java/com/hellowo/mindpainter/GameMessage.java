package com.hellowo.mindpainter;

import java.io.Serializable;

public class GameMessage implements Serializable {
    int type;
    String text;
    int pNum;
    float x;
    float y;
    long time;
}
