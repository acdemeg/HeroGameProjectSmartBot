package com.neolab.heroesGame.smartBots;

import com.neolab.heroesGame.arena.BattleArena;
import com.neolab.heroesGame.arena.SquareCoordinate;
import com.neolab.heroesGame.client.ai.PlayerBot;
import com.neolab.heroesGame.enumerations.HeroActions;
import com.neolab.heroesGame.errors.HeroExceptions;
import com.neolab.heroesGame.heroes.Hero;
import com.neolab.heroesGame.heroes.IWarlord;
import com.neolab.heroesGame.heroes.Magician;
import com.neolab.heroesGame.server.answers.Answer;

import java.io.IOException;
import java.util.Set;

public class SmartBotExpert extends SmartBotBase {

    public SmartBotExpert(final int id, final String name) {
        super(id, name);
    }

    @Override
    public Answer getAnswer(final BattleArena board) throws HeroExceptions {
        initializeFullSimulation(board);

        final Set<SquareCoordinate> availableHeroes = getAvailableHeroes(board);

        for(SquareCoordinate heroCoord : availableHeroes){
            final Set<SquareCoordinate> availableTargets = getAvailableTargets(board, heroCoord);
            Hero activeUnit = getHero(board, heroCoord, playerId);

            for(SquareCoordinate target : availableTargets){
                Hero targetUnit = getHero(board, target, enemyId);
                if(targetUnit.getHp() <= activeUnit.getDamage() - (activeUnit.getDamage() * targetUnit.getArmor())){
                    return new Answer(heroCoord, HeroActions.ATTACK, target, this.getId());
                }
                else if(targetUnit instanceof IWarlord){
                     return new Answer(heroCoord, HeroActions.ATTACK, target, this.getId());
                }
                else if(activeUnit instanceof Magician){
                     return new Answer(heroCoord, HeroActions.ATTACK, target, this.getId());
                }
                else if(isMostWoundedUnit(board, availableTargets, targetUnit)){
                     return new Answer(heroCoord, HeroActions.ATTACK, target, this.getId());
                }
            }
        }
        return new PlayerBot(this.getId(), "bot").getAnswer(board);
    }

    private Hero getHero(final BattleArena battleArena, final SquareCoordinate heroCoord, final int armyId){
        return battleArena.getArmy(armyId).getHeroes().get(heroCoord);
    }

    private boolean isMostWoundedUnit(final BattleArena battleArena, final Set<SquareCoordinate> availableTargets, final Hero targetUnit){
        Hero mostWoundedUnit = targetUnit;
        for(SquareCoordinate unit : availableTargets){
            Hero hero = getHero(battleArena, unit, enemyId);
            if(hero.getHp() < mostWoundedUnit.getHp()){
                mostWoundedUnit = hero;
            }
        }
        return mostWoundedUnit.equals(targetUnit);
    }

}
