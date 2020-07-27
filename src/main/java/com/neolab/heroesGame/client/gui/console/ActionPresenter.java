package com.neolab.heroesGame.client.gui.console;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.terminal.Terminal;
import com.neolab.heroesGame.arena.Army;
import com.neolab.heroesGame.arena.BattleArena;
import com.neolab.heroesGame.arena.SquareCoordinate;
import com.neolab.heroesGame.client.dto.ExtendedServerResponse;
import com.neolab.heroesGame.enumerations.GameEvent;
import com.neolab.heroesGame.enumerations.HeroActions;
import com.neolab.heroesGame.heroes.Hero;
import com.neolab.heroesGame.server.ActionEffect;

import java.io.IOException;
import java.util.Optional;

import static com.neolab.heroesGame.client.gui.console.AsciiGraphics.*;

public class ActionPresenter {
    private final Terminal term;
    private final TextGraphics textGraphics;
    private int leftOffset;
    private final int playerId;
    private final int infoString = 22;
    private int lastRow = 0;

    public ActionPresenter(final int playerId, final Terminal term, final TextGraphics textGraphics) throws IOException {
        this.term = term;
        this.textGraphics = textGraphics;
        leftOffset = (term.getTerminalSize().getColumns() - ARMY_WIDTH) / 2;
        this.playerId = playerId;
    }

    public int showPosition(final ExtendedServerResponse response, final boolean isYourTurn) throws IOException {
        final ActionEffect effect = Optional.ofNullable(response.effect).orElse(ActionEffect.defaultActionEffect());
        term.clearScreen();
        printBattleArena(response.arena, effect);
        printTurn(isYourTurn);
        if (!effect.getSourceUnit().equals(IS_AOE_EFFECT)) {
            printEffect(effect, isYourTurn);
        }
        term.flush();
        term.readInput();
        return lastRow;
    }

    public int endGame(final ExtendedServerResponse response) throws IOException {
        term.clearScreen();
        printBattleArena(response.arena, response.effect);
        printGameResult(response.event);
        term.flush();
        term.readInput();
        term.close();
        return lastRow;
    }

    private void printGameResult(final GameEvent event) {
        if (event == GameEvent.YOU_WIN_GAME) {
            textGraphics.putString(leftOffset, infoString, "Поздравляем, вы одержали великую победу", SGR.BOLD);
        } else if (event == GameEvent.YOU_LOSE_GAME) {
            textGraphics.putString(leftOffset, infoString, "Ты недостоен своей жизни, умри!", SGR.BOLD);
        } else {
            textGraphics.putString(leftOffset, infoString, "Никто не смог получить преимущества. Ничья", SGR.BOLD);
        }
    }

    private void printBattleArena(final BattleArena arena, final ActionEffect effect) throws IOException {
        leftOffset = (term.getTerminalSize().getColumns() - ARMY_WIDTH) / 2;
        textGraphics.setBackgroundColor(TextColor.ANSI.BLACK);
        textGraphics.setForegroundColor(TextColor.ANSI.WHITE);
        textGraphics.putString(leftOffset, ENEMY_ARMY_ROW_START_AT, "------------Армия противника------------");
        printArmy(arena.getEnemyArmy(playerId), effect, false);
        printArmy(arena.getArmy(playerId), effect, true);
        textGraphics.putString(leftOffset, YOUR_ARMY_ROW_START_AT, "---------------Ваша армия---------------");
    }

    /**
     * С помощью булевой переменной определяем на какую строку выводить армию, в каком порядке выводить линии армии,
     * эта или нет армия действовала в прошлом ходу
     * Проходим в цикле по всем доступным координатам, выбираем для юнита цвет,
     * если юнит только что умер, отрисовываем смерть. Иначе рисуем юнита
     * Если юнита нет, то поле будет пустым
     * Для определения умер ли юнит только что проверяем чей последний ход и обращаемся к функции isUnitDiedRightNow
     * для того, чтобы определить наносился ли этому юниту в прошлом ходу урон
     */
    private void printArmy(final Army yours, final ActionEffect effect, final boolean isItYourArmy) {
        int topString = isItYourArmy ? 12 : 2;
        final boolean isLastMoveMakeThisArmy = (effect.getLastMovedPlayerId() == playerId) == isItYourArmy;
        for (int i = 0; i < 2; i++) {
            final int y = isItYourArmy ? 1 - i : i;
            for (int x = 0; x < 3; x++) {
                final SquareCoordinate coordinate = new SquareCoordinate(x, y);
                final Optional<Hero> hero = yours.getHero(coordinate);
                final TextColor textColor = chooseColorForHero(coordinate, effect, isLastMoveMakeThisArmy);
                final int step = 13;
                if (!isLastMoveMakeThisArmy && hero.isEmpty() && isUnitDiedRightNow(effect, coordinate)) {
                    printDeadUnit(topString, leftOffset + 1 + x * step);
                } else {
                    printHero(hero, yours, topString, leftOffset + 1 + x * step, textColor);
                }
            }
            topString += 4;
        }
    }

    private void printTurn(final boolean isYourTurn) {
        if (isYourTurn) {
            textGraphics.putString(leftOffset, infoString, "Сейчас ваш ход", SGR.BOLD);
        } else {
            textGraphics.putString(leftOffset, infoString, "Сейчас ход вашего оппонента", SGR.BOLD);
        }

    }

