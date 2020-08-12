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
import java.util.stream.Collectors;

import static com.neolab.heroesGame.smartBots.SelfPlay.MAX_ROUND;

public class SmartBotMaxMax extends SmartBotBase {

    public SmartBotMaxMax(final int id, final String name) {
        super(id, name);
    }

    @Override
    public Answer getAnswer(final BattleArena board) throws IOException, HeroExceptions {

        final long startTime = System.currentTimeMillis();
        initializeFullSimulation(board);

        final AnswerAndWin aw = runSimulationForSmartBot(board, WinCollector::getTotalWin);

        printResultsFullSimulation(startTime);
        return aw.answer;
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
            final AnswerAndWin aw = runSimulationForSmartBot(board, WinCollector::getTotalWin);
            awList.add(aw);
        }
        else {
            winCalculator = w -> 1.0D - w.getTotalWin();
            final AnswerAndWin aw = runSimulationForEnemyBot(board,  w -> 1.0D - w.getTotalWin());
            awList.add(aw);
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

    private AnswerAndWin runSimulationForSmartBot(final BattleArena board, final ToDoubleFunction<WinCollector> winCalculator) throws IOException, HeroExceptions {
        final List<AnswerAndWin> awList = new ArrayList<>();
        final Set<SquareCoordinate> availableHeroes = getAvailableHeroes(board);
        for(SquareCoordinate heroCoord : availableHeroes){
            playerId = this.getId();

            for(Enum<HeroActions> action : HeroActions.values()){
                playerId = this.getId();

                final Hero activeHero = getActiveHero(board, heroCoord);

                if(action == HeroActions.DEFENCE){
                    final AnswerAndWin aw = receiveRatingFromNode(board, this.getId(), enemyId,
                            new Answer(heroCoord, HeroActions.DEFENCE, new SquareCoordinate(-1,-1), this.getId()));
                    awList.add(aw);
                }

                if(action == HeroActions.HEAL){
                    if(CommonFunction.isUnitHealer(activeHero)){
                        final Set<SquareCoordinate> availableTargetsForHeal = getAvailableTargets(board, heroCoord);
                        for(SquareCoordinate target : availableTargetsForHeal){
                            playerId = this.getId();
                            final AnswerAndWin aw = receiveRatingFromNode(board, this.getId(), enemyId,
                                    new Answer(heroCoord, HeroActions.HEAL, target, this.getId()));
                            awList.add(aw);
                        }
                    }
                    continue;
                }

                if(action == HeroActions.ATTACK){
                    if(!CommonFunction.isUnitHealer(activeHero)){
                        final Set<SquareCoordinate> availableTargetsForAttack = getAvailableTargets(board, heroCoord);
                        for(SquareCoordinate target : availableTargetsForAttack){
                            playerId = this.getId();
                            final AnswerAndWin aw = receiveRatingFromNode(board, this.getId(), enemyId,
                                    new Answer(heroCoord, HeroActions.ATTACK, target, this.getId()));
                            awList.add(aw);
                        }
                    }
                }
            }
        }
        return getGreedyDecision(awList, winCalculator);
    }

    private AnswerAndWin runSimulationForEnemyBot(final BattleArena board, final ToDoubleFunction<WinCollector> winCalculator) throws IOException, HeroExceptions {
        return receiveRatingFromNode(board, enemyId, this.getId(), randomBot.getAnswer(board));
//        final List<AnswerAndWin> awList = new ArrayList<>();
//        final Set<SquareCoordinate> availableHeroes = getAvailableHeroes(board);
//        for(SquareCoordinate heroCoord : availableHeroes){
//            playerId = enemyId;
//
//            for(Enum<HeroActions> action : HeroActions.values()){
//                playerId = enemyId;
//                final Hero activeHero = getActiveHero(board, heroCoord);
//
//                if(action == HeroActions.DEFENCE){
//                    final AnswerAndWin aw = receiveRatingFromNode(board, enemyId, this.getId(),
//                            new Answer(heroCoord, HeroActions.DEFENCE, new SquareCoordinate(-1,-1), enemyId));
//                    awList.add(aw);
//                }
//
//                if(action == HeroActions.HEAL){
//                    if(CommonFunction.isUnitHealer(activeHero)){
//                        final Set<SquareCoordinate> availableTargetsForHeal = getAvailableTargets(board, heroCoord);
//                        for(SquareCoordinate target : availableTargetsForHeal){
//                            playerId = enemyId;
//                            final AnswerAndWin aw = receiveRatingFromNode(board, enemyId, this.getId(),
//                                    new Answer(heroCoord, HeroActions.HEAL, target, enemyId));
//                            awList.add(aw);
//                        }
//                    }
//                    continue;
//                }
//
//                if(action == HeroActions.ATTACK){
//                    if(!CommonFunction.isUnitHealer(activeHero)){
//                        final Set<SquareCoordinate> availableTargetsForAttack = getAvailableTargets(board, heroCoord);
//                        for(SquareCoordinate target : availableTargetsForAttack){
//                            playerId = enemyId;
//                            final AnswerAndWin aw = receiveRatingFromNode(board, enemyId, this.getId(),
//                                    new Answer(heroCoord, HeroActions.ATTACK, target, enemyId));
//                            awList.add(aw);
//                        }
//                    }
//                }
//            }
//        }
//        return getGreedyDecision(awList, winCalculator);
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
        final List<AnswerAndWin> attackWin = potentialWin.stream().filter(
                win -> win.answer.getAction().equals(HeroActions.ATTACK))
                .collect(Collectors.toList());

        return (attackWin.isEmpty()) ? potentialWin.get(0) : attackWin.get(0);
    }
}


