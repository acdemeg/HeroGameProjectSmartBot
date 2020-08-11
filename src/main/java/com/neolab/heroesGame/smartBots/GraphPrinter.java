package com.neolab.heroesGame.smartBots;

import com.neolab.heroesGame.aditional.CommonFunction;
import com.neolab.heroesGame.arena.BattleArena;

public class GraphPrinter {
    public static String printNode(final BattleArena board, final boolean isRoundEnd) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\\n");
        for (final Integer key : board.getArmies().keySet()) {
            stringBuilder.append(String.format("Армия игрока <%d>: \\n", key));
            stringBuilder.append(CommonFunction.printArmyForGraph(board.getArmies().get(key), isRoundEnd));
        }
        return stringBuilder.toString() + "\"";

    }
}
