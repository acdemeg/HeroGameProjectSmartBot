package com.neolab.heroesGame.smartBots;

import com.neolab.heroesGame.arena.Army;
import com.neolab.heroesGame.arena.BattleArena;
import com.neolab.heroesGame.arena.SquareCoordinate;
import com.neolab.heroesGame.enumerations.HeroActions;
import com.neolab.heroesGame.enumerations.HeroErrorCode;
import com.neolab.heroesGame.errors.HeroExceptions;
import com.neolab.heroesGame.heroes.Hero;
import com.neolab.heroesGame.server.ActionEffect;
import com.neolab.heroesGame.server.answers.Answer;
import com.neolab.heroesGame.validators.AnswerValidator;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class BoardUpdater {

    private final int waitingPlayerId;
    private final int activePlayerId;
    private final BattleArena board;
    private ActionEffect actionEffect;

    public BoardUpdater(final int activePlayerId, final int waitingPlayerId, final BattleArena board) {
        this.activePlayerId = activePlayerId;
        this.waitingPlayerId = waitingPlayerId;
        this.board = board;
        this.actionEffect = null;
    }

    public ActionEffect getActionEffect() {
        return actionEffect;
    }

    public BattleArena getBoard() {
        return board;
    }

    public void toAct(final Answer answer) throws HeroExceptions {
        if (AnswerValidator.isAnswerValidate(answer, board)) {
            final Map<SquareCoordinate, Integer> effectActionMap;
            final Hero activeHero = getActiveHero(board, answer);

            if (activeHero.isDefence()) {
                activeHero.cancelDefence();
            }
            if (answer.getAction() == HeroActions.DEFENCE) {
                effectActionMap = Collections.emptyMap();
                activeHero.setDefence();
            } else {
                if (answer.getAction() == HeroActions.ATTACK) {
                    effectActionMap = activeHero.toAct(answer.getTargetUnitCoordinate(), board.getArmy(waitingPlayerId));
                    tryToKill(effectActionMap.keySet(), board.getArmy(waitingPlayerId));
                } else {
                    effectActionMap = activeHero.toAct(answer.getTargetUnitCoordinate(), board.getArmy(activePlayerId));
                }
            }
            removeUsedHero(activePlayerId, activeHero.getUnitId());
            setActionEffect(answer, effectActionMap);
        } else {
            throw new HeroExceptions(HeroErrorCode.ERROR_ANSWER);
        }
    }

    private void tryToKill(final Set<SquareCoordinate> coordinateSet, final Army army) {
        coordinateSet.forEach(army::tryToKill);
    }

    private Hero getActiveHero(final BattleArena board, final Answer answer) throws HeroExceptions {
        final Optional<Hero> activeHero = board.getArmy(activePlayerId).getHero(answer.getActiveHeroCoordinate());
        if (activeHero.isPresent()) {
            return activeHero.get();
        }
        throw new HeroExceptions(HeroErrorCode.ERROR_ACTIVE_UNIT);
    }

    private void removeUsedHero(final int activePlayerId, final int heroId) {
        board.removeUsedHeroesById(heroId, activePlayerId);
    }

    private void setActionEffect(final Answer answer, final Map<SquareCoordinate, Integer> enemyHeroPosDamage) {
        actionEffect = new ActionEffect(answer.getAction(), answer.getActiveHeroCoordinate(), enemyHeroPosDamage, activePlayerId);
    }
}
