package com.neolab.heroesGame.smartBots;

import com.neolab.heroesGame.aditional.CommonFunction;
import com.neolab.heroesGame.arena.BattleArena;
import com.neolab.heroesGame.arena.SquareCoordinate;
import com.neolab.heroesGame.enumerations.HeroActions;
import com.neolab.heroesGame.errors.HeroExceptions;
import com.neolab.heroesGame.heroes.Hero;
import com.neolab.heroesGame.server.answers.Answer;

import java.io.IOException;
import java.util.*;
import java.util.function.ToDoubleFunction;

import static com.neolab.heroesGame.smartBots.SelfPlay.MAX_ROUND;

public class SmartBotMaxMax extends SmartBotBase {

    public SmartBotMaxMax(final int id, final String name) {
        super(id, name);
    }

    @Override
    public Answer getAnswer(final BattleArena board) throws IOException, HeroExceptions {

        final long startTime = System.currentTimeMillis();
        initializeFullSimulation(board);

        final List<AnswerAndWin> awList = new ArrayList<>();
        runSimulationForSmartBot(board, awList);

        printResultsFullSimulation(startTime);
        return getGreedyDecision(awList, WinCollector::getTotalWin).answer;
    }

    @Override
    protected WinCollector getAnswerByGameTree(final BattleArena board) throws HeroExceptions, IOException {

        WinCollector winCollector = prepareStepDippingInTree(board);

        if(winCollector != null){
            return winCollector;
        }

        final List<AnswerAndWin> awList = new ArrayList<>();
        ToDoubleFunction<WinCollector> winCalculator;

        if(!checkCanMove(playerId, board)){
            changeSmartAndRandomPlayers();
        }

        if(playerId == this.getId()){
            winCalculator = WinCollector::getTotalWin;
            runSimulationForSmartBot(board, awList);
        }
        else {
            winCalculator = w -> 1.0D - w.getTotalWin();
            runSimulationForEnemyBot(board, awList);
        }

        final AnswerAndWin greedyDecision = getGreedyDecision(awList, winCalculator);
        depth--;
        return greedyDecision.winCollector.catchBrokenProbabilities();
    }

    private WinCollector prepareStepDippingInTree(final BattleArena board){
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
            if(mapDepthRecursionRoundNumber.size() == depth){
                mapDepthRecursionRoundNumber.put(depth, (roundCounter > MAX_ROUND) ? roundCounter : ++roundCounter);
            }
            else {
                mapDepthRecursionRoundNumber.entrySet().removeIf(item -> item.getKey() > depth);
                roundCounter = mapDepthRecursionRoundNumber.get(depth);
            }

            if (mapDepthRecursionRoundNumber.get(depth) > MAX_ROUND){
                toLogInfo("Поединок закончился ничьей", null);
                depth--;
                return distributeWin(WinnerType.DRAW);
            }
            toLogInfo("-----------------Начинается раунд <{}>---------------", roundCounter);
            board.endRound();
        }
        return null;
    }

    private void runSimulationForSmartBot(final BattleArena board, final List<AnswerAndWin> awList) throws IOException, HeroExceptions {
        final Set<SquareCoordinate> availableHeroes = getAvailableHeroes(board);
        for(SquareCoordinate heroCoord : availableHeroes){

            for(Enum<HeroActions> action : HeroActions.values()){

                final Hero activeHero = getActiveHero(board, heroCoord);

                if(action == HeroActions.DEFENCE){
                    receiveRatingFromNode(board, awList, this.getId(), enemyId,
                            new Answer(heroCoord, HeroActions.DEFENCE, new SquareCoordinate(-1,-1), this.getId()));
                }

                if(action == HeroActions.HEAL){
                    if(CommonFunction.isUnitHealer(activeHero)){
                        final Set<SquareCoordinate> availableTargetsForHeal = getAvailableTargets(board, heroCoord);
                        for(SquareCoordinate target : availableTargetsForHeal){
                            receiveRatingFromNode(board, awList, this.getId(), enemyId,
                                    new Answer(heroCoord, HeroActions.HEAL, target, this.getId()));
                        }
                    }
                    continue;
                }

                if(action == HeroActions.ATTACK){
                    if(!CommonFunction.isUnitHealer(activeHero)){
                        final Set<SquareCoordinate> availableTargetsForAttack = getAvailableTargets(board, heroCoord);
                        for(SquareCoordinate target : availableTargetsForAttack){
                            receiveRatingFromNode(board, awList, this.getId(), enemyId,
                                    new Answer(heroCoord, HeroActions.ATTACK, target, this.getId()));
                        }
                    }
                }
            }
        }
    }

    private void runSimulationForEnemyBot(final BattleArena board, final List<AnswerAndWin> awList) throws IOException, HeroExceptions {
        receiveRatingFromNode(board, awList, enemyId, this.getId(), randomBot.getAnswer(board));
    }

    private AnswerAndWin getGreedyDecision(final List<AnswerAndWin> awList, final ToDoubleFunction<WinCollector> winCalculator) {
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
}


