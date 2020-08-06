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

import static com.neolab.heroesGame.smartBots.SelfPlay.MAX_ROUND;

public class SmartBot_v1 extends Player {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmartBot_v1.class);
    //id активного игрока, сначала всегда активны мы
    private int playerId = this.getId();
    private int enemyId;

    public SmartBot_v1(final int id, final String name) {
        super(id, name);
    }

    @Override
    public Answer getAnswer(final BattleArena board) throws HeroExceptions, IOException {
        terminalNodes = 0;
        totalNodes = 0;
        final long startTime = System.currentTimeMillis();
        enemyId = getEnemyId(board);
        Answer answer = getAnswerByGameTree(board);

        System.out.println("Total nodes = " + totalNodes);
        System.out.println("Terminal nodes = " + terminalNodes);

        return answer;
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

     private Answer getAnswerByGameTree(final BattleArena board) throws HeroExceptions, IOException {
         if (totalNodes == 1_000_000){
             System.out.println("Total nodes = 1_000_000");
         }
         LOGGER.info("******************************* Начинается симуляция *************************************");
         BattleArena battleArena = board;
         int roundCounter = 0;
         if (someOneWhoWin(battleArena)) {
             terminalNodes++;
             totalNodes++;
             return null;
         }
         if (!battleArena.canSomeoneAct()) {
             roundCounter++;
             if (roundCounter > MAX_ROUND) {
                 LOGGER.info("Поединок закончился ничьей");
                 terminalNodes++;
                 totalNodes++;
                 return null;
             }
             LOGGER.info("-----------------Начинается раунд <{}>---------------", roundCounter);
             battleArena.endRound();
         }
         if (checkCanMove(playerId, battleArena)) {
             if(playerId == this.getId()){
                 final Set<SquareCoordinate> availableHeroes = getAvailableHeroes(battleArena);

                 for(SquareCoordinate heroCoord : availableHeroes){

                     for(Enum<HeroActions> action : HeroActions.values()){

                         final Hero activeHero = getActiveHero(battleArena, heroCoord);

                         if(action == HeroActions.DEFENCE){
                             battleArena.toLog();
                             final BattleArena copy = battleArena.getCopy();
                             final BoardUpdater boardUpdater = new BoardUpdater(playerId, enemyId, copy);
                             Answer answer = new Answer(heroCoord, HeroActions.DEFENCE, new SquareCoordinate(-1,-1), playerId);
                             answer.toLog();
                             boardUpdater.toAct(answer);
                             boardUpdater.getActionEffect().toLog();
                             battleArena = boardUpdater.getBoard();
                             changeSmartAndRandomPlayers();
                             totalNodes++;
                             getAnswerByGameTree(battleArena);
                         }

                         if(action == HeroActions.HEAL){
                             if(CommonFunction.isUnitHealer(activeHero)){
                                 final Set<SquareCoordinate> availableTargetsForHeal = getAvailableTargets(battleArena, heroCoord);
                                 for(SquareCoordinate target : availableTargetsForHeal){
                                     battleArena.toLog();
                                     final BattleArena copy = battleArena.getCopy();
                                     final BoardUpdater boardUpdater = new BoardUpdater(playerId, enemyId, copy);
                                     Answer answer = new Answer(heroCoord, HeroActions.HEAL, target, playerId);
                                     answer.toLog();
                                     boardUpdater.toAct(answer);
                                     boardUpdater.getActionEffect().toLog();
                                     battleArena = boardUpdater.getBoard();
                                     changeSmartAndRandomPlayers();
                                     totalNodes++;
                                     getAnswerByGameTree(battleArena);
                                 }
                             }
                             continue;
                         }

                         if(action == HeroActions.ATTACK){
                             if(!CommonFunction.isUnitHealer(activeHero)){
                                 final Set<SquareCoordinate> availableTargetsForAttack = getAvailableTargets(battleArena, heroCoord);
                                 for(SquareCoordinate target : availableTargetsForAttack){
                                     battleArena.toLog();
                                     final BattleArena copy = battleArena.getCopy();
                                     final BoardUpdater boardUpdater = new BoardUpdater(playerId, enemyId, copy);
                                     Answer answer = new Answer(heroCoord, HeroActions.ATTACK, target, playerId);
                                     answer.toLog();
                                     boardUpdater.toAct(answer);
                                     boardUpdater.getActionEffect().toLog();
                                     battleArena = boardUpdater.getBoard();
                                     changeSmartAndRandomPlayers();
                                     totalNodes++;
                                     getAnswerByGameTree(battleArena);
                                 }
                             }
                         }
                     }
                 }
             }
             else {
                 battleArena.toLog();
                 final BattleArena copy = battleArena.getCopy();
                 final BoardUpdater boardUpdater = new BoardUpdater(playerId, this.getId(), copy);
                 //заглушка на решение соперника
                 Player randomBot = new PlayerBot(enemyId, "randomBot");
                 Answer randomAnswer = randomBot.getAnswer(copy);
                 randomAnswer.toLog();
                 boardUpdater.toAct(randomAnswer);
                 boardUpdater.getActionEffect().toLog();
                 battleArena = boardUpdater.getBoard();
                 changeSmartAndRandomPlayers();
                 totalNodes++;
                 getAnswerByGameTree(battleArena);
             }
         }
         changeSmartAndRandomPlayers();
         getAnswerByGameTree(battleArena);
         LOGGER.info("******************************* Конец симуляции *************************************");
         return null;
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

     private boolean someOneWhoWin(final BattleArena battleArena) {
         for(int armyId : battleArena.getArmies().keySet()){
             if(battleArena.getArmy(armyId).getHeroes().isEmpty()){
                 int playerId = (this.getId() == armyId) ? enemyId : this.getId();
                 battleArena.toLog();
                 LOGGER.info("Игрок<{}> выиграл это тяжкое сражение", playerId);
                 return true;
             }
         }
         return false;
     }

     private boolean checkCanMove(final Integer id, final BattleArena battleArena) {
        return !battleArena.haveAvailableHeroByArmyId(id);
     }
}
