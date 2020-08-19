package com.neolab.heroesGame.smartBots;

import com.neolab.heroesGame.arena.Army;
import com.neolab.heroesGame.client.ai.Player;
import com.neolab.heroesGame.client.ai.PlayerHuman;
import com.neolab.heroesGame.client.dto.ClientRequest;
import com.neolab.heroesGame.client.dto.ExtendedServerResponse;
import com.neolab.heroesGame.client.gui.IGraphics;
import com.neolab.heroesGame.client.gui.NullGraphics;
import com.neolab.heroesGame.client.gui.console.AsciiGraphics;
import com.neolab.heroesGame.errors.HeroExceptions;
import com.neolab.heroesGame.server.answers.Answer;

import java.io.IOException;
import java.util.Scanner;

public class ClientSmartBot {
    private final Player player;
    private final IGraphics gui;

    public ClientSmartBot(final int playerId, final String name) {
        player = new SmartBotMinMax(playerId, name);
        gui = new NullGraphics();
    }

    public ClientSmartBot(final int playerId, final String name, final IGraphics gui) {
        player = new SmartBotMinMax(playerId, name);
        this.gui = gui;
    }

    private ClientSmartBot(final Player player,
                                  final IGraphics gui) {
        this.player = player;
        this.gui = gui;
    }

    public static ClientSmartBot createPlayerWithAsciiGraphics(final int playerId,
                                                                                            final String name) throws IOException {
        final IGraphics graphics = new AsciiGraphics(playerId);
        return new ClientSmartBot(playerId, name, graphics);
    }

    public static ClientSmartBot createHumanPlayerWithAsciiGraphics(final int playerId,
                                                                                                 final String name) throws IOException {
        final IGraphics graphics = new AsciiGraphics(playerId);
        final Player human = new PlayerHuman(playerId, name, graphics);
        return new ClientSmartBot(human, graphics);
    }

    public static ClientSmartBot createCustomPlayer(final int playerId, final String name) throws IOException {
        System.out.println("1. Создать бота без графической отрисовки");
        System.out.println("2. Создать бота с графической отрисовки");
        System.out.println("3. Создать игрока человека");
        while (true) {
            final Scanner in = new Scanner(System.in);
            int choose = 1;
            try {
                choose = in.nextInt();
                if (choose < 1 || choose > 3) {
                    System.out.println("Неверный формат. Нужно ввести число от 1 до 3");
                    continue;
                }
            } catch (final Exception e) {
                System.out.println("Неверный формат. Нужно ввести число от 1 до 3");
            }
            if (choose == 2) {
                return createPlayerWithAsciiGraphics(playerId, name);
            } else if (choose == 3) {
                return createHumanPlayerWithAsciiGraphics(playerId, name);
            }
            return new ClientSmartBot(playerId, name);
        }
    }

    public String getAnswer(final ExtendedServerResponse response) throws IOException, HeroExceptions {
        gui.showPosition(response);
        final Answer answer = player.getAnswer(response.arena);
        return new ClientRequest(answer).jsonAnswer;
    }

    public void sendInformation(final ExtendedServerResponse response) throws IOException {
        gui.showPosition(response);
    }

    public void endGame(final ExtendedServerResponse response) throws IOException {
        gui.endGame(response);
    }

    public String getArmyFirst(final int armySize) throws IOException {
        return player.getStringArmyFirst(armySize);
    }

    public String getArmySecond(final int armySize, final Army army) throws IOException {
        return player.getStringArmySecond(armySize, army);
    }

    public Player getPlayer() {
        return player;
    }

    public int getPlayerId() {
        return player.getId();
    }

    public String getPlayerName() {
        return player.getName();
    }

    public void setPlayerId(final int id) {
        player.setId(id);
        if (gui instanceof AsciiGraphics) {
            final AsciiGraphics temp = (AsciiGraphics) gui;
            temp.setPresenterPlayerId(id);
        }
    }

    public void setPlayerName(final String name) {
        player.setName(name);
    }
}
