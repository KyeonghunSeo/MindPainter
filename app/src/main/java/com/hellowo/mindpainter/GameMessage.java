package com.hellowo.mindpainter;

import java.io.Serializable;

public class GameMessage implements Serializable {
    byte type;
    String text;
    int pNum;
    float x;
    float y;
    long time;
}
