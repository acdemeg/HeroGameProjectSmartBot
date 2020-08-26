package com.neolab.heroesGame.smartBots;

import com.neolab.heroesGame.server.answers.Answer;

public class AnswerAndWin {
    final Answer answer;
    final WinCollector winCollector;

    AnswerAndWin(final Answer answer, final WinCollector winCollector) {
        this.answer = answer;
        this.winCollector = winCollector;
    }

    @Override
    public String toString() {
        return answer + " -> " + winCollector;
    }
}
