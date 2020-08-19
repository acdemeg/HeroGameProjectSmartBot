package com.neolab.heroesGame.smartBots;

import com.neolab.heroesGame.arena.Army;
import com.neolab.heroesGame.arena.BattleArena;
import com.neolab.heroesGame.client.ai.PlayerBot;
import com.neolab.heroesGame.errors.HeroExceptions;
import com.neolab.heroesGame.server.answers.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import static com.neolab.heroesGame.smartBots.SelfPlay.MAX_ROUND;

public class SmartBotBaseWithGraph extends SmartBotBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(SmartBotBaseWithGraph.class);
    private BufferedWriter dotGraphWriter;
    private boolean isOpenWriter = false;
    private boolean isAllowGraphWrite = false;

    public SmartBotBaseWithGraph(final int id, final String name) {
        super(id, name);
    }

    @Override
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
        try {
            File graph = new File("graph.dot");
            if(graph.exists()){
                if(graph.length() == 0){
                    dotGraphWriter = new BufferedWriter(new FileWriter("graph.dot"));
                    isOpenWriter = true;
                    dotGraphWriter.write("digraph G {\nnode [style=filled, fontsize=10,color=lightblue2];\nedge[penwidth = 3, arrowhead=vee,arrowtail=inv, arrowsize=1,color=maroon,fontsize=10,fontcolor=navy]\n");
                }
            }
            else {
                if(graph.createNewFile())
                    dotGraphWriter = new BufferedWriter(new FileWriter("graph.dot"));
                    isOpenWriter = true;
                    dotGraphWriter.write("digraph G {\nnode [style=filled, fontsize=10,color=lightblue2];\nedge[penwidth = 3, arrowhead=vee,arrowtail=inv, arrowsize=1,color=maroon,fontsize=10,fontcolor=navy]\n");
            }
        }
        catch(IOException ex){
            System.out.println(ex.getMessage());
        }
    }

    @Override
    protected void printResultsFullSimulation(long startTime) throws IOException {
        if(isOpenWriter){
            if(isAllowGraphWrite){
                dotGraphWriter.write("}");
                dotGraphWriter.close();
                isOpenWriter = false;
            }
        }

        System.out.println("Total nodes = " + totalNodes);
        System.out.println("Terminal nodes = " + terminalNodes);
        System.out.println("Round counter = " + roundCounter);
        System.out.println("Time answer = " + (System.currentTimeMillis() - startTime));
        System.out.println();

        if(totalNodes < 100){
            isLogging = true;
            isAllowGraphWrite = true;
        }

        if(isLogging){
            LOGGER.info("******************************* Конец симуляции *************************************");
            System.out.println("******************************* Конец симуляции *************************************");
            System.out.println();
        }
    }

    @Override
    protected AnswerAndWin receiveRatingFromNode(
            final BattleArena board, int activePlayerId, int waitingPlayerId, Answer answer) throws IOException, HeroExceptions {

        printStepByTree(board, false, " -> ");
        toLogAdditionalInfo();
        toLogBoard(board);
        final BattleArena copy = board.getCopy();
        final BoardUpdater boardUpdater = new BoardUpdater(activePlayerId, waitingPlayerId, copy);
        toLogAnswer(answer);
        boardUpdater.toAct(answer);
        toLogActionEffect(boardUpdater);
        changeSmartAndRandomPlayers();
        totalNodes++;
        depth++;

        boolean isRoundEnd = isRoundEnd(boardUpdater.getBoard());
        printStepByTree(boardUpdater.getBoard(), isRoundEnd, ";\n");

        WinCollector winCollector = getAnswerByGameTree(boardUpdater.getBoard());
        return new AnswerAndWin(answer, winCollector);
    }

    private boolean isTerminalNode(final BattleArena battleArena) {
        for(int armyId : battleArena.getArmies().keySet()){
            if(battleArena.getArmy(armyId).getHeroes().isEmpty()){
                return true;
            }
        }

        return roundCounter > MAX_ROUND;
    }

    private void printStepByTree(final BattleArena board, final boolean isRoundEnd, final String suffix) throws IOException {
        if(!isAllowGraphWrite)
            return;
        int round = isRoundEnd ? roundCounter + 1 : roundCounter;
        String nodeInfo = "\"" + "   R=" + round + " D=" + depth + "  TN=" + totalNodes + "\"";
        String node = GraphPrinter.printNode(board, isRoundEnd);
        if(suffix.equals(";\n")){
            writeStringToGraphWriter(node + "[label=" + nodeInfo + "]" + suffix);
        }
        else writeStringToGraphWriter(node + suffix);

        if(suffix.equals(";\n") && isTerminalNode(board)){
            writeStringToGraphWriter(node + "[fillcolor=lightcoral]\n");
        }

    }

    private void writeStringToGraphWriter(String string) throws IOException {
        if(isOpenWriter){
            dotGraphWriter.write(string);
        }
    }

    private boolean isRoundEnd(final BattleArena board){
        Army[] armies = board.getArmies().values().toArray(new Army[2]);
        return !armies[0].canSomeOneAct() && !armies[1].canSomeOneAct();
    }

}

