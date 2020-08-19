//package com.neolab.heroesGame.smartBots;
//
//import com.neolab.heroesGame.aditional.CommonFunction;
//import com.neolab.heroesGame.arena.Army;
//import com.neolab.heroesGame.arena.BattleArena;
//import com.neolab.heroesGame.arena.SquareCoordinate;
//import com.neolab.heroesGame.enumerations.HeroActions;
//import com.neolab.heroesGame.errors.HeroExceptions;
//import com.neolab.heroesGame.heroes.Hero;
//import com.neolab.heroesGame.server.answers.Answer;
//
//import java.io.IOException;
//import java.util.*;
//import java.util.function.ToDoubleFunction;
//import java.util.stream.Collectors;
//
//import static com.neolab.heroesGame.smartBots.SelfPlay.MAX_ROUND;
//
//public class MonteCarloEvaluator extends SmartBotBase{
//
//    private final Random rnd = new Random();
//
//    public MonteCarloEvaluator(final int botId, final String botName){
//        super(botId, botName);;
//    }
//
//    public WinCollector evaluateWin(final BattleArena board,
//                                    final int activePlayerId,
//                                    final int thisId,
//                                    final int enemyId,
//                                    final WinnerType expectedWinnerType) throws IOException, HeroExceptions {
//        double win = 0.0D;
//        double drawWin = 0.0D;
//        double looseWin = 0.0D;
//        double pWin = 0.0D;
//        double pDraw = 0.0D;
//        double pLoose = 0.0D;
//        int playerId = activePlayerId;
//
//        for (int i = 0; i < 100; i++) {
//            boolean continueGame = true;
//            while (continueGame) {
//                initializeFullSimulation(board);
//
//                final AnswerAndWin aw = runSimulationForSmartBot(board, WinCollector::getTotalWin);
//
//            }
//        }
//        return new WinCollector(
//            win / 100,
//            drawWin / 100,
//            looseWin / 100,
//            pLoose / 100,
//            pWin / 100);
//    }
//
//    @Override
//    protected WinCollector getAnswerByGameTree(final BattleArena board) throws HeroExceptions, IOException {
//
//        WinCollector winCollector = prepareStepDippingInTree(board);
//
//        if(winCollector != null){
//            return winCollector;
//        }
//
//        final List<AnswerAndWin> awList = new ArrayList<>();
//        ToDoubleFunction<WinCollector> winCalculator;
//
//        if(!checkCanMove(playerId, board)){
//            changeSmartAndRandomPlayers();
//        }
//
//        if(playerId == this.getId()){
//            final AnswerAndWin aw = runSimulationForSmartBot(board);
//        }
//        else {
//            winCalculator = w -> -w.getTotalWin();
//            final AnswerAndWin aw = runSimulationForEnemyBot(board);
//        }
//
//        depth--;
//        return greedyDecision.winCollector.catchBrokenProbabilities();
//    }
//
//    private WinCollector prepareStepDippingInTree(final BattleArena board){
//        if(depth > previousDepth){
//            mapDepthRecursionRoundNumber.put(depth, roundCounter);
//        }
//        previousDepth = depth;
//        WinnerType winnerType = someOneWhoWin(board);
//        if (winnerType != WinnerType.NONE) {
//            changeSmartAndRandomPlayers();
//            depth--;
//            return distributeWin(winnerType);
//        }
//        if (depth >= maxRecLevel) {
//            final WinCollector termNodeWinCollector = distributeMaxRecNode(board);
//            return termNodeWinCollector.catchBrokenProbabilities();
//        }
//        if (!board.canSomeoneAct()) {
//            if(mapDepthRecursionRoundNumber.size() == depth){
//                mapDepthRecursionRoundNumber.put(depth, (roundCounter > MAX_ROUND) ? roundCounter : ++roundCounter);
//            }
//            else {
//                mapDepthRecursionRoundNumber.entrySet().removeIf(item -> item.getKey() > depth);
//                roundCounter = mapDepthRecursionRoundNumber.get(depth);
//            }
//
//            if (mapDepthRecursionRoundNumber.get(depth) > MAX_ROUND){
//                toLogInfo("Поединок закончился ничьей", null);
//                depth--;
//                return distributeWin(WinnerType.DRAW);
//            }
//            toLogInfo("-----------------Начинается раунд <{}>---------------", roundCounter);
//            board.endRound();
//        }
//        return null;
//    }
//
//    private AnswerAndWin runSimulationForSmartBot(final BattleArena board, final ToDoubleFunction<WinCollector> winCalculator) throws IOException, HeroExceptions {
//        final List<AnswerAndWin> awList = new ArrayList<>();
//        final Set<SquareCoordinate> availableHeroes = getAvailableHeroes(board);
//        for(SquareCoordinate heroCoord : availableHeroes){
//            playerId = this.getId();
//
//            for(Enum<HeroActions> action : HeroActions.values()){
//                playerId = this.getId();
//
//                final Hero activeHero = getActiveHero(board, heroCoord);
//
//                if(action == HeroActions.DEFENCE){
//                    final AnswerAndWin aw = receiveRatingFromNode(board, this.getId(), enemyId,
//                            new Answer(heroCoord, HeroActions.DEFENCE, new SquareCoordinate(-1,-1), this.getId()));
//                    awList.add(aw);
//                }
//
//                if(action == HeroActions.HEAL){
//                    if(CommonFunction.isUnitHealer(activeHero)){
//                        final Set<SquareCoordinate> availableTargetsForHeal = getAvailableTargets(board, heroCoord);
//                        for(SquareCoordinate target : availableTargetsForHeal){
//                            playerId = this.getId();
//                            final AnswerAndWin aw = receiveRatingFromNode(board, this.getId(), enemyId,
//                                    new Answer(heroCoord, HeroActions.HEAL, target, this.getId()));
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
//                            playerId = this.getId();
//                            final AnswerAndWin aw = receiveRatingFromNode(board, this.getId(), enemyId,
//                                    new Answer(heroCoord, HeroActions.ATTACK, target, this.getId()));
//                            awList.add(aw);
//                        }
//                    }
//                }
//            }
//        }
//        return getGreedyDecision(awList, winCalculator);
//    }
//
//    private AnswerAndWin runSimulationForEnemyBot(final BattleArena board, final ToDoubleFunction<WinCollector> winCalculator) throws IOException, HeroExceptions {
//        return receiveRatingFromNode(board, enemyId, this.getId(), randomBot.getAnswer(board));
//    }
//
//    private AnswerAndWin getGreedyDecision(final List<AnswerAndWin> awList, final ToDoubleFunction<WinCollector> winCalculator) {
//        if (awList.size() == 1) {
//            return awList.get(0);
//        }
//        final List<AnswerAndWin> potentialWin = new ArrayList<>();
//        potentialWin.add(awList.get(0));
//        for (final AnswerAndWin tmpDecision : awList.subList(1, awList.size())) {
//            final double bestWin = winCalculator.applyAsDouble(potentialWin.get(0).winCollector);
//            final double tmpWin = winCalculator.applyAsDouble(tmpDecision.winCollector);
//            if (bestWin < tmpWin) {
//                potentialWin.clear();
//                potentialWin.add(tmpDecision);
//            } else if (Math.abs(bestWin - tmpWin) < EPS) {
//                potentialWin.add(tmpDecision);
//            }
//        }
//        final List<AnswerAndWin> attackWin = potentialWin.stream().filter(
//                win -> win.answer.getAction().equals(HeroActions.ATTACK))
//                .collect(Collectors.toList());
//
//        return (attackWin.isEmpty()) ? potentialWin.get(0) : attackWin.get(0);
//    }
//
//    private WinCollector distributeMaxRecNode(BattleArena board) {
//        double differenceRating = (getRatingArmy(board,this.getId()) - getRatingArmy(board, enemyId))/10_000;
//        double normalizeValue = 1/(1 + Math.exp(-differenceRating));
//        return new WinCollector(normalizeValue, 0,1.0D - normalizeValue,normalizeValue,0);
//    }
//
//    private double getRatingArmy(BattleArena board, int id){
//        Army army = board.getArmy(id);
//        double rating = 0;
//        for(Hero hero : army.getHeroes().values()){
//            rating += hero.getHp() * hero.getDamage() * hero.getPrecision();
//        }
//        return  rating;
//    }
//}
//
