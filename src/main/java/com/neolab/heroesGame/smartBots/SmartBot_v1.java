package com.neolab.heroesGame.smartBots;

import com.neolab.heroesGame.aditional.CommonFunction;
import com.neolab.heroesGame.arena.Army;
import com.neolab.heroesGame.arena.BattleArena;
import com.neolab.heroesGame.arena.SquareCoordinate;
import com.neolab.heroesGame.client.ai.Player;
import com.neolab.heroesGame.client.ai.PlayerBot;
import com.neolab.heroesGame.enumerations.HeroActions;
import com.neolab.heroesGame.errors.HeroExceptions;
import com.neolab.heroesGame.heroes.Hero;
import com.neolab.heroesGame.server.answers.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.ToDoubleFunction;

import static com.neolab.heroesGame.smartBots.SelfPlay.MAX_ROUND;

public class SmartBot_v1 extends Player {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmartBot_v1.class);
    private final boolean isLogging = true;
    //id активного игрока, сначала всегда активны мы
    private int playerId;
    private int enemyId;
    private int depth;
    private int previousDepth;
    private Map<Integer, Integer> mapDepthRecursionRoundNumber;

    public SmartBot_v1(final int id, final String name) {
        super(id, name);
    }

    @Override
    public Answer getAnswer(final BattleArena board) throws HeroExceptions, IOException {
        mapDepthRecursionRoundNumber = new HashMap<>();
        depth = 0;
        previousDepth = 0;
        roundCounter = 0;
        terminalNodes = 0;
        totalNodes = 0;
        playerId = this.getId();
        enemyId = getEnemyId(board);
        final long startTime = System.currentTimeMillis();
        if(isLogging){
            LOGGER.info("******************************* Начинается симуляция *************************************");
        }

        final List<AnswerAndWin> awList = new ArrayList<>();
        final Set<SquareCoordinate> availableHeroes = getAvailableHeroes(board);
        for(SquareCoordinate heroCoord : availableHeroes){

            for(Enum<HeroActions> action : HeroActions.values()){

                final Hero activeHero = getActiveHero(board, heroCoord);

                if(action == HeroActions.DEFENCE){
                    playerId = this.getId();
                    if(isLogging){
                        board.toLog();
                    }
                    final BattleArena copy = board.getCopy();
                    final BoardUpdater boardUpdater = new BoardUpdater(playerId, enemyId, copy);
                    Answer answer = new Answer(heroCoord, HeroActions.DEFENCE, new SquareCoordinate(-1,-1), playerId);
                    answer.toLog();
                    boardUpdater.toAct(answer);
                    if(isLogging){
                        boardUpdater.getActionEffect().toLog();
                    }
                    changeSmartAndRandomPlayers();
                    totalNodes++;
                    depth++;
                    WinCollector winCollector = getAnswerByGameTree(boardUpdater.getBoard());
                    awList.add(new AnswerAndWin(answer, winCollector));
                }

                if(action == HeroActions.HEAL){
                    if(CommonFunction.isUnitHealer(activeHero)){
                        final Set<SquareCoordinate> availableTargetsForHeal = getAvailableTargets(board, heroCoord);
                        for(SquareCoordinate target : availableTargetsForHeal){
                            playerId = this.getId();
                            if(isLogging){
                                board.toLog();
                            }
                            final BattleArena copy = board.getCopy();
                            final BoardUpdater boardUpdater = new BoardUpdater(playerId, enemyId, copy);
                            Answer answer = new Answer(heroCoord, HeroActions.HEAL, target, playerId);
                            if(isLogging){
                                answer.toLog();
                            }
                            boardUpdater.toAct(answer);
                            if(isLogging){
                                boardUpdater.getActionEffect().toLog();
                            }
                            changeSmartAndRandomPlayers();
                            totalNodes++;
                            depth++;
                            WinCollector winCollector = getAnswerByGameTree(boardUpdater.getBoard());
                            awList.add(new AnswerAndWin(answer, winCollector));
                        }
                    }
                    continue;
                }

                if(action == HeroActions.ATTACK){
                    if(!CommonFunction.isUnitHealer(activeHero)){
                        final Set<SquareCoordinate> availableTargetsForAttack = getAvailableTargets(board, heroCoord);
                        for(SquareCoordinate target : availableTargetsForAttack){
                            playerId = this.getId();
                            if(isLogging){
                                board.toLog();
                            }
                            final BattleArena copy = board.getCopy();
                            final BoardUpdater boardUpdater = new BoardUpdater(playerId, enemyId, copy);
                            Answer answer = new Answer(heroCoord, HeroActions.ATTACK, target, playerId);
                            if(isLogging){
                                answer.toLog();
                            }
                            boardUpdater.toAct(answer);
                            if(isLogging){
                                boardUpdater.getActionEffect().toLog();
                            }
                            changeSmartAndRandomPlayers();
                            totalNodes++;
                            depth++;
                            WinCollector winCollector = getAnswerByGameTree(boardUpdater.getBoard());
                            awList.add(new AnswerAndWin(answer, winCollector));
                        }
                    }
                }
            }
        }

        System.out.println("Total nodes = " + totalNodes);
        System.out.println("Terminal nodes = " + terminalNodes);
        System.out.println("Round counter = " + roundCounter);
        System.out.println("Time answer = " + (System.currentTimeMillis() - startTime));
        System.out.println();

        return getGreedyDecision(awList, WinCollector::getTotalWin).answer;
    }

    public String getStringArmyFirst(final int armySize) {
        //final List<String> armies = CommonFunction.getAllAvailableArmiesCode(armySize);
        //return armies.get(RANDOM.nextInt(armies.size()));
        return "   fFf";
    }

    public String getStringArmySecond(final int armySize, final Army army) {
        return getStringArmyFirst(armySize);
    }

    /********************************************************************************************************************/

     private WinCollector getAnswerByGameTree(final BattleArena board) throws HeroExceptions, IOException {
         if(depth > previousDepth){
             mapDepthRecursionRoundNumber.put(depth, roundCounter);
         }
         previousDepth = depth;
         WinnerType winnerType = someOneWhoWin(board);
         if (winnerType != WinnerType.NONE) {
             changeSmartAndRandomPlayers();
             depth--;
             return distributeWin(winnerType);
         }
         if (!board.canSomeoneAct()) {
             //todo разобраться с counter
             if(mapDepthRecursionRoundNumber.size() == depth){
                 mapDepthRecursionRoundNumber.put(depth, (roundCounter > MAX_ROUND) ? roundCounter : ++roundCounter);
             }
             else {
                 mapDepthRecursionRoundNumber.entrySet().removeIf(item -> item.getKey() > depth);
                 roundCounter = mapDepthRecursionRoundNumber.get(depth);
             }

             if (mapDepthRecursionRoundNumber.get(depth) > MAX_ROUND){
                 if(isLogging){
                     LOGGER.info("Поединок закончился ничьей");
                 }
                 depth--;
                 return distributeWin(WinnerType.DRAW);
             }
             if(isLogging){
                 LOGGER.info("-----------------Начинается раунд <{}>---------------", roundCounter);
             }
             board.endRound();
         }

         final List<AnswerAndWin> awList = new ArrayList<>();
         ToDoubleFunction<WinCollector> winCalculator;

         if(!checkCanMove(playerId, board)){
             changeSmartAndRandomPlayers();
         }

         if(playerId == this.getId()){
             winCalculator = WinCollector::getTotalWin;
             final Set<SquareCoordinate> availableHeroes = getAvailableHeroes(board);
             for(SquareCoordinate heroCoord : availableHeroes){

                 for(Enum<HeroActions> action : HeroActions.values()){

                     final Hero activeHero = getActiveHero(board, heroCoord);

                     if(action == HeroActions.DEFENCE){
                         playerId = this.getId();
                         if(isLogging){
                             board.toLog();
                         }
                         final BattleArena copy = board.getCopy();
                         final BoardUpdater boardUpdater = new BoardUpdater(playerId, enemyId, copy);
                         Answer answer = new Answer(heroCoord, HeroActions.DEFENCE, new SquareCoordinate(-1,-1), playerId);
                         if(isLogging){
                            answer.toLog();
                         }
                         boardUpdater.toAct(answer);
                         if(isLogging){
                             boardUpdater.getActionEffect().toLog();
                         }
                         changeSmartAndRandomPlayers();
                         totalNodes++;
                         depth++;
                         WinCollector winCollector = getAnswerByGameTree(boardUpdater.getBoard());
                         awList.add(new AnswerAndWin(answer, winCollector));
                     }

                     if(action == HeroActions.HEAL){
                         if(CommonFunction.isUnitHealer(activeHero)){
                             final Set<SquareCoordinate> availableTargetsForHeal = getAvailableTargets(board, heroCoord);
                             for(SquareCoordinate target : availableTargetsForHeal){
                                 playerId = this.getId();
                                 if(isLogging){
                                    board.toLog();
                                 }
                                 final BattleArena copy = board.getCopy();
                                 final BoardUpdater boardUpdater = new BoardUpdater(playerId, enemyId, copy);
                                 Answer answer = new Answer(heroCoord, HeroActions.HEAL, target, playerId);
                                 if(isLogging){
                                    answer.toLog();
                                 }
                                 boardUpdater.toAct(answer);
                                 if(isLogging){
                                     boardUpdater.getActionEffect().toLog();
                                 }
                                 changeSmartAndRandomPlayers();
                                 totalNodes++;
                                 depth++;
                                 WinCollector winCollector = getAnswerByGameTree(boardUpdater.getBoard());
                                 awList.add(new AnswerAndWin(answer, winCollector));
                             }
                         }
                         continue;
                     }

                     if(action == HeroActions.ATTACK){
                         if(!CommonFunction.isUnitHealer(activeHero)){
                             final Set<SquareCoordinate> availableTargetsForAttack = getAvailableTargets(board, heroCoord);
                             for(SquareCoordinate target : availableTargetsForAttack){
                                 playerId = this.getId();
                                 if(isLogging){
                                    board.toLog();
                                 }
                                 final BattleArena copy = board.getCopy();
                                 final BoardUpdater boardUpdater = new BoardUpdater(playerId, enemyId, copy);
                                 Answer answer = new Answer(heroCoord, HeroActions.ATTACK, target, playerId);
                                 if(isLogging){
                                     answer.toLog();
                                 }
                                 boardUpdater.toAct(answer);
                                 if(isLogging){
                                     boardUpdater.getActionEffect().toLog();
                                 }
                                 changeSmartAndRandomPlayers();
                                 totalNodes++;
                                 depth++;
                                 WinCollector winCollector = getAnswerByGameTree(boardUpdater.getBoard());
                                 awList.add(new AnswerAndWin(answer, winCollector));
                             }
                         }
                     }
                 }
             }
         }
         else {
             if(isLogging){
                 board.toLog();
             }
             winCalculator = w -> 1.0D - w.getTotalWin();
             final BattleArena copy = board.getCopy();
             final BoardUpdater boardUpdater = new BoardUpdater(playerId, this.getId(), copy);
             //заглушка на решение соперника
             Player randomBot = new PlayerBot(enemyId, "randomBot");
             Answer randomAnswer = randomBot.getAnswer(copy);
             if(isLogging){
                 randomAnswer.toLog();
             }
             boardUpdater.toAct(randomAnswer);
             if(isLogging){
                 boardUpdater.getActionEffect().toLog();
             }
             changeSmartAndRandomPlayers();
             totalNodes++;
             depth++;
             WinCollector winCollector = getAnswerByGameTree(boardUpdater.getBoard());
             awList.add(new AnswerAndWin(randomAnswer, winCollector));
         }
         if(isLogging){
             LOGGER.info("******************************* Конец симуляции *************************************");
         }
         final AnswerAndWin greedyDecision = getGreedyDecision(awList, winCalculator);
         depth--;
         return greedyDecision.winCollector.catchBrokenProbabilities();
     }

    private AnswerAndWin getGreedyDecision(final List<AnswerAndWin> awList,
                                           final ToDoubleFunction<WinCollector> winCalculator) {
        if (awList.size() == 1) {
            return awList.get(0);
        }
        final List<AnswerAndWin> potentialWin = new ArrayList<>();
        potentialWin.add(awList.get(0));
        for (final AnswerAndWin tmpDecision : awList.subList(1, awList.size())) {
            final double bestWin = winCalculator.applyAsDouble(potentialWin.get(0).winCollector);
            final double tmpWin = winCalculator.applyAsDouble(tmpDecision.winCollector);
            if (bestWin < tmpWin) {
                potentialWin.clear();
                potentialWin.add(tmpDecision);
            } else if (Math.abs(bestWin - tmpWin) < EPS) {
                potentialWin.add(tmpDecision);
            }
        }
        final int idx = new Random().nextInt(potentialWin.size());
        return potentialWin.get(idx);
    }

    protected WinCollector distributeWin(final WinnerType winnerType) {
        terminalNodes++;
        totalNodes++;
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

    private void changeSmartAndRandomPlayers() {
        playerId = (playerId == this.getId()) ? enemyId : this.getId();
    }

    private Set<SquareCoordinate> getAvailableTargets(BattleArena battleArena, SquareCoordinate heroCoord) {
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

    private Set<SquareCoordinate> getAvailableHeroes(final BattleArena battleArena){
         return battleArena.getArmy(playerId).getAvailableHeroes().keySet();
     }

     private Hero getActiveHero(final BattleArena battleArena, final SquareCoordinate heroCoord){
         return battleArena.getArmy(playerId).getHeroes().get(heroCoord);
     }

     private Set<SquareCoordinate> getEnemyHeroes(final BattleArena battleArena){
         Army enemyArmy = getEnemyArmy(battleArena);
         return enemyArmy.getHeroes().keySet();
     }

     private Army getEnemyArmy(final BattleArena battleArena){
         return battleArena.getArmy(enemyId);
     }

     private Integer getEnemyId(final  BattleArena battleArena){
         return battleArena.getArmies().keySet().stream().filter(id -> id != this.getId()).findFirst().get();
     }

     private WinnerType someOneWhoWin(final BattleArena battleArena) {
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

     private boolean checkCanMove(final Integer id, final BattleArena battleArena) {
        return !battleArena.haveAvailableHeroByArmyId(id);
     }
}
