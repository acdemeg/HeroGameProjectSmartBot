package com.neolab.heroesGame.smartBots;

import com.neolab.heroesGame.aditional.CommonFunction;
import com.neolab.heroesGame.arena.Army;
import com.neolab.heroesGame.arena.BattleArena;
import com.neolab.heroesGame.arena.SquareCoordinate;
import com.neolab.heroesGame.client.ai.Player;
import com.neolab.heroesGame.client.ai.PlayerBot;
import com.neolab.heroesGame.errors.HeroExceptions;
import com.neolab.heroesGame.heroes.Hero;
import com.neolab.heroesGame.server.answers.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SmartBotBase extends Player {
    private static final Logger LOGGER = LoggerFactory.getLogger(SmartBotBase.class);
    protected int totalNodes;
    protected int terminalNodes;
    protected int roundCounter;
    protected int playerId;
    protected int enemyId;
    protected int depth;
    protected int previousDepth;
    protected boolean isLogging = false;
    protected Player randomBot;
    protected Map<Integer, Integer> mapDepthRecursionRoundNumber;
    protected static final double EPS = 1.0e-9;

    public SmartBotBase(final int id, final String name) {
        super(id, name);
    }

    public Answer getAnswer(final BattleArena board) throws IOException, HeroExceptions { return null; };

    protected WinCollector getAnswerByGameTree(final BattleArena board) throws HeroExceptions, IOException { return null; }

    protected void initializeFullSimulation(final BattleArena board){
        mapDepthRecursionRoundNumber = new HashMap<>();
        depth = 0;
        previousDepth = 0;
        roundCounter = 0;
        terminalNodes = 0;
        totalNodes = 0;
        playerId = this.getId();
        enemyId = getEnemyId(board);
        randomBot = new PlayerBot(enemyId, "randomBot");
        if(isLogging){
            LOGGER.info("******************************* Начинается симуляция *************************************");
            System.out.println("******************************* Начинается симуляция *************************************");
        }
    }

    protected void printResultsFullSimulation(long startTime) throws IOException {
       /* System.out.println("Total nodes = " + totalNodes);
        System.out.println("Terminal nodes = " + terminalNodes);
        System.out.println("Round counter = " + roundCounter);
        System.out.println("Time answer = " + (System.currentTimeMillis() - startTime));
        System.out.println();*/

        if(isLogging){
            LOGGER.info("******************************* Конец симуляции *************************************");
            System.out.println("******************************* Конец симуляции *************************************");
            System.out.println();
        }
    }

    protected WinnerType someOneWhoWin(final BattleArena battleArena) {
        for(int armyId : battleArena.getArmies().keySet()){
            if(battleArena.getArmy(armyId).getHeroes().isEmpty()){
                int playerId = (this.getId() == armyId) ? enemyId : this.getId();
                WinnerType winnerType = (playerId == this.getId()) ? WinnerType.SMART_BOT : WinnerType.ENEMY_BOT;
                if(isLogging){
                    battleArena.toLog();
                    LOGGER.info("Игрок<{}> выиграл это тяжкое сражение", playerId);
                }
                return winnerType;
            }
        }
        return WinnerType.NONE;
    }

    protected Integer getEnemyId(final  BattleArena battleArena){
        return battleArena.getArmies().keySet().stream().filter(id -> id != this.getId()).findFirst().get();
    }

    protected void toLogActionEffect(final BoardUpdater boardUpdater){
        if(isLogging){
            boardUpdater.getActionEffect().toLog();
        }
    }

    protected void toLogBoard(final BattleArena board){
        if(isLogging){
            board.toLog();
        }
    }

    protected void toLogAdditionalInfo(){
        if(isLogging){
            LOGGER.info("");
            LOGGER.info("Round {}       Depth {}   TotalNodes {}",  roundCounter, depth, totalNodes);
        }
    }

    protected void toLogAnswer(final Answer answer){
        if(isLogging){
            answer.toLog();
        }
    }

    protected void toLogInfo(final String string, final Integer arg){
        if(isLogging){
            if(arg != null){
                LOGGER.info(string, arg);
            }
            else LOGGER.info(string);
        }
    }

    protected void changeSmartAndRandomPlayers() {
        playerId = (playerId == this.getId()) ? enemyId : this.getId();
    }

    protected Set<SquareCoordinate> getAvailableHeroes(final BattleArena battleArena){
        return battleArena.getArmy(playerId).getAvailableHeroes().keySet();
    }

    protected Set<SquareCoordinate> getAvailableTargets(BattleArena battleArena, SquareCoordinate heroCoord) {
        final Hero activeHero = getActiveHero(battleArena, heroCoord);
        if(CommonFunction.isUnitArcher(activeHero) || CommonFunction.isUnitMagician(activeHero)){
            return getEnemyHeroes(battleArena);
        }
        if(CommonFunction.isUnitHealer(activeHero)){
            return battleArena.getArmy(playerId).getHeroes().keySet();
        }
        else {
            Army enemyArmy = getEnemyArmy(battleArena);
            return CommonFunction.getCorrectTargetForFootman(heroCoord, enemyArmy);
        }
    }

    protected Hero getActiveHero(final BattleArena battleArena, final SquareCoordinate heroCoord){
        return battleArena.getArmy(playerId).getHeroes().get(heroCoord);
    }

    protected Set<SquareCoordinate> getEnemyHeroes(final BattleArena battleArena){
        Army enemyArmy = getEnemyArmy(battleArena);
        return enemyArmy.getHeroes().keySet();
    }

    protected Army getEnemyArmy(final BattleArena battleArena){
        return battleArena.getArmy((playerId == enemyId) ? this.getId() : enemyId);
    }


    protected boolean checkCanMove(final Integer id, final BattleArena battleArena) {
        return !battleArena.haveAvailableHeroByArmyId(id);
    }

    protected AnswerAndWin receiveRatingFromNode(
            final BattleArena board, int activePlayerId, int waitingPlayerId, Answer answer) throws IOException, HeroExceptions {
        toLogBoard(board);
        final BattleArena copy = board.getCopy();
        final BoardUpdater boardUpdater = new BoardUpdater(activePlayerId, waitingPlayerId, copy);
        toLogAnswer(answer);
        boardUpdater.toAct(answer);
        toLogActionEffect(boardUpdater);
        changeSmartAndRandomPlayers();
        totalNodes++;
        depth++;
        WinCollector winCollector = getAnswerByGameTree(boardUpdater.getBoard());
        return new AnswerAndWin(answer, winCollector);
    }

    protected WinCollector distributeWin(final WinnerType winnerType) {
        terminalNodes++;
        final WinCollector result;
        if (!winnerType.isTerminalWinnerType()) {
            throw new IllegalStateException(winnerType + " is not terminal WinnerType");
        }
        if (winnerType == WinnerType.DRAW) {
            result = new WinCollector(0.0D, 0.5D, 0.0D, 0.0D, 1.0D);
        } else if (winnerType == WinnerType.SMART_BOT) {
            result = new WinCollector(1.0D, 0.0D, 0.0D, 1.0D, 0.0D);
        } else {
            result = new WinCollector(0.0D, 0.0D, 1.0D, 0.0D, 0.0D);
        }
        return result.catchBrokenProbabilities();
    }

    public String getStringArmyFirst(final int armySize) {
        //final List<String> armies = CommonFunction.getAllAvailableArmiesCode(armySize);
        //return armies.get(RANDOM.nextInt(armies.size()));
        return "   fFf";
    }

    public String getStringArmySecond(final int armySize, final Army army) {
        return getStringArmyFirst(armySize);
    }
}

