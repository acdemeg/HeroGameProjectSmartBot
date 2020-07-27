package com.neolab.heroesGame.samplesSockets;

import com.neolab.heroesGame.aditional.StatisticWriter;
import com.neolab.heroesGame.aditional.plotters.DynamicPlotter;
import com.neolab.heroesGame.enumerations.GameEvent;
import com.neolab.heroesGame.errors.HeroExceptions;
import com.neolab.heroesGame.server.serverNetwork.GameServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Queue;

/**
 * класс реализующий функционал игровой комнты для двух игроков
 */
public class GameRoom extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameRoom.class);
    private final PlayerSocket playerOne;
    private final PlayerSocket playerTwo;
    private final int countBattles;

    public GameRoom(final Queue<PlayerSocket> queuePlayers, final int countBattles) {
        playerOne = queuePlayers.poll();
        playerTwo = queuePlayers.poll();
        this.countBattles = countBattles;
    }

    @Override
    public void run() {
        try {
            final long start = System.nanoTime();
            DynamicPlotter plotter = DynamicPlotter.createDynamicPlotterWithOldInformation(
                    playerOne.getPlayerName(), playerTwo.getPlayerName());

            for (int i = 0; i < countBattles; i++) {
                final GameServer server = new GameServer(playerOne, playerTwo);
                server.prepareForBattle();
                final GameEvent event = server.gameProcess();

                StatisticWriter.writePlayerAnyStatistic(playerOne.getPlayerName(),
                        playerTwo.getPlayerName(), event);
                try {
                    plotter.plotDynamicInfo(playerOne.getPlayerName(), event);
                    plotter.dynamicHistogramPlot();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            final long end = System.nanoTime();
            //1 second = 1__000__000__000 nano seconds
            final double elapsedTimeInSecond = (double) (end - start) / 1__000__000__000;
            LOGGER.warn("Игра длилась {}", elapsedTimeInSecond);

            Server.getCountGameRooms().decrementAndGet();
        } catch (InterruptedException | HeroExceptions | IOException e) {
            LOGGER.warn(e.getMessage());
            throw new IllegalStateException("Игра прервана, внутренняя ошибка сервера");
        }
    }
}
