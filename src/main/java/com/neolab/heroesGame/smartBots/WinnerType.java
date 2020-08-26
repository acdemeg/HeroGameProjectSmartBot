package com.neolab.heroesGame.smartBots;

import java.util.EnumSet;
import java.util.Set;

public enum WinnerType {
    NONE,
    ENEMY_BOT,
    SMART_BOT,
    DRAW,
    ;

    public boolean isTerminalWinnerType() {
        return terminalWinnerTypesSet.contains(this);
    }

    private static final Set<WinnerType> terminalWinnerTypesSet = EnumSet.of(ENEMY_BOT, SMART_BOT, DRAW);
}