    private void printEffect(final ActionEffect effect, final boolean isNowYourTurn) throws IOException {
        final int offsetForEffect = 23;
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Во время прошлого хода ");
        stringBuilder.append(isNowYourTurn ? "вражеский " : "ваш ");
        stringBuilder.append(effect.toString());

        if (effect.getAction() == HeroActions.ATTACK) {
            printAttackEffect(stringBuilder, offsetForEffect);
        } else if (effect.getAction() == HeroActions.DEFENCE) {
            final int offset = (term.getTerminalSize().getColumns() - stringBuilder.length()) / 2;
            textGraphics.putString(offset, offsetForEffect, stringBuilder.toString());
            lastRow = offsetForEffect + 2;
        } else {
            printHealEffect(stringBuilder, offsetForEffect);
        }
    }

    private void printHealEffect(final StringBuilder stringBuilder, int currentStringNumber) {
        final int offset = 10;
        final String[] strings = stringBuilder.toString().split("восстановил");
        textGraphics.putString(0, currentStringNumber++, strings[0] + ":");
        for (int i = 1; i < strings.length; i++) {
            textGraphics.putString(offset, currentStringNumber++, "восстановил" + strings[i]);
        }
        lastRow = currentStringNumber + 1;
    }

    private void printAttackEffect(final StringBuilder stringBuilder, int currentStringNumber) {
        final int offset = 10;
        final String[] strings = stringBuilder.toString().split("нанес|промахнулся");
        textGraphics.putString(0, currentStringNumber++, strings[0] + ":");
        for (int i = 1; i < strings.length; i++) {
            final String additional = strings[i].contains("урона") ? "-нанес" : "-промахнулся";
            textGraphics.putString(offset, currentStringNumber++, additional + strings[i]);
        }
        lastRow = currentStringNumber + 1;
    }

    private boolean isUnitDiedRightNow(final ActionEffect effect, final SquareCoordinate coordinate) {
        return effect.getTargetUnitsMap().containsKey(coordinate)
                && effect.getAction() == HeroActions.ATTACK;

    }

    /**
     * Если последний ход был этой армии, то юнит может быть циан (если он действовал) или зеленным (если его лечили)
     * если лекарь лечил сам себя, то он циан; если юнит встал в защиту, то он циан
     * Если последний ход делала вражеская армия, то юнит может быть красным (если ему нанесли урон)
     * или желтым (если по нему не попали
     * Если в прошлом ходу юнит никак не участвовал, то он остается белым
     */
    private TextColor chooseColorForHero(final SquareCoordinate coordinate, final ActionEffect effect,
                                         final boolean isLastTurnMakeYou) {
        if (isLastTurnMakeYou) {
            if (effect.getSourceUnit().equals(coordinate)) {
                return TextColor.ANSI.CYAN;
            }
            if (effect.getAction() == HeroActions.HEAL && effect.getTargetUnitsMap().containsKey(coordinate)) {
                return TextColor.ANSI.GREEN;
            }
        } else if (effect.getTargetUnitsMap().containsKey(coordinate) && effect.getAction() == HeroActions.ATTACK) {
            if (effect.getTargetUnitsMap().get(coordinate) != 0) {
                return TextColor.ANSI.RED;
            }
            return TextColor.ANSI.YELLOW;
        }
        return TextColor.ANSI.WHITE;
    }

    private void printHero(final Optional<Hero> hero, final Army army, int heightOffset,
                           final int widthOffset, final TextColor textColor) {
        final int width = 12;
        textGraphics.setBackgroundColor(textColor);
        textGraphics.setForegroundColor(TextColor.ANSI.BLACK);
        if (hero.isPresent()) {
            textGraphics.putString(widthOffset, heightOffset++, putToTheCenter(hero.get().getClassName(), width));
            textGraphics.putString(widthOffset, heightOffset++, getUnitHPString(hero.get()));
            textGraphics.putString(widthOffset, heightOffset, getUnitStatus(hero.get(), army));
        } else {
            printEmptyUnit(widthOffset, heightOffset);
        }
        textGraphics.setBackgroundColor(TextColor.ANSI.BLACK);
        textGraphics.setForegroundColor(TextColor.ANSI.WHITE);
    }

    private String getUnitHPString(final Hero hero) {
        return String.format("  HP%3d/%3d ", hero.getHp(), hero.getHpMax());
    }

    private String getUnitStatus(final Hero hero, final Army army) {
        final StringBuilder result = new StringBuilder();
        if (hero.isDefence()) {
            result.append("   D  ");
        } else {
            result.append("      ");
        }
        if (army.getAvailableHeroes().containsValue(hero)) {
            result.append("  CA  ");
        } else {
            result.append("   W  ");
        }
        return result.toString();
    }

    private void printEmptyUnit(int topString, final int widthOffset) {
        textGraphics.putString(widthOffset, topString++, "            ");
        textGraphics.putString(widthOffset, topString++, "            ");
        textGraphics.putString(widthOffset, topString, "            ");
    }

    private void printDeadUnit(int topString, final int widthOffset) {
        textGraphics.setBackgroundColor(TextColor.ANSI.RED);
        textGraphics.setForegroundColor(TextColor.ANSI.BLACK);
        textGraphics.putString(widthOffset, topString++, "            ");
        textGraphics.putString(widthOffset, topString++, "    DEAD    ", SGR.BLINK);
        textGraphics.putString(widthOffset, topString, "            ");
        textGraphics.setBackgroundColor(TextColor.ANSI.BLACK);
        textGraphics.setForegroundColor(TextColor.ANSI.WHITE);
    }
}
