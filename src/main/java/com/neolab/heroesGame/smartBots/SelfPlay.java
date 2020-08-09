package com.neolab.heroesGame.smartBots;

import com.neolab.heroesGame.aditional.HeroConfigManager;
import com.neolab.heroesGame.arena.Army;
import com.neolab.heroesGame.arena.BattleArena;
import com.neolab.heroesGame.arena.StringArmyFactory;
import com.neolab.heroesGame.client.ai.Player;
import com.neolab.heroesGame.client.ai.PlayerBot;
import com.neolab.heroesGame.enumerations.GameEvent;
import com.neolab.heroesGame.errors.HeroExceptions;
import com.neolab.heroesGame.server.answers.Answer;
import com.neolab.heroesGame.server.answers.AnswerProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SelfPlay {
    public static final Integer MAX_ROUND = 15;
    private static final Logger LOGGER = LoggerFactory.getLogger(SelfPlay.class);
    private Player currentPlayer;
    private Player waitingPlayer;
    private AnswerProcessor answerProcessor;
    private BattleArena battleArena;
    private int counter;
    private final Player[] bots = new Player[2];
    private final Map<Player, Integer> winRate = new HashMap<>();

    public SelfPlay(final Player botOne, final Player botTwo) {
        currentPlayer = botOne;
        waitingPlayer = botTwo;
        counter = 0;
    }

    public GameEvent gameProcess() throws IOException, HeroExceptions {
        LOGGER.info("-----------------Начинается великая битва---------------");
        final Player thisPlayerStartFirst = currentPlayer;
        while (true) {
            final Optional<Player> whoIsWin = someoneWhoWin();
            if (whoIsWin.isPresent()) {
                someoneWin(whoIsWin.get());
                return thisPlayerStartFirst.equals(whoIsWin.get()) ?
                        GameEvent.YOU_WIN_GAME : GameEvent.YOU_LOSE_GAME;
            }

            if (!battleArena.canSomeoneAct()) {
                counter++;
                if (counter > MAX_ROUND) {
                    LOGGER.info("Поединок закончился ничьей");
                    return GameEvent.GAME_END_WITH_A_TIE;
                }
                LOGGER.info("-----------------Начинается раунд <{}>---------------", counter);
                battleArena.endRound();
            }

            if (checkCanMove(currentPlayer.getId())) {
                askPlayerProcess();
            }
            changeCurrentAndWaitingPlayers();
        }
    }

    public void prepareForBattle(Player botOne, Player botTwo) throws IOException, HeroExceptions {
        currentPlayer = botOne;
        waitingPlayer = botTwo;

        String armySize =  HeroConfigManager.getHeroConfig().getProperty("hero.army.size");
        final String player1ArmyResponse = currentPlayer.getStringArmyFirst(Integer.parseInt(armySize));
        final Army player1Army = new StringArmyFactory(player1ArmyResponse).create();

        final String player2ArmyResponse = waitingPlayer.getStringArmyFirst(Integer.parseInt(armySize));
        final Army player2Army = new StringArmyFactory(player2ArmyResponse).create();

        final Map<Integer, Army> battleMap = new HashMap<>();
        battleMap.put(currentPlayer.getId(), player1Army);
        battleMap.put(waitingPlayer.getId(), player2Army);
        this.battleArena = new BattleArena(battleMap);
        this.answerProcessor = new AnswerProcessor(currentPlayer.getId(),
                waitingPlayer.getId(), battleArena);
    }

    private void changeCurrentAndWaitingPlayers() {
        final Player temp = currentPlayer;
        currentPlayer = waitingPlayer;
        waitingPlayer = temp;
        setAnswerProcessorPlayerId(currentPlayer.getId(), waitingPlayer.getId());
    }

    private void setAnswerProcessorPlayerId(final int currentPlayerId, final int waitingPlayerId) {
        answerProcessor.setActivePlayerId(currentPlayerId);
        answerProcessor.setWaitingPlayerId(waitingPlayerId);
    }

    private void askPlayerProcess() throws IOException, HeroExceptions {
        battleArena.toLog();
        final Answer answer = currentPlayer.getAnswer(battleArena);
        answer.toLog();
        answerProcessor.handleAnswer(answer);
        answerProcessor.getActionEffect().toLog();
    }

    private Optional<Player> someoneWhoWin() {
        Player isWinner = battleArena.isArmyDied(getCurrentPlayerId()) ? waitingPlayer : null;
        if (isWinner == null) {
            isWinner = battleArena.isArmyDied(getWaitingPlayerId()) ? currentPlayer : null;
        }
        return Optional.ofNullable(isWinner);
    }

    private boolean checkCanMove(final Integer id) {
        return !battleArena.haveAvailableHeroByArmyId(id);
    }

    private int getCurrentPlayerId() {
        return currentPlayer.getId();
    }

    private int getWaitingPlayerId() {
        return waitingPlayer.getId();
    }

    private void someoneWin(final Player winner){
        winRate.merge(winner, 1, Integer::sum);
        battleArena.toLog();
        LOGGER.info("Игрок<{}> выиграл это тяжкое сражение", winner.getName());
    }

    public static void main(final String[] args) throws IOException, HeroExceptions {

        final long start = System.nanoTime();

        Player smartBot_v1 = new SmartBot_v1(1, "smartBot");
        Player randomBot = new PlayerBot(2, "randomBot");
        SelfPlay selfPlay = new SelfPlay(smartBot_v1, randomBot);

        final int numGames = 100;
        for (int i = 0; i < numGames; i++) {
            if(i % 2 == 0){
                selfPlay.prepareForBattle(randomBot, smartBot_v1);
            }
            else selfPlay.prepareForBattle(smartBot_v1, randomBot);
            selfPlay.gameProcess();
        }

        //win rate calc
        double draw = numGames;
        for (final Map.Entry<Player, Integer> e : selfPlay.winRate.entrySet()) {
            final double winRate = e.getValue() / (double) numGames  * 100;
            draw -= e.getValue();
            LOGGER.warn("{}'s win rate = {} %", e.getKey().getName(), winRate);
            System.out.println(e.getKey().getName() + " win rate = " + winRate + "%");
        }
        draw = draw / numGames * 100.0D;
        LOGGER.warn("Draw={} %", draw);
        System.out.println("Draw = " + draw + "%");

        final long end = System.nanoTime();
        //1 second = 1__000__000__000 nano seconds
        final double elapsedTimeInSecond = (double) (end - start) / 1__000__000__000;
        LOGGER.warn("Игра длилась {}", elapsedTimeInSecond);
        System.out.println("Игра длилась " + elapsedTimeInSecond);
    }
}